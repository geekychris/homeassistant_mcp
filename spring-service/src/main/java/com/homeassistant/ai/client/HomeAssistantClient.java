package com.homeassistant.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.homeassistant.ai.config.HomeAssistantProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class HomeAssistantClient {

    private final WebClient webClient;

    public HomeAssistantClient(HomeAssistantProperties properties) {
        // Increase buffer size to handle large responses (Home Assistant can return many entities)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
        
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getAccessToken())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .exchangeStrategies(strategies)
                .build();
        log.info("HomeAssistantClient initialized for {}", properties.getBaseUrl());
    }

    public Mono<JsonNode> getStates() {
        return webClient.get()
                .uri("/api/states")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(result -> log.debug("Successfully retrieved {} states", result.size()))
                .doOnError(error -> log.error("Error getting states: {}", error.getMessage(), error));
    }

    public Mono<JsonNode> getState(String entityId) {
        return webClient.get()
                .uri("/api/states/{entityId}", entityId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> log.error("Error getting state for {}", entityId, error));
    }

    public Mono<JsonNode> callService(String domain, String service, Map<String, Object> data) {
        return webClient.post()
                .uri("/api/services/{domain}/{service}", domain, service)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> log.error("Error calling service {}.{}", domain, service, error));
    }

    public Mono<JsonNode> getServices() {
        return webClient.get()
                .uri("/api/services")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> log.error("Error getting services", error));
    }

    public Mono<JsonNode> getConfig() {
        return webClient.get()
                .uri("/api/config")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(error -> log.error("Error getting config", error));
    }
}
