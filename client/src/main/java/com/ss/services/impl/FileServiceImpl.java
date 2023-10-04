package com.ss.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.DO.CompleteChunkFileDO;
import com.ss.DO.FileChunkDO;
import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.DTO.FileMeta;
import com.ss.VO.BucketVO;
import com.ss.VO.FileChunkMetaVO;
import com.ss.VO.MetaFileVO;
import com.ss.config.ClientConfig;
import com.ss.services.FileService;
import com.ss.error.BusinessException;
import com.ss.errors.EnumClientException;
import com.ss.response.CommonResponse;
import com.ss.utils.ChunkAddressStrategy;
import com.ss.utils.ChunkDownloaderStrategy;
import com.ss.utils.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    private final RestTemplate restTemplate;
    private final ClientConfig clientConfig;
    private final ObjectMapper objectMapper;
    private final ChunkAddressStrategy chunkAddressStrategy;
    private final ChunkDownloaderStrategy chunkDownloaderStrategy;

    public FileServiceImpl(RestTemplate restTemplate, ClientConfig clientConfig, ObjectMapper objectMapper, ChunkAddressStrategy chunkAddressStrategy, ChunkDownloaderStrategy chunkDownloaderStrategy) {
        this.restTemplate = restTemplate;
        this.clientConfig = clientConfig;
        this.objectMapper = objectMapper;
        this.chunkAddressStrategy = chunkAddressStrategy;
        this.chunkDownloaderStrategy = chunkDownloaderStrategy;
    }

    /**
     * 上传文件，进行切片，并发执行切片任务
     * 调用chunk-server的write方法写入切片
     * 分片上传完成时调用meta保存分片完成状态，并存放在MongoDB中
     *
     * @param bucketName
     * @param file
     * @return
     */
    @Override
    public String upload(String bucketName, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (Objects.nonNull(originalFilename)) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex != -1) {
                extension = originalFilename.substring(dotIndex);
            }
        }
        FileMeta fileMeta = new FileMeta()
                .setBucketName(bucketName)
                .setExtension(extension)
                .setFilesize(file.getSize());

        Object response = restTemplate.postForObject(clientConfig.getMetaServerAddress() + "/meta/generate", fileMeta, Object.class);
        CommonResponse<MetaFile> commonResponse = objectMapper
                .convertValue(response, new TypeReference<CommonResponse<MetaFile>>() {
                });

        if (Objects.isNull(commonResponse)) {
            throw new BusinessException(EnumClientException.FAILED_TO_GET_META_FILE);
        }

        MetaFile metaFile = commonResponse.getData();

        if (Objects.isNull(metaFile)) {
            throw new BusinessException("meta file 为空", EnumClientException.FAILED_TO_GET_META_FILE);
        }

        try {
            uploadChunks(file, metaFile);
        } catch (IOException e) {
            throw new BusinessException(EnumClientException.FAILED_TO_UPLOAD_CHUNK_FILE);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "%s/%s.%s".formatted(bucketName, metaFile.getFilename(), metaFile.getExtension());
    }


    /**
     * 上传文件，进行切片，并发执行切片任务
     * 调用chunk-server的write方法写入切片
     * 分片上传完成时调用meta保存分片完成状态，并存放在MongoDB中
     *
     * @param file
     * @param metaFile
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void uploadChunks(MultipartFile file, MetaFile metaFile) throws IOException, ExecutionException, InterruptedException {
        List<FileChunkMeta> chunks = metaFile.getChunks();
        chunks = chunks.stream()
                .sorted(Comparator.comparing(FileChunkMeta::getChunkNo))
                .collect(Collectors.toList());
        if (chunks.size() == 0) {
            return;
        }
        InputStream inputStream = file.getInputStream();
        // 切割分片时的操作可以并行执行，创建任务列表
        CompletableFuture<?>[] tasks = new CompletableFuture[chunks.size()];

        byte[] buffer = new byte[0];
        int preChunkNo = -1;

        for (int i = 0; i < chunks.size(); i++) {
            FileChunkMeta chunk = chunks.get(i);
            FileChunkDO fileChunkDO = new FileChunkDO();

            Integer chunkSize = chunk.getChunkSize();

            if (chunk.getChunkNo() != preChunkNo) {
                preChunkNo = chunk.getChunkNo();
                buffer = new byte[chunkSize];
                inputStream.read(buffer);
            }


            byte[] finalBuffer = buffer;
            tasks[i] = CompletableFuture.runAsync(() -> {
                // 如果已经上传直接return
                if (chunk.getCompleted()) {
                    return;
                }
                String md5 = Md5Util.getMd5(finalBuffer);
                fileChunkDO.setFilename(chunk.getFilename())
                        .setExtension(chunk.getExtension())
                        .setChunkNo(chunk.getChunkNo())
                        .setChunkSize(chunkSize)
                        .setBucketName(chunk.getBucketName())
                        .setBytes(finalBuffer);
                String schema = chunk.getSchema();
                String address = chunkAddressStrategy.get(chunk);
                // 调用chunk-server存放文件分片
                Object response = restTemplate.postForObject(address + "/file/write",
                        fileChunkDO,
                        Object.class);
                if (Objects.isNull(response)) {
                    throw new BusinessException("第 " + chunk.getChunkNo() + " 个分片上传失败", EnumClientException.FAILED_TO_UPLOAD_CHUNK_FILE);
                }

                CommonResponse<String> md5Response = objectMapper.convertValue(response, new TypeReference<CommonResponse<String>>() {
                });

                if (!md5Response.getData().equals(md5)) {
                    throw new BusinessException(EnumClientException.CHUNK_FILE_INCOMPLETE);
                }

                CompleteChunkFileDO completeChunkFileDO = new CompleteChunkFileDO();
                completeChunkFileDO.setFilename(chunk.getFilename())
                        .setChunkNo(chunk.getChunkNo())
                        .setAddress(chunk.getAddress())
                        .setSchema(chunk.getSchema())
                        .setMd5(md5);
                // 调用meta服务设置complete为true
                Object resp = restTemplate.postForObject(clientConfig.getMetaServerAddress() + "/meta/chunk/complete",
                        completeChunkFileDO,
                        Object.class);

                if (Objects.isNull(resp)) {
                    throw new BusinessException(EnumClientException.FAILED_TO_UPDATE_CHUNK_FILE_COMPLETE_STATUS);
                }

                log.info("更新分片状态: {}", resp);
            }).whenComplete((o, throwable) -> {
                if (Objects.nonNull(throwable)) {
                    throw new RuntimeException(MessageFormat.format("第 {} 个分片上传失败", chunk.getChunkNo()));
                }
            });

        }

        /**
         * 执行任务列表
         * 若有一个任务失败，则全部任务快速失败
         */
        CompletableFuture<Void> allOf = CompletableFuture.allOf(tasks);
        CompletableFuture<?> anyException = new CompletableFuture<>();
        Arrays.stream(tasks).forEach(t -> {
            t.exceptionally(throwable -> {
                anyException.completeExceptionally(throwable);
                return null;
            });
        });
        CompletableFuture.anyOf(allOf, anyException).get();
    }

    @Override
    public MetaFile getMeta(String bucketName, String filename) {
        // 去除文件后缀名
        if (filename.indexOf(".") != -1) {
            filename = filename.substring(filename.indexOf("."));
        }

        String url = clientConfig.getMetaServerAddress() + "/meta/info?bucketName={bucketName}&filename={filename}";
        Map<String, Object> params = new HashMap<>();
        params.put("bucketName", bucketName);
        params.put("filename", filename);
        // 向meta发送请求，获取metaFile信息
        Object response = restTemplate.getForObject(url, Object.class, params);
        CommonResponse<MetaFile> commonResponse = objectMapper.convertValue(response, new TypeReference<CommonResponse<MetaFile>>() {
        });
        return commonResponse.getData();
    }

    @Override
    public byte[] downloadChunk(FileChunkMeta chunk) {
        return chunkDownloaderStrategy.download(chunk);
    }

    /**
     * 生成大文件上传所需要的meta信息
     *
     * @param fileMeta
     * @return
     */
    @Override
    public MetaFileVO meta(FileMeta fileMeta) {
        String url = clientConfig.getMetaServerAddress() + "/meta/generate";
        Object response = restTemplate.postForObject(
                url,
                fileMeta,
                Object.class
        );
        CommonResponse<MetaFile> commonResponse = objectMapper.convertValue(response,
                new TypeReference<CommonResponse<MetaFile>>() {
                });
        MetaFile metaFile = commonResponse.getData();
        return buildMetaFileVO(metaFile);
    }

    /**
     * 把MetaFile整理成MetaFileVO
     * 并将重复的chunk去除
     *
     * @param metaFile
     * @return
     */
    private MetaFileVO buildMetaFileVO(MetaFile metaFile) {
        MetaFileVO metaFileVO = new MetaFileVO();
        List<FileChunkMeta> originChunks = metaFile.getChunks();
        List<FileChunkMetaVO> fileChunkMetaVOS = new ArrayList<>();
        // 先把原始的chunks改造整理的新的chunks中，最后在根据chunkNo去重
        for (FileChunkMeta originChunk : originChunks) {
            FileChunkMetaVO fileChunkMetaVO = new FileChunkMetaVO();
            fileChunkMetaVO.setFilename(originChunk.getFilename())
                    .setChunkNo(originChunk.getChunkNo())
                    .setChunkStart(originChunk.getChunkStart().intValue())
                    .setChunkSize(originChunk.getChunkSize())
                    .setCompleted(originChunk.getCompleted());
            fileChunkMetaVOS.add(fileChunkMetaVO);
        }

        fileChunkMetaVOS = fileChunkMetaVOS.stream().distinct().collect(Collectors.toList());
        metaFileVO.setChunks(fileChunkMetaVOS)
                .setFilename(metaFile.getFilename())
                .setBucketName(metaFile.getBucketName());
        return metaFileVO;
    }

    @Override
    public String uploadChunk(String bucketName,
                              String filename,
                              String md5,
                              Integer chunkNo,
                              MultipartFile file) {
//        MetaFile metaFile = getMeta(bucketName, filename);
//        List<FileChunkMeta> chunks = metaFile.getChunks();

        String metaServerAddress = clientConfig.getMetaServerAddress();
        String url = metaServerAddress + "/meta/chunk/info?bucketName={bucketName}&filename={filename}&chunkNo={chunkNo}";
        Map<String, Object> params = new HashMap<>();
        params.put("bucketName", bucketName);
        params.put("filename", filename);
        params.put("chunkNo", chunkNo);

        Object resp = restTemplate.getForObject(url, Object.class, params);
        CommonResponse<List<FileChunkMeta>> chunkInfoResp = objectMapper.convertValue(resp,
                new TypeReference<CommonResponse<List<FileChunkMeta>>>() {
        });

        List<FileChunkMeta> chunks = chunkInfoResp.getData();

        String realMd5 = Md5Util.getMd5(file);
        if (!Objects.equals(md5, realMd5)) {
            throw new BusinessException(EnumClientException.CHUNK_FILE_INCOMPLETE);
        }
        chunks = chunks.stream().filter(c -> c.getChunkNo().equals(chunkNo)).collect(Collectors.toList());
        chunks.forEach(c -> {
            Integer chunkSize = c.getChunkSize();
            byte[] buffer = new byte[chunkSize];
            try (InputStream inputStream = file.getInputStream()) {
                inputStream.read(buffer);

                FileChunkDO fileChunkDO = new FileChunkDO();
                fileChunkDO.setFilename(c.getFilename())
                        .setChunkNo(c.getChunkNo())
                        .setExtension(c.getExtension())
                        .setChunkSize(c.getChunkSize())
                        .setBucketName(c.getBucketName())
                        .setBytes(buffer);
                String address = chunkAddressStrategy.get(c);
                Object response = restTemplate.postForObject(address + "/file/write", fileChunkDO, Object.class);
                if (Objects.isNull(realMd5)) {
                    throw new RuntimeException(MessageFormat.format("第 {} 分片上传失败", c.getChunkNo()));
                }
                CommonResponse<String> commonResponse = objectMapper.convertValue(response, new TypeReference<CommonResponse<String>>() {
                });
                String serverMd5 = commonResponse.getData();
                if (!Objects.equals(serverMd5, realMd5)) {
                    throw new RuntimeException(MessageFormat.format("第 {} 分片不完整", c.getChunkNo()));
                }



                CompleteChunkFileDO completeChunkFileDO = new CompleteChunkFileDO();
                completeChunkFileDO.setFilename(c.getFilename())
                        .setChunkNo(c.getChunkNo())
                        .setSchema(c.getSchema())
                        .setMd5(md5)
                        .setAddress(c.getAddress());

                Object completeResp = restTemplate.postForObject(
                        metaServerAddress + "/meta/chunk/complete",
                        completeChunkFileDO,
                        Object.class
                );
                if (Objects.isNull(completeResp)) {
                    throw new RuntimeException(MessageFormat.format("第 {} 分片状态更新失败", c.getChunkNo()));
                }
            } catch (IOException e) {
                log.info("第 {} 分片上传失败，原因: ", c.getChunkNo(), e);
                throw new BusinessException(EnumClientException.FAILED_TO_UPLOAD_CHUNK_FILE);
            }
        });
        return md5;
    }

    @Override
    public List<BucketVO> files() {
        String metaServerAddress = clientConfig.getMetaServerAddress();
        Object response = restTemplate.getForObject(metaServerAddress + "/meta/files", Object.class);
        CommonResponse<List<BucketVO>> commonResponse = objectMapper.convertValue(response,
                new TypeReference<CommonResponse<List<BucketVO>>>() {
                });
        return commonResponse.getData();

    }

    @Override
    public void delete(String bucketName, String filename) {
        String metaServerAddress = clientConfig.getMetaServerAddress();
        String url = "%s/meta/%s/%s".formatted(metaServerAddress, bucketName, filename);
        restTemplate.delete(url);
    }
}
