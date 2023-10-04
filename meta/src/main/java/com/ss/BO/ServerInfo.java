package com.ss.BO;

import ch.qos.logback.core.pattern.color.BoldBlueCompositeConverter;
import lombok.Data;

import java.util.Objects;

@Data
public class ServerInfo {
    private String serviceId;
    private String host;
    private Integer port;
    private Long preTimeStamp;
    private Boolean alive;
    private String schema;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerInfo)) return false;
        ServerInfo info = (ServerInfo) o;
        return Objects.equals(serviceId, info.serviceId) && Objects.equals(host, info.host) && Objects.equals(port, info.port) && Objects.equals(schema, info.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, host, port, schema);
    }
}
