package com.ss.service;

import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.VO.BucketVO;
import com.ss.dto.CompleteChunkFileDTO;
import com.ss.dto.FileMeta;

import java.util.List;

public interface MetaService {
    MetaFile generate(FileMeta fileMeta);

    void completeChunk(CompleteChunkFileDTO completeChunkFileDTO);

    MetaFile meta(String bucketName, String filename);

    List<BucketVO> files();

    void delete(String bucketName, String filename);

    List<FileChunkMeta> chunkInfo(String bucketName, String filename, Integer chunkNo);
}
