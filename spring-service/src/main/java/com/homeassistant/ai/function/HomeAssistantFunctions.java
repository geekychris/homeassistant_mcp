package com.homeassistant.ai.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.homeassistant.ai.mcp.McpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HomeAssistantFunctions {

    private final McpClient mcpClient;

    @Bean
    @Description("Get all entity states from Home Assistant")
    public Function<GetStatesRequest, String> getStates() {
        return request -> {
            try {
                log.info("🏠 Function: getStates() - Fetching all Home Assistant entity states");
                JsonNode states = mcpClient.callTool("get_states", new HashMap<>());
                if (states != null) {
                    int entityCount = states.isArray() ? states.size() : 0;
                    log.info("✓ getStates() returned {} entities", entityCount);
                    return states.toString();
                }
                return "No states found";
            } catch (Exception e) {
                log.error("✗ Error in getStates()", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get the state of a specific Home Assistant entity")
    public Function<GetStateRequest, String> getState() {
        return request -> {
            try {
                log.info("🏠 Function: getState(entityId='{}') - Fetching entity state", request.entityId());
                Map<String, Object> args = new HashMap<>();
                args.put("entity_id", request.entityId());
                JsonNode state = mcpClient.callTool("get_state", args);
                if (state != null) {
                    log.info("✓ getState() succeeded for entity: {}", request.entityId());
                    return state.toString();
                }
                log.warn("✗ Entity not found: {}", request.entityId());
                return "Entity not found: " + request.entityId();
            } catch (Exception e) {
                log.error("✗ Error in getState() for entity: {}", request.entityId(), e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Call a Home Assistant service to control a device")
    public Function<CallServiceRequest, String> callService() {
        return request -> {
            try {
                log.info("🏠 Function: callService(domain='{}', service='{}', entityId='{}', temperature={}, brightness={}) - Controlling device",
                        request.domain(), request.service(), request.entityId(), 
                        request.temperature(), request.brightness());
                
                // Validate entity_id is present
                if (request.entityId() == null || request.entityId().isEmpty()) {
                    log.error("⚠️ CRITICAL: callService called WITHOUT entity_id! Domain: {}, Service: {}", 
                            request.domain(), request.service());
                    return "Error: entity_id is required for service calls. Please specify which device to control.";
                }
                
                Map<String, Object> args = new HashMap<>();
                args.put("domain", request.domain());
                args.put("service", request.service());
                args.put("entity_id", request.entityId());
                
                // Add optional service data
                if (request.temperature() != null) {
                    args.put("temperature", request.temperature());
                }
                if (request.brightness() != null) {
                    args.put("brightness", request.brightness());
                }
                if (request.hvac_mode() != null) {
                    args.put("hvac_mode", request.hvac_mode());
                }
                if (request.rgb_color() != null) {
                    args.put("rgb_color", request.rgb_color());
                }
                
                log.info("   ↳ Executing: {}.{} on {} with args: {}", 
                    request.domain(), request.service(), request.entityId(), args);
                
                JsonNode result = mcpClient.callTool("call_service", args);
                
                log.info("✓ callService() succeeded: {}.{} on {}",
                        request.domain(), request.service(), request.entityId());
                
                return "Service called successfully: " + request.domain() + "." + request.service() + 
                       (request.entityId() != null ? " on " + request.entityId() : "");
            } catch (Exception e) {
                log.error("✗ Error in callService() for {}.{}", request.domain(), request.service(), e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Get all available Home Assistant services")
    public Function<GetServicesRequest, String> getServices() {
        return request -> {
            try {
                log.info("🏠 Function: getServices() - Fetching available Home Assistant services");
                JsonNode services = mcpClient.callTool("get_services", new HashMap<>());
                if (services != null) {
                    log.info("✓ getServices() returned service list");
                    return services.toString();
                }
                return "No services found";
            } catch (Exception e) {
                log.error("✗ Error in getServices()", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    // Request records
    public record GetStatesRequest() {}
    
    public record GetStateRequest(
        @Description("The entity ID (e.g., light.living_room)") String entityId
    ) {}
    
    public record CallServiceRequest(
        @Description("Service domain (e.g., light, switch, climate)") String domain,
        @Description("Service name (e.g., turn_on, turn_off, set_temperature)") String service,
        @Description("Target entity ID (optional if controlling multiple entities)") String entityId,
        @Description("Temperature value for climate.set_temperature (optional)") Double temperature,
        @Description("Brightness value 0-255 for lights (optional)") Integer brightness,
        @Description("HVAC mode: heat, cool, auto, off (optional)") String hvac_mode,
        @Description("RGB color as [r,g,b] array, e.g., [255,0,0] for red (optional)") int[] rgb_color
    ) {}
    
    public record GetServicesRequest() {}
}
