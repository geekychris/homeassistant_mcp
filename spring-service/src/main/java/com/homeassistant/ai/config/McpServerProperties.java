package com.homeassistant.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp.server")
public class McpServerProperties {
    private String url = "http://localhost:8081/mcp";
}
