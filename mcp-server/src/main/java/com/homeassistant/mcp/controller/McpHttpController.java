package com.homeassistant.mcp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeassistant.mcp.service.McpRequestHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpHttpController {

    private final McpRequestHandler requestHandler;
    private final ObjectMapper objectMapper;

    @PostMapping(
        value = "/message",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonNode handleMessage(@RequestBody JsonNode request) {
        try {
            log.debug("Received MCP request: {}", request);
            JsonNode response = requestHandler.handleRequest(request);
            log.debug("Sending MCP response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error handling MCP request", e);
            return requestHandler.handleError(e, request.has("id") ? request.get("id") : null);
        }
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
