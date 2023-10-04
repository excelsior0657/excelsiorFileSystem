package com.ss.services;

import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.DTO.FileMeta;
import com.ss.VO.BucketVO;
import com.ss.VO.MetaFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Excelsior
 */
public interface FileService {
    String upload(String bucketName, MultipartFile file);

    MetaFile getMeta(String bucketName, String filename);

    byte[] downloadChunk(FileChunkMeta chunk);

    MetaFileVO meta(FileMeta fileMeta);

    String uploadChunk(String bucketName, String filename, String md5, Integer chunkNo, MultipartFile file);

    List<BucketVO> files();

    void delete(String bucketName, String filename);
}
