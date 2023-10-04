package com.ss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Data
@Configuration
@ConfigurationProperties("client")
public class ClientConfig {
    private String metaServerHost;
    private String metaServerPort;


    public String getMetaServerAddress(){
        return "%s:%s".formatted(metaServerHost, metaServerPort);
    }
}
