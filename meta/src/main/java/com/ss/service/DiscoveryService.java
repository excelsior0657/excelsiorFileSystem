package com.ss.service;

import com.ss.BO.ServerInfo;
import com.ss.dto.ServerInfoDto;

import java.util.List;
import java.util.Map;

public interface DiscoveryService {

    /**
     * 服务注册
     * @param serverInfo 服务器信息
     */
    void register(ServerInfoDto serverInfo);

    void heartbeat(ServerInfoDto serverInfo);

    Map<String, List<ServerInfo>> services();

    List<ServerInfo> aliveServers();
}
