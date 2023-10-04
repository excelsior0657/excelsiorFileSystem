package com.ss.service;

import com.ss.config.DiscoveryConfig;
import com.ss.utils.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class Actuator {
    @Value("${server.port}")
    private int port;

    private final DiscoveryConfig discoveryConfig;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduledExecutorService;

    public Actuator(DiscoveryConfig discoveryConfig, RestTemplate restTemplate, ScheduledExecutorService scheduledExecutorService) {
        this.discoveryConfig = discoveryConfig;
        this.restTemplate = restTemplate;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @PostConstruct
    public void register() throws UnknownHostException {
        String serverAddress = discoveryConfig.getServerAddress();
        Map<String, Object> params = new HashMap<>();
        params.put("serviceId", discoveryConfig.getServiceId());
        params.put("host", RequestUtil.getLocalHostExactAddress());
//        params.put("host", InetAddress.getLocalHost().getHostAddress());
        params.put("port", port);
        params.put("schema", discoveryConfig.getSchema());
        Map result = restTemplate.postForObject(serverAddress+"/register", params, Map.class);
        log.info("服务注册result: ");
        log.info("{}", result);

        if(Objects.isNull(result) || !result.get("code").equals(200)){
            throw new RuntimeException("服务注册失败");
        }

        scheduledExecutorService.scheduleAtFixedRate(() -> {
            restTemplate.put(serverAddress+"/heartbeat", params, Map.class);
        }, 0, 10, TimeUnit.SECONDS);
    }
}
