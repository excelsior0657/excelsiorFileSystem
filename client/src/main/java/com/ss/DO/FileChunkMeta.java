package com.ss.DO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileChunkMeta {
    private String filename;
    private String extension;
    private Integer chunkNo;
    private String bucketName;
    private Long chunkStart ;
    private Integer chunkSize ;
    private String address;
    // 确保分片数据完整，即使分片有一小部分发送修改，计算出的md5值也会天差地别
    private String chunkMd5;
    private String schema;
    private Boolean completed = false;
}
