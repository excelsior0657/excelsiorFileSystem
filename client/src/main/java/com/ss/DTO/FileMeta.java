package com.ss.DTO;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain = true)
public class FileMeta {
    @NotEmpty(message = "文件大小不能为空")
    private Long filesize;

    @NotBlank(message = "文件后缀名不能为空")
    private String extension;

    @NotBlank(message = "文件桶名不能为空")
    private String bucketName;
}
