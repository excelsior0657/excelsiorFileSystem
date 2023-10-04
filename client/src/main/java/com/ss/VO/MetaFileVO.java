package com.ss.VO;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author Excelsior
 */
@Data
@Accessors(chain = true)
public class MetaFileVO {
    private String filename;
    private String bucketName;
    List<FileChunkMetaVO> chunks;
}
