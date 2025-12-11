package com.homeassistant.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.homeassistant.ai.client.HomeAssistantClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/homeassistant")
@RequiredArgsConstructor
public class HomeAssistantController {

    private final HomeAssistantClient haClient;

    @GetMapping("/states")
    public Mono<JsonNode> getStates() {
        return haClient.getStates();
    }

    @GetMapping("/states/{entityId}")
    public Mono<JsonNode> getState(@PathVariable String entityId) {
        return haClient.getState(entityId);
    }

    @PostMapping("/services/{domain}/{service}")
    public Mono<JsonNode> callService(
            @PathVariable String domain,
            @PathVariable String service,
            @RequestBody Map<String, Object> data) {
        return haClient.callService(domain, service, data);
    }

    @GetMapping("/services")
    public Mono<JsonNode> getServices() {
        return haClient.getServices();
    }

    @GetMapping("/config")
    public Mono<JsonNode> getConfig() {
        return haClient.getConfig();
    }
}
