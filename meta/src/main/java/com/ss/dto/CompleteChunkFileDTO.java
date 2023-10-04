package com.ss.dto;

import lombok.Data;

/**
 * @author Excelsior
 */
@Data
public class CompleteChunkFileDTO {
    private String filename;
    private String address;
    private Integer chunkNo;
    private String schema;
    private String md5;
}
