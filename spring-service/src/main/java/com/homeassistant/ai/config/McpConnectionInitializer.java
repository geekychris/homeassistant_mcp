package com.homeassistant.ai.config;

import com.homeassistant.ai.mcp.McpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpConnectionInitializer {

    private final McpClient mcpClient;
    private final McpServerProperties mcpServerProperties;

    @PostConstruct
    public void connect() {
        try {
            String mcpServerUrl = mcpServerProperties.getUrl();
            log.info("Connecting to MCP server at: {}", mcpServerUrl);
            mcpClient.connect(mcpServerUrl);
            log.info("MCP client connected and ready");
        } catch (IOException e) {
            log.error("Failed to connect to MCP server", e);
            log.error("Please make sure MCP server is running at: {}", mcpServerProperties.getUrl());
            throw new RuntimeException("Failed to connect to MCP server", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        log.info("Disconnecting from MCP server");
        mcpClient.disconnect();
    }
}
