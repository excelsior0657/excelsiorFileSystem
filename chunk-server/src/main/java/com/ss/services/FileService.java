package com.ss.services;

import com.ss.DTO.FileChunkDTO;

public interface FileService {
    String write(FileChunkDTO fileChunkDTO);

    byte[] read(String filename, String extension, Integer chunkNo, String bucketName);
}
