package com.homeassistant.ai.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.homeassistant.ai.mcp.McpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.function.Function;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class QueryDevicesFunction {

    private final McpClient mcpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    @Description("Query and filter Home Assistant devices by domain and/or location. Returns a filtered list of matching entities with their current state.")
    public Function<QueryDevicesRequest, String> queryDevices() {
        return request -> {
            try {
                // Call MCP server's get_states tool
                JsonNode states = mcpClient.callTool("get_states", new HashMap<>());
                if (states == null || !states.isArray()) {
                    return "No entities found";
                }

                ArrayNode filteredResults = objectMapper.createArrayNode();
                String domainFilter = request.domain() != null ? request.domain().toLowerCase() : null;
                String locationFilter = request.location() != null ? request.location().toLowerCase() : null;

                log.info("Querying devices - domain: {}, location: {}", domainFilter, locationFilter);

                for (JsonNode entity : states) {
                    String entityId = entity.get("entity_id").asText();
                    String domain = entityId.split("\\.")[0];
                    
                    // Get friendly name if available
                    String friendlyName = "";
                    if (entity.has("attributes") && entity.get("attributes").has("friendly_name")) {
                        friendlyName = entity.get("attributes").get("friendly_name").asText();
                    }

                    // Apply domain filter
                    if (domainFilter != null && !domain.equals(domainFilter)) {
                        continue;
                    }

                    // Apply location filter (check entity_id and friendly_name)
                    if (locationFilter != null) {
                        boolean matchesLocation = entityId.toLowerCase().contains(locationFilter) ||
                                                 friendlyName.toLowerCase().contains(locationFilter);
                        if (!matchesLocation) {
                            continue;
                        }
                    }

                    // Create simplified entity response
                    ObjectNode simplified = objectMapper.createObjectNode();
                    simplified.put("entity_id", entityId);
                    simplified.put("friendly_name", friendlyName);
                    simplified.put("state", entity.get("state").asText());
                    simplified.put("domain", domain);
                    
                    filteredResults.add(simplified);
                }

                log.info("Found {} matching entities", filteredResults.size());

                if (filteredResults.isEmpty()) {
                    return String.format("No %s devices found%s", 
                        domainFilter != null ? domainFilter : "",
                        locationFilter != null ? " in " + locationFilter : "");
                }

                return filteredResults.toString();
            } catch (Exception e) {
                log.error("Error querying devices", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    public record QueryDevicesRequest(
        @Description("Device domain to filter by (e.g., 'light', 'switch', 'climate'). Leave null for all domains.") String domain,
        @Description("Location keyword to search for in entity names (e.g., 'kitchen', 'living room'). Leave null for all locations.") String location
    ) {}
}
