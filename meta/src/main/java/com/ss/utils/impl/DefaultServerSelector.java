package com.ss.utils.impl;

import com.ss.BO.ServerInfo;
import com.ss.error.BusinessException;
import com.ss.errors.EnumMetaException;
import com.ss.utils.ServerSelector;

import java.util.ArrayList;
import java.util.List;

public class DefaultServerSelector implements ServerSelector {
    @Override
    public List<ServerInfo> select(List<ServerInfo> aliveServers, int count) {
        if(aliveServers.size() < count){
            throw new BusinessException("存活的服务数量 < 分片存储数量", EnumMetaException.NOT_ENOUGH_CHUNK_SERVER);
        }
        int[] indxArray  = new int[aliveServers.size()];
        for (int i = 0; i < indxArray.length; i++) {
            indxArray[i] = i;
        }
        // 洗牌法打乱
        for (int i = 0; i < count; i++) {
            int randomIndex = (int) (Math.random() * aliveServers.size());
            int tmp = indxArray[randomIndex];
            indxArray[randomIndex] = indxArray[i];
            indxArray[i] =tmp;
        }
        List<ServerInfo> selectedServers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            selectedServers.add(aliveServers.get(indxArray[i]));
        }
        return selectedServers;
    }
}
