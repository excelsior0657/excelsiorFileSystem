package com.ss.service.impl;

import com.ss.BO.ServerInfo;
import com.ss.dto.ServerInfoDto;
import com.ss.service.DiscoveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscoveryServiceImpl implements DiscoveryService {
    private final Map<String, List<ServerInfo>> SERVER_MAP = new ConcurrentHashMap<>();
    @Override
    public void register(ServerInfoDto serverInfo) {
        // 判断map是否存在这个server信息
        List<ServerInfo> serverInfos = SERVER_MAP.getOrDefault(serverInfo.getServiceId(), new ArrayList<>());
        // 如果不存在，则直接放入
        ServerInfo info = new ServerInfo();
        BeanUtils.copyProperties(serverInfo, info);
        // todo info.host 不带有 http或者https 的前缀
        info.setPreTimeStamp(System.currentTimeMillis());
        info.setAlive(true);
        if(!serverInfos.contains(info)){
            serverInfos.add(info);
            log.info("服务注册成功...");
        }
        SERVER_MAP.put(serverInfo.getServiceId(), serverInfos);
    }

    @Override
    public void heartbeat(ServerInfoDto serverInfo) {
        // server 信息是否注册过
        List<ServerInfo> serverInfoList = SERVER_MAP.getOrDefault(serverInfo.getServiceId(), new ArrayList<>());
        boolean exists = false;
        for (ServerInfo server : serverInfoList) {
            if(server.getHost().equals(serverInfo.getHost()) &&
                    server.getPort().equals(serverInfo.getPort())){
                server.setAlive(true);
                server.setPreTimeStamp(System.currentTimeMillis());
                exists = true;
            }
        }
        if(!exists){
            register(serverInfo);
        }else{
            log.info("心跳发送成功...");
        }
    }

    @Scheduled(cron = "*/10 * * * * *")
    private void checkAlive(){
        SERVER_MAP.forEach((serviceId, serverList)->{
            serverList = serverList.stream().filter((server)->{
                Long preTimeStamp = server.getPreTimeStamp() / 1000;
                Long current = System.currentTimeMillis() / 1000;
                if(current - preTimeStamp > 30){
                    server.setAlive(false);
                }
                return (current - preTimeStamp < 60);
            }).collect(Collectors.toList());
            SERVER_MAP.put(serviceId, serverList);
        });
        log.info("check server status end...");
    }

    @Override
    public Map<String, List<ServerInfo>> services() {
        return SERVER_MAP;
    }

    @Override
    public List<ServerInfo> aliveServers() {
        List<ServerInfo> chunkServers = SERVER_MAP.getOrDefault("chunk-server", new ArrayList<>());
        return chunkServers.stream().filter(e->e.getAlive()).collect(Collectors.toList());
    }
}
