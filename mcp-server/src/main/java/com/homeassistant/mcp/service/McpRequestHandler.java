package com.homeassistant.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeassistant.mcp.config.McpProtocolProperties;
import com.homeassistant.mcp.config.McpServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpRequestHandler {

    private final McpServerProperties serverProperties;
    private final McpProtocolProperties protocolProperties;
    private final McpToolsService toolsService;
    private final ObjectMapper objectMapper;

    public JsonNode handleRequest(JsonNode request) throws Exception {
        String method = request.get("method").asText();
        JsonNode params = request.has("params") ? request.get("params") : objectMapper.createObjectNode();
        Object id = request.has("id") ? request.get("id") : null;

        log.debug("Handling MCP request: method={}, id={}", method, id);

        Object result = switch (method) {
            case "initialize" -> handleInitialize(params);
            case "tools/list" -> toolsService.listTools();
            case "tools/call" -> toolsService.callTool(params);
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };

        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.put("id", id);
        }
        response.put("result", result);

        return objectMapper.valueToTree(response);
    }

    private Map<String, Object> handleInitialize(JsonNode params) {
        log.info("MCP server initializing...");
        return Map.of(
            "protocolVersion", protocolProperties.getVersion(),
            "capabilities", Map.of(
                "tools", Map.of()
            ),
            "serverInfo", Map.of(
                "name", serverProperties.getName(),
                "version", serverProperties.getVersion()
            )
        );
    }

    public JsonNode handleError(Exception e, Object id) {
        log.error("Error processing request", e);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("jsonrpc", "2.0");
        if (id != null) {
            errorResponse.put("id", id);
        }
        errorResponse.put("error", Map.of(
            "code", -32603,
            "message", "Internal error: " + e.getMessage()
        ));
        return objectMapper.valueToTree(errorResponse);
    }
}
