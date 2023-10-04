package com.ss.controller;

import com.ss.BO.ServerInfo;
import com.ss.dto.ServerInfoDto;
import com.ss.response.CommonResponse;
import com.ss.service.DiscoveryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @PostMapping("/register")
    public CommonResponse<?> register(@RequestBody ServerInfoDto serverInfo){
        discoveryService.register(serverInfo);
        return CommonResponse.success();
    }

    @PutMapping("/heartbeat")
    public void heartbeat(@RequestBody ServerInfoDto serverInfo){
        discoveryService.heartbeat(serverInfo);
    }

    @GetMapping("/services")
    public CommonResponse<?> services(){
        Map<String, List<ServerInfo>> map = discoveryService.services();
        return CommonResponse.success(map);
    }
}
