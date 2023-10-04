package com.ss.VO;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileVO {
    private String filename;
    private String extension;
    private String bucketName;
    private Long filesize;
}
