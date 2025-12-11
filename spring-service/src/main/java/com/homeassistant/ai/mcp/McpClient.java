package com.homeassistant.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class McpClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);
    private boolean initialized = false;
    private String mcpServerUrl;

    public synchronized void connect(String mcpServerUrl) throws IOException {
        if (initialized) {
            log.info("MCP client already connected");
            return;
        }

        this.mcpServerUrl = mcpServerUrl;
        log.info("Connecting to MCP server at: {}", mcpServerUrl);
        
        // Test connectivity
        try {
            // Health endpoint is at base URL + /health (e.g., http://localhost:8081/mcp/health)
            String healthUrl = mcpServerUrl + "/health";
            log.debug("Checking MCP server health at: {}", healthUrl);
            String response = restTemplate.getForObject(healthUrl, String.class);
            log.info("MCP server health check: {}", response);
        } catch (Exception e) {
            log.error("Health check failed for URL: {}", mcpServerUrl + "/health", e);
            throw new IOException("Failed to connect to MCP server at " + mcpServerUrl, e);
        }

        // Initialize the MCP connection
        initialize();
        
        log.info("MCP client connected successfully");
    }

    private void initialize() throws IOException {
        if (initialized) {
            return;
        }

        Map<String, Object> initRequest = new HashMap<>();
        initRequest.put("jsonrpc", "2.0");
        initRequest.put("method", "initialize");
        initRequest.put("id", requestId.getAndIncrement());
        
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("clientInfo", Map.of("name", "spring-ai-client", "version", "1.0.0"));
        initRequest.put("params", params);

        JsonNode response = sendHttpRequest(initRequest);
        log.debug("Initialize response: {}", response);
        
        initialized = true;
    }

    public JsonNode callTool(String toolName, Map<String, Object> arguments) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("MCP client not initialized");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("id", requestId.getAndIncrement());
        
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : new HashMap<>());
        request.put("params", params);

        log.info("\n" +
                "┌─────────────────────────────────────────────────────────────┐\n" +
                "│ 🔧 MCP TOOL CALL                                            │\n" +
                "├─────────────────────────────────────────────────────────────┤\n" +
                "│ Tool: {}\n" +
                "│ Arguments: {}\n" +
                "└─────────────────────────────────────────────────────────────┘",
                toolName, objectMapper.writeValueAsString(arguments));
        
        JsonNode jsonResponse = sendHttpRequest(request);
        
        log.info("\n" +
                "┌─────────────────────────────────────────────────────────────┐\n" +
                "│ ✅ MCP TOOL RESPONSE                                        │\n" +
                "├─────────────────────────────────────────────────────────────┤\n" +
                "│ Tool: {}\n" +
                "│ Response Size: {} bytes\n" +
                "└─────────────────────────────────────────────────────────────┘",
                toolName, jsonResponse.toString().length());
        
        if (jsonResponse.has("error")) {
            throw new IOException("MCP error: " + jsonResponse.get("error"));
        }
        
        if (jsonResponse.has("result") && jsonResponse.get("result").has("content")) {
            JsonNode content = jsonResponse.get("result").get("content");
            if (content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                if (firstContent.has("text")) {
                    String text = firstContent.get("text").asText();
                    return objectMapper.readTree(text);
                }
            }
        }
        
        return jsonResponse.get("result");
    }

    private JsonNode sendHttpRequest(Map<String, Object> request) throws IOException {
        try {
            String url = mcpServerUrl + "/message";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.trace("Sending HTTP request to {}: {}", url, request);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IOException("MCP server returned error: " + response.getStatusCode());
            }
            
            log.trace("Received HTTP response: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            throw new IOException("Failed to send request to MCP server", e);
        }
    }

    public synchronized void disconnect() {
        initialized = false;
        log.info("MCP client disconnected");
    }

    public boolean isConnected() {
        if (!initialized || mcpServerUrl == null) {
            return false;
        }
        
        try {
            String healthUrl = mcpServerUrl + "/health";
            restTemplate.getForObject(healthUrl, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
