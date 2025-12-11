package com.homeassistant.ai.controller;

import com.homeassistant.ai.service.AiChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = aiChatService.chat(request.getMessage());
        return new ChatResponse(response);
    }

    @PostMapping("/clear")
    public void clearHistory() {
        aiChatService.clearHistory();
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private final String response;
    }
}
