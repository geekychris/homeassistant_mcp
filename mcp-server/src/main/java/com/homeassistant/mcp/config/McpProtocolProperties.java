package com.homeassistant.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mcp.protocol")
public class McpProtocolProperties {
    private String version = "2024-11-05";
}
