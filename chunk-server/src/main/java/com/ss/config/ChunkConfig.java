package com.ss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("chunk")
public class ChunkConfig {
    private String workSpace = "/usr/chunk-server/";
}
