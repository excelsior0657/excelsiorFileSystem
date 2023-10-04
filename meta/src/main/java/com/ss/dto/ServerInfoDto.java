package com.ss.dto;

import lombok.Data;

/**
 * @author Excelsior
 */
@Data
public class ServerInfoDto {
    private String serviceId;
    private String host;
    private Integer port;
    private String schema = "http";
}
