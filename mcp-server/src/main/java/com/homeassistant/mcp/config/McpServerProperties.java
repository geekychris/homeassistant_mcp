package com.homeassistant.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerProperties {
    private String name = "homeassistant-mcp-server";
    private String version = "1.0.0";
}
