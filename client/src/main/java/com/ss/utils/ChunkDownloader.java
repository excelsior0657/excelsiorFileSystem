package com.ss.utils;

import com.ss.DO.FileChunkMeta;
import com.ss.DTO.FileMeta;

public interface ChunkDownloader {
    byte[] downloader(FileChunkMeta chunkMeta);

    String schema();
}
