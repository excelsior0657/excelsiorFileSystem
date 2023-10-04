package com.ss.VO;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Excelsior
 */
@Data
@Accessors(chain = true)
public class FileChunkMetaVO {
    private String filename;
    private Integer chunkNo;
    private Integer chunkStart;
    private Integer chunkSize;
    private Boolean completed;
}
