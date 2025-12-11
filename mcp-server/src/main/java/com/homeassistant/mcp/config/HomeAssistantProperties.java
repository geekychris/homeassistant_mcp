package com.homeassistant.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "homeassistant")
public class HomeAssistantProperties {
    private String baseUrl;
    private String accessToken;
}
