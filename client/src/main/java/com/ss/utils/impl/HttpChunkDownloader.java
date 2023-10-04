package com.ss.utils.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ss.DO.FileChunkMeta;
import com.ss.DO.MetaFile;
import com.ss.response.CommonResponse;
import com.ss.utils.ChunkAddressStrategy;
import com.ss.utils.ChunkDownloader;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Excelsior
 */

@Component
public class HttpChunkDownloader implements ChunkDownloader {

    private final ChunkAddressStrategy chunkAddressStrategy;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpChunkDownloader(ChunkAddressStrategy chunkAddressStrategy, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.chunkAddressStrategy = chunkAddressStrategy;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] downloader(FileChunkMeta chunkMeta) {
        String address = chunkAddressStrategy.get(chunkMeta);
        String url = address + "/file/read?filename={filename}&extension={extension}&chunkNo={chunkNo}&bucketName={bucketName}";
        Map<String, Object> params = new HashMap<>();
        params.put("filename", chunkMeta.getFilename());
        params.put("extension", chunkMeta.getExtension());
        params.put("chunkNo", chunkMeta.getChunkNo());
        params.put("bucketName", chunkMeta.getBucketName());
        // 向meta发送请求，获取metaFile信息
        Object response = restTemplate.getForObject(url, Object.class, params);
        CommonResponse<byte[]> commonResponse = objectMapper.convertValue(response, new TypeReference<CommonResponse<byte[]>>() {
        });
        return commonResponse.getData();
    }

    @Override
    public String schema() {
        return "http";
    }
}
