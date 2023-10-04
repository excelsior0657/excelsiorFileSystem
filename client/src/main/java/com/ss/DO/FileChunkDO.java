package com.ss.DO;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
public class FileChunkDO {
    @NotBlank(message = "文件名不得为空")
    private String filename;
    // @NotBlank(message = "文件拓展名不得为空")
    private String extension;
    @NotNull(message = "文件序号不得为空")
    private Integer chunkNo;
    @NotNull(message = "文件大小不得为空")
    private Integer chunkSize;
    @NotBlank(message = "文件存储桶不得为空")
    private String bucketName;
    @NotNull(message = "文件内容不得为空")
    private byte[] bytes;
}
