package com.ss.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Data
@Configuration
@ConfigurationProperties("discovery")
@Slf4j
public class DiscoveryConfig {
    private String serviceId;
    private String serverHost;
    private Integer serverPort;

    private String schema;

    public String getServerAddress(){
        return "%s:%s".formatted(serverHost, serverPort);
    }


    @Bean
    public ScheduledExecutorService scheduledExecutorService(){
        return Executors.newScheduledThreadPool(20);
    }
}
