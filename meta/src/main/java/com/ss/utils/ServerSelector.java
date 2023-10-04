package com.ss.utils;

import com.ss.BO.ServerInfo;

import java.util.List;

public interface ServerSelector {
    List<ServerInfo> select(List<ServerInfo> aliveServers, int count);
}
