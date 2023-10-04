package com.ss.VO;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author Excelsior
 */
@Data
@Accessors(chain = true)
public class BucketVO {
    private String bucketName;
    private List<FileVO> files;
}
