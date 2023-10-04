package com.ss.utils.impl;

import com.ss.DO.FileChunkMeta;
import com.ss.utils.ChunkAddressBuilder;
import org.springframework.stereotype.Component;

@Component
public class HttpChunkAddressBuilder implements ChunkAddressBuilder {
    @Override
    public String build(FileChunkMeta chunkMeta) {
        return "%s://%s".formatted(chunkMeta.getSchema(),chunkMeta.getAddress());
    }

    @Override
    public String schema() {
        return "http";
    }
}
