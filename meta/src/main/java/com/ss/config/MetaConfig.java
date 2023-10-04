package com.ss.config;

import com.ss.utils.FilenameGenerator;
import com.ss.utils.ServerSelector;
import com.ss.utils.impl.DefaultFilenameGenerator;
import com.ss.utils.impl.DefaultServerSelector;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("meta")
public class MetaConfig {
    private Integer chunkSize;
    private Integer chunkInstanceCount;
    private Boolean useHttps = false;
    private Integer chunkInstanceMaxWeight = 16;

    @Bean
    @ConditionalOnMissingBean(value = FilenameGenerator.class)
    public FilenameGenerator filenameGenerator(){
        return new DefaultFilenameGenerator();
    }

    @Bean
    @ConditionalOnMissingBean(value = ServerSelector.class)
    public ServerSelector serverSelector(){
        return new DefaultServerSelector();
    }
}
