package com.ss.utils;

import com.ss.DO.FileChunkMeta;
import com.ss.error.BusinessException;
import com.ss.errors.EnumClientException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Excelsior
 */
@Component
public class ChunkDownloaderStrategy {
    private final Map<String, ChunkDownloader> downloaderMap;

    public ChunkDownloaderStrategy(List<ChunkDownloader> downloaderList) {
        this.downloaderMap = new HashMap<>();
        downloaderList.forEach(downloader -> downloaderMap.put(downloader.schema(), downloader));
    }

    public byte[] download(FileChunkMeta fileChunkMeta) {
        String schema = fileChunkMeta.getSchema();
        ChunkDownloader downloader = downloaderMap.get(schema);
        if (Objects.isNull(downloader)) {
            throw new BusinessException(EnumClientException.SCHEMA_DOES_NOT_SUPPORT);
        }
        return downloader.downloader(fileChunkMeta);
    }
}
