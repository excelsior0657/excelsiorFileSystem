package com.ss.service.impl;

import com.ss.BO.ServerInfo;
import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.VO.BucketVO;
import com.ss.VO.FileVO;
import com.ss.config.MetaConfig;
import com.ss.dto.CompleteChunkFileDTO;
import com.ss.dto.FileMeta;
import com.ss.error.BusinessException;
import com.ss.errors.EnumMetaException;
import com.ss.service.DiscoveryService;
import com.ss.service.MetaService;
import com.ss.utils.FilenameGenerator;
import com.ss.utils.RequestUtil;
import com.ss.utils.ServerSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetaServiceImpl implements MetaService {
    private final FilenameGenerator filenameGenerator;
    private final HttpServletRequest request;
    private final MetaConfig metaConfig;
    private final MongoTemplate mongoTemplate;
    private final DiscoveryService discoveryService;
    private final ServerSelector serverSelector;

    public MetaServiceImpl(FilenameGenerator filenameGenerator, HttpServletRequest request, MetaConfig metaConfig, MongoTemplate mongoTemplate, DiscoveryService discoveryService, ServerSelector serverSelector) {
        this.filenameGenerator = filenameGenerator;
        this.request = request;
        this.metaConfig = metaConfig;
        this.mongoTemplate = mongoTemplate;
        this.discoveryService = discoveryService;
        this.serverSelector = serverSelector;
    }

    @Override
    public MetaFile generate(FileMeta fileMeta) {
        Long filesize = fileMeta.getFilesize();
        String extension = fileMeta.getExtension();
        String bucketName = fileMeta.getBucketName();
        String clientIpAddress = RequestUtil.getIpAddress(request);
        Integer chunkSize = metaConfig.getChunkSize();
        int totalChunk = (int) Math.ceil(filesize * 1.0 / chunkSize);
        String filename = filenameGenerator.generate(fileMeta, clientIpAddress);
        MetaFile metaFile = mongoTemplate.findById(filename, MetaFile.class);
        if (Objects.nonNull(metaFile)) {
            return metaFile;
        }


        List<FileChunkMeta> chunks = createChunks(filesize, extension, bucketName, chunkSize, totalChunk, filename);

        metaFile = new MetaFile();
        metaFile.setFilename(filename)
                .setExtension(extension)
                .setFilesize(filesize)
                .setBucketName(bucketName)
                .setTotalChunk(totalChunk)
                .setChunks(chunks)
                .setCompleted(false);

        mongoTemplate.insert(metaFile);
        return metaFile;
    }


    /**
     * 创建文件分片元数据
     *
     * @param filesize   文件大小
     * @param extension  文件后缀名
     * @param bucketName 文件存储桶
     * @param chunkSize  分片大小
     * @param totalChunk 总分片数量
     * @param filename   文件名
     * @return 分片元数据列表
     */
    private List<FileChunkMeta> createChunks(Long filesize,
                                             String extension,
                                             String bucketName,
                                             Integer chunkSize,
                                             int totalChunk,
                                             String filename) {
        List<FileChunkMeta> chunks = new ArrayList<>();
        List<ServerInfo> aliveServers = discoveryService.aliveServers();
        if (aliveServers.size() == 0) {
            throw new BusinessException(EnumMetaException.NOT_ENOUGH_CHUNK_SERVER);
        }

        long start = 0;
        for (int i = 0; i < totalChunk; i++) {
            long currentChunkSize = chunkSize;
            if (filesize < (i + 1) * chunkSize) {
                currentChunkSize = (long) (filesize - i * chunkSize);
            }

            // 选择策略
            List<ServerInfo> selectServers = serverSelector.select(aliveServers, metaConfig.getChunkInstanceCount());
            for (ServerInfo selectServer : selectServers) {
                String address = selectServer.getHost() + ":" + selectServer.getPort();

                FileChunkMeta fileChunkMeta = new FileChunkMeta();
                fileChunkMeta.setFilename(filename)
                        .setChunkNo(i)
                        .setBucketName(bucketName)
                        .setChunkStart(start)
                        .setChunkSize((int) currentChunkSize)
                        .setExtension(extension)
                        .setSchema(selectServer.getSchema())
                        .setAddress(address)
                        .setWeight(metaConfig.getChunkInstanceMaxWeight());
                chunks.add(fileChunkMeta);
            }
            start += currentChunkSize;
        }

        return chunks;
    }


    @Override
    public void completeChunk(CompleteChunkFileDTO completeChunkFileDTO) {
        String filename = completeChunkFileDTO.getFilename();
        MetaFile metaFile = mongoTemplate.findById(filename, MetaFile.class);
        if (Objects.isNull(metaFile)) {
            throw new BusinessException(EnumMetaException.META_FILE_NOT_FOUND);
        }

        AtomicBoolean completed = new AtomicBoolean(true);

        metaFile.getChunks().forEach(c -> {
            if (c.getChunkNo().equals(completeChunkFileDTO.getChunkNo()) &&
                    c.getAddress().equals(completeChunkFileDTO.getAddress()) &&
                    c.getSchema().equals(completeChunkFileDTO.getSchema())) {
                c.setChunkMd5(completeChunkFileDTO.getMd5());
                c.setCompleted(true);
                if (!c.getCompleted()) {
                    completed.set(false);
                }
            }
        });
        metaFile.setCompleted(completed.get());
        mongoTemplate.save(metaFile);
    }

    /**
     * 获取文件存储的每个Server是否可用情况
     * 如果所有分片分开存储的Server最终不能将该文件所有分片返回（部分分片的Server宕机），则抛异常，说明该文件存在分片不可用
     * 否则更新并返回metaFile信息（更新了metaFile存储每个分片权重最大的server）
     *
     * @param bucketName
     * @param filename
     * @return
     */
    @Override
    public MetaFile meta(String bucketName, String filename) {
        // 获取metaFile
        MetaFile metaFile = mongoTemplate.findById(filename, MetaFile.class);
        if (Objects.isNull(metaFile)) {
            throw new BusinessException(EnumMetaException.META_FILE_NOT_FOUND);
        }

        // 获取可用server
        List<ServerInfo> serverInfos = discoveryService.aliveServers();
        List<String> addressSet = serverInfos.stream()
                .map(serverInfo -> serverInfo.getHost() + ":" + serverInfo.getPort())
                .collect(Collectors.toList());

        // 更新Server权重
        List<FileChunkMeta> chunks = metaFile.getChunks();
        chunks.forEach(c -> {
            String address = c.getAddress();
            if (!addressSet.contains(address)) {
                c.setWeight(c.getWeight() > 0 ? c.getWeight() - 1 : 0);
            } else {
                c.setWeight(metaConfig.getChunkInstanceMaxWeight());
            }
            if (!c.getCompleted()) {
                throw new BusinessException(EnumMetaException.CHUNK_FILE_NOT_UPLOADED);
            }
        });
        mongoTemplate.save(metaFile);

        // 获取每个分片权重最大的Server
        List<FileChunkMeta> chunkMetas = chunks.stream()
                .collect(Collectors.groupingBy(FileChunkMeta::getChunkNo))
                .values()
                .stream()
                .parallel()
                .map(fileChunkMetas -> fileChunkMetas.stream()
                        .peek(c -> {
                            String address = c.getAddress();
                            if (!addressSet.contains(address)) {
                                c.setWeight(c.getWeight() > 0 ? c.getWeight() - 1 : 0);
                            }

                        })
                        .max(Comparator.comparing(FileChunkMeta::getWeight))
                        .orElse(new FileChunkMeta())
                )
                .filter(e -> e.getWeight() > 0)
                .collect(Collectors.toList());

        // 如果分片信息不完整，部分存储分片的Server宕机，则抛异常
        if (chunkMetas.size() < metaFile.getTotalChunk()) {
            Set<Integer> chunkNoSet = chunkMetas.stream()
                    .map(FileChunkMeta::getChunkNo)
                    .collect(Collectors.toSet());

            List<Integer> lossChunkNo = new ArrayList<>();
            for (int i = 0; i < metaFile.getTotalChunk(); i++) {
                if (!chunkNoSet.contains(i)) {
                    lossChunkNo.add(i);
                }

            }
            log.warn("文件 {} 存在分片不可用现象: {}", filename, lossChunkNo);
            throw new BusinessException(EnumMetaException.NO_CHUNK_META_AVAILABLE);
        }

        // 如果Server存储的分片均完整，则返回新的metaFile
        metaFile.setChunks(chunkMetas);
        return metaFile;
    }

    /**
     * 查询所有file，并按照bucketName分类
     * 且转成页面所需要的VO
     *
     * @return
     */
    @Override
    public List<BucketVO> files() {
        List<MetaFile> allFilesMeta = mongoTemplate.findAll(MetaFile.class);
        Map<String, List<MetaFile>> bucketMap = allFilesMeta.stream()
                .collect(Collectors.groupingBy(MetaFile::getBucketName));
        List<BucketVO> bucketVOList = bucketMap.entrySet().stream().map(entry -> {
            List<FileVO> fileVOList = entry.getValue()
                    .stream()
                    .filter(MetaFile::getCompleted)
                    .map(metaFile -> new FileVO()
                            .setFilename(metaFile.getFilename())
                            .setExtension(metaFile.getExtension())
                            .setBucketName(metaFile.getBucketName())
                            .setFilesize(metaFile.getFilesize()))
                    .collect(Collectors.toList());
            return new BucketVO().setBucketName(entry.getKey()).setFiles(fileVOList);
        }).collect(Collectors.toList());
        return bucketVOList;
    }

    @Override
    public void delete(String bucketName, String filename) {
        Query query =new Query();
        query.addCriteria(Criteria.where("filename").is(filename));
        mongoTemplate.remove(query, MetaFile.class);

    }

    @Override
    public List<FileChunkMeta> chunkInfo(String bucketName, String filename, Integer chunkNo) {
        if(filename.contains(".")){
            filename = filename.split(".")[0];
        }
        MetaFile metaFile = mongoTemplate.findById(filename, MetaFile.class);
        if(Objects.isNull(metaFile)){
            throw new BusinessException(EnumMetaException.META_FILE_NOT_FOUND);
        }
        return metaFile.getChunks()
                .stream()
                .filter(chunk -> chunk.getChunkNo().equals(chunkNo))
                .collect(Collectors.toList());
    }
}
