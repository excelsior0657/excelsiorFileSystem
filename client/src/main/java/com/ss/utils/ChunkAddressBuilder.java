package com.ss.utils;

import com.ss.DO.FileChunkMeta;

public interface ChunkAddressBuilder {
    String build(FileChunkMeta chunkMeta);
    String schema();
}
