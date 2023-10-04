package com.ss.services.impl;

import com.ss.DTO.FileChunkDTO;
import com.ss.config.ChunkConfig;
import com.ss.error.BusinessException;
import com.ss.errors.EnumChunkException;
import com.ss.services.FileService;
import com.ss.utils.Md5Util;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class FileServiceImpl implements FileService {

    private final ChunkConfig chunkConfig;
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public FileServiceImpl(ChunkConfig chunkConfig) {
        this.chunkConfig = chunkConfig;
    }


    @Override
    public String write(FileChunkDTO fileChunkDTO) {
        Integer chunkNo = fileChunkDTO.getChunkNo();
        Integer chunkSize = fileChunkDTO.getChunkSize();
        String bucketName = fileChunkDTO.getBucketName();
        String extension = fileChunkDTO.getExtension();
        String filename = fileChunkDTO.getFilename();
        byte[] bytes = fileChunkDTO.getBytes();
        // 存储桶路径
        String chunkPath = buildChunkPath(bucketName, filename, chunkNo, extension);
        File chunkFile = new File(chunkPath);

        try (FileOutputStream fileOutputStream = new FileOutputStream(chunkFile)) {
            reentrantReadWriteLock.writeLock().lock();
            // 存储桶空间不足
            if (chunkFile.getFreeSpace() < chunkSize) {
                throw new BusinessException(EnumChunkException.DISK_SPACE_NOT_ENOUGH_MEMORY);
            }
            // 创建文件失败
            if (!chunkFile.exists()) {
                boolean created = chunkFile.createNewFile();
                if (!created) {
                    throw new BusinessException(EnumChunkException.FAILED_TO_CREATE_CHUNK_FILE);
                }
            }
            // 写入文件，返回md5
            fileOutputStream.write(bytes);
            String md5 = Md5Util.getMd5(bytes);
            return md5;
        } catch (Exception e) {
            throw new BusinessException(EnumChunkException.FAILED_TO_CREATE_CHUNK_FILE);
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public byte[] read(String filename, String extension, Integer chunkNo, String bucketName) {
        String chunkPath = buildChunkPath(bucketName, filename, chunkNo, extension);

        try {
            reentrantReadWriteLock.readLock().lock();
            return Files.readAllBytes(Paths.get(chunkPath));
        } catch (Exception e) {
            throw new BusinessException(EnumChunkException.FAILED_TO_READ_CHUNK_FILE);
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    private String buildChunkPath(String bucketName, String filename, Integer chunkNo, String extension) {

        return "%s/%s_%s_%s.%s".formatted(chunkConfig.getWorkSpace(), bucketName, filename, chunkNo, extension);
    }
}
