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

    @Bean
    @Description("Set thermostat temperature")
    public Function<SetThermostatRequest, String> setThermostat() {
        return request -> {
            try {
                log.info("🌡️ Function: setThermostat(entityId='{}', temperature={}, hvacMode='{}') - Setting climate control",
                        request.entityId(), request.temperature(), request.hvacMode());
                
                if (request.entityId() == null || request.entityId().isEmpty()) {
                    log.error("⚠️ CRITICAL: setThermostat called WITHOUT entity_id!");
                    return "Error: entity_id is required. Please specify which thermostat to control.";
                }
                
                Map<String, Object> args = new HashMap<>();
                args.put("domain", "climate");
                args.put("service", "set_temperature");
                args.put("entity_id", request.entityId());
                
                // Build service data separately
                Map<String, Object> serviceData = new HashMap<>();
                if (request.temperature() != null) {
                    serviceData.put("temperature", request.temperature());
                }
                if (request.hvacMode() != null && !request.hvacMode().isEmpty()) {
                    serviceData.put("hvac_mode", request.hvacMode());
                }
                
                // Add service data if not empty
                if (!serviceData.isEmpty()) {
                    args.put("data", serviceData);
                }
                
                log.info("   ↳ Executing: climate.set_temperature with args: {}", args);
                JsonNode result = mcpClient.callTool("call_service", args);
                
                log.info("✓ setThermostat() succeeded for {}", request.entityId());
                return String.format("Set thermostat %s to %.1f°F", request.entityId(), request.temperature());
            } catch (Exception e) {
                log.error("✗ Error in setThermostat()", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Turn lights or switches on/off (for simple non-dimmable lights and switches)")
    public Function<ControlSwitchRequest, String> turnOnOff() {
        return request -> {
            try {
                log.info("🔆 Function: turnOnOff(entityId='{}', action='{}') - Controlling switch/light",
                        request.entityId(), request.action());
                
                if (request.entityId() == null || request.entityId().isEmpty()) {
                    log.error("⚠️ CRITICAL: turnOnOff called WITHOUT entity_id!");
                    return "Error: entity_id is required. Please specify which device to control.";
                }
                
                // Auto-detect domain from entity_id
                String domain = request.entityId().split("\\.")[0];
                String rawAction = request.action() != null ? request.action() : "turn_on";
                
                // Normalize action to proper Home Assistant service names
                String service;
                if (rawAction.equalsIgnoreCase("on") || rawAction.equalsIgnoreCase("turn_on")) {
                    service = "turn_on";
                } else if (rawAction.equalsIgnoreCase("off") || rawAction.equalsIgnoreCase("turn_off")) {
                    service = "turn_off";
                } else {
                    service = rawAction; // Pass through as-is if not recognized
                }
                
                Map<String, Object> args = new HashMap<>();
                args.put("domain", domain);
                args.put("service", service);
                args.put("entity_id", request.entityId());
                
                log.info("   ↳ Executing: {}.{} on {}", domain, service, request.entityId());
                JsonNode result = mcpClient.callTool("call_service", args);
                
                log.info("✓ turnOnOff() succeeded for {}", request.entityId());
                return String.format("%s - action: %s", request.entityId(), service);
            } catch (Exception e) {
                log.error("✗ Error in turnOnOff()", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Control dimmable lights - set brightness and color")
    public Function<ControlDimmableLightRequest, String> controlDimmableLight() {
        return request -> {
            try {
                log.info("💡 Function: controlDimmableLight(entityId='{}', brightness={}, rgb={})",
                        request.entityId(), request.brightness(), 
                        request.rgb() != null ? java.util.Arrays.toString(request.rgb()) : "null");
                
                if (request.entityId() == null || request.entityId().isEmpty()) {
                    log.error("⚠️ CRITICAL: controlDimmableLight called WITHOUT entity_id!");
                    return "Error: entity_id is required. Please specify which light to control.";
                }
                
                Map<String, Object> args = new HashMap<>();
                args.put("domain", "light");
                args.put("service", "turn_on");
                args.put("entity_id", request.entityId());
                
                // Build service data separately
                Map<String, Object> serviceData = new HashMap<>();
                if (request.brightness() != null) {
                    serviceData.put("brightness", request.brightness());
                }
                if (request.rgb() != null && request.rgb().length == 3) {
                    serviceData.put("rgb_color", request.rgb());
                }
                
                // Add service data if not empty
                if (!serviceData.isEmpty()) {
                    args.put("data", serviceData);
                }
                
                log.info("   ↳ Executing: light.turn_on with args: {}", args);
                JsonNode result = mcpClient.callTool("call_service", args);
                
                log.info("✓ controlDimmableLight() succeeded for {}", request.entityId());
                return String.format("Set dimmable light %s", request.entityId());
            } catch (Exception e) {
                log.error("✗ Error in controlDimmableLight()", e);
                return "Error: " + e.getMessage();
            }
        };
    }

    @Bean
    @Description("Control media players - play, pause, stop, volume")
    public Function<ControlMediaPlayerRequest, String> controlMediaPlayer() {
        return request -> {
            try {
                log.info("📺 Function: controlMediaPlayer(entityId='{}', action='{}', volume={})",
                        request.entityId(), request.action(), request.volumeLevel());
                
                if (request.entityId() == null || request.entityId().isEmpty()) {
                    log.error("⚠️ CRITICAL: controlMediaPlayer called WITHOUT entity_id!");
                    return "Error: entity_id is required. Please specify which media player to control.";
                }
                
                Map<String, Object> args = new HashMap<>();
                args.put("domain", "media_player");
                args.put("entity_id", request.entityId());
                
                // Map actions to services
                String service = switch (request.action() != null ? request.action() : "play") {
                    case "play" -> "media_play";
                    case "pause" -> "media_pause";
                    case "stop" -> "media_stop";
                    case "next" -> "media_next_track";
                    case "previous" -> "media_previous_track";
                    case "volume" -> "volume_set";
                    default -> "media_play";
                };
                args.put("service", service);
                
                // Build service data if needed
                if (service.equals("volume_set") && request.volumeLevel() != null) {
                    Map<String, Object> serviceData = new HashMap<>();
                    serviceData.put("volume_level", request.volumeLevel());
                    args.put("data", serviceData);
                }
                
                log.info("   ↳ Executing: media_player.{} with args: {}", service, args);
                JsonNode result = mcpClient.callTool("call_service", args);
                
                log.info("✓ controlMediaPlayer() succeeded for {}", request.entityId());
                return String.format("Media player %s - action: %s", request.entityId(), request.action());
            } catch (Exception e) {
                log.error("✗ Error in controlMediaPlayer()", e);
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
    
    public record SetThermostatRequest(
        @Description("The climate entity ID (e.g., climate.living_room)") String entityId,
        @Description("Target temperature in Fahrenheit (e.g., 72.0)") Double temperature,
        @Description("HVAC mode: heat, cool, heat_cool, auto, off (optional)") String hvacMode
    ) {}
    
    public record ControlSwitchRequest(
        @Description("The entity ID - works with both lights and switches (e.g., light.kitchen, switch.porch)") String entityId,
        @Description("Action: turn_on, turn_off, toggle") String action
    ) {}
    
    public record ControlDimmableLightRequest(
        @Description("The dimmable light entity ID (e.g., light.kitchen)") String entityId,
        @Description("Brightness 0-255 (e.g., 128 for 50%, 255 for 100%)") Integer brightness,
        @Description("RGB color [r,g,b] e.g., [255,0,0] for red (optional)") int[] rgb
    ) {}
    
    public record ControlMediaPlayerRequest(
        @Description("The media player entity ID (e.g., media_player.living_room_tv)") String entityId,
        @Description("Action: play, pause, stop, next, previous, volume") String action,
        @Description("Volume level 0.0-1.0 (required for volume action)") Double volumeLevel
    ) {}
}
