package com.ss.DO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CompleteChunkFileDO {
    private String filename;
    private String address;
    private Integer chunkNo;
    private String schema;
    private String md5;
}
