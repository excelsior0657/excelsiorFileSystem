package com.ss.utils;

import com.ss.DO.FileChunkMeta;
import com.ss.error.BusinessException;
import com.ss.errors.EnumClientException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ChunkAddressStrategy {
    private final Map<String, ChunkAddressBuilder> builderMap;

    public ChunkAddressStrategy(List<ChunkAddressBuilder> builderList) {
        this.builderMap = new HashMap<>();
        builderList.forEach(builder->builderMap.put(builder.schema(), builder));
    }

    public String get(FileChunkMeta fileChunkMeta){
        String schema = fileChunkMeta.getSchema();
        ChunkAddressBuilder addressBuilder = builderMap.get(schema);
        if(Objects.isNull(addressBuilder)){
            throw new BusinessException(EnumClientException.SCHEMA_DOES_NOT_SUPPORT);
        }
        return addressBuilder.build(fileChunkMeta);
    }

}
