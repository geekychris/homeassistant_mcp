package com.homeassistant.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeassistant.mcp.HomeAssistantClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolsService {

    private final HomeAssistantClient haClient;
    private final ObjectMapper objectMapper;

    public Map<String, Object> listTools() {
        return Map.of(
            "tools", new Object[]{
                Map.of(
                    "name", "get_states",
                    "description", "Get all entity states from Home Assistant",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of()
                    )
                ),
                Map.of(
                    "name", "get_state",
                    "description", "Get the state of a specific entity",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "entity_id", Map.of(
                                "type", "string",
                                "description", "The entity ID (e.g., light.living_room)"
                            )
                        ),
                        "required", new String[]{"entity_id"}
                    )
                ),
                Map.of(
                    "name", "call_service",
                    "description", "Call a Home Assistant service",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "domain", Map.of(
                                "type", "string",
                                "description", "Service domain (e.g., light, switch)"
                            ),
                            "service", Map.of(
                                "type", "string",
                                "description", "Service name (e.g., turn_on, turn_off)"
                            ),
                            "entity_id", Map.of(
                                "type", "string",
                                "description", "Target entity ID"
                            ),
                            "data", Map.of(
                                "type", "object",
                                "description", "Additional service data"
                            )
                        ),
                        "required", new String[]{"domain", "service"}
                    )
                ),
                Map.of(
                    "name", "get_services",
                    "description", "Get all available services",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of()
                    )
                ),
                Map.of(
                    "name", "get_config",
                    "description", "Get Home Assistant configuration",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of()
                    )
                )
            }
        );
    }

    public Map<String, Object> callTool(JsonNode params) throws Exception {
        String toolName = params.get("name").asText();
        JsonNode arguments = params.has("arguments") ? params.get("arguments") : objectMapper.createObjectNode();

        log.debug("Calling tool: {}", toolName);

        JsonNode result = switch (toolName) {
            case "get_states" -> haClient.getStates();
            case "get_state" -> haClient.getState(arguments.get("entity_id").asText());
            case "call_service" -> {
                String domain = arguments.get("domain").asText();
                String service = arguments.get("service").asText();
                Map<String, Object> data = new HashMap<>();
                
                if (arguments.has("entity_id")) {
                    data.put("entity_id", arguments.get("entity_id").asText());
                }
                if (arguments.has("data")) {
                    data.putAll(objectMapper.convertValue(arguments.get("data"), Map.class));
                }
                
                yield haClient.callService(domain, service, data);
            }
            case "get_services" -> haClient.getServices();
            case "get_config" -> haClient.getConfig();
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };

        return Map.of(
            "content", new Object[]{
                Map.of(
                    "type", "text",
                    "text", objectMapper.writeValueAsString(result)
                )
            }
        );
    }
}
