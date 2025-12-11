package com.homeassistant.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        log.info("Starting Home Assistant MCP Server (HTTP)");
        SpringApplication.run(McpServerApplication.class, args);
    }
}
