package com.homeassistant.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homeassistant.mcp.config.HomeAssistantProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class HomeAssistantClient {
    private final OkHttpClient client;
    private final String baseUrl;
    private final String accessToken;
    private final ObjectMapper objectMapper;

    public HomeAssistantClient(HomeAssistantProperties properties, ObjectMapper objectMapper) {
        this.client = new OkHttpClient();
        this.baseUrl = properties.getBaseUrl();
        this.accessToken = properties.getAccessToken();
        this.objectMapper = objectMapper;
        log.info("Home Assistant client initialized for: {}", baseUrl);
    }

    public JsonNode getStates() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/states")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    public JsonNode getState(String entityId) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/states/" + entityId)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    public JsonNode callService(String domain, String service, Map<String, Object> data) throws IOException {
        String jsonData = objectMapper.writeValueAsString(data);
        String url = baseUrl + "/api/services/" + domain + "/" + service;
        
        log.debug("Calling service: {} with data: {}", url, jsonData);
        
        RequestBody body = RequestBody.create(
                jsonData,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("Home Assistant returned {}: {}", response.code(), responseBody);
                throw new IOException("Unexpected response code: " + response + " Body: " + responseBody);
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    public JsonNode getServices() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/services")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    public JsonNode getConfig() throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/config")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return objectMapper.readTree(response.body().string());
        }
    }
}
