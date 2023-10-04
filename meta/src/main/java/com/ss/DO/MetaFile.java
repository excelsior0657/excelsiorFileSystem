package com.ss.DO;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author Excelsior
 */
@Data
@Document("meta")
@Accessors(chain = true)
public class MetaFile {
    @Id
    private String filename;
    private String extension;
    private Long filesize;
    private String bucketName;
    private Integer totalChunk;
    private Boolean completed;
    private List<FileChunkMeta> chunks;
}
