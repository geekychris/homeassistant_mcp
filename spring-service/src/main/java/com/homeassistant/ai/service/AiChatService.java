package com.homeassistant.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatModel chatModel;
    private ChatClient chatClient;
    
    // Store conversation history per session (sessionId -> messages)
    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 50; // Keep last 50 messages

    public String chat(String userMessage) {
        return chat("default", userMessage);
    }
    
    public String chat(String sessionId, String userMessage) {
        try {
            // Initialize chat client with Home Assistant functions if not already done
            if (chatClient == null) {
                chatClient = ChatClient.builder(chatModel)
                    .defaultSystem("""
                        You are a helpful assistant for controlling a Home Assistant smart home.
                        
                        CRITICAL RULES:
                        1. ALWAYS include the full entity_id (e.g., climate.living_room_thermostat) when calling services
                        2. REMEMBER entity IDs from previous queries and use them in follow-up commands
                        3. When a user refers to a device mentioned earlier ("it", "the thermostat", "that light"), use the entity_id from the conversation history
                        
                        INSTRUCTIONS:
                        1. Finding devices:
                           - Use queryDevices(domain, location) to find matching devices
                           - domain: device type ("light", "switch", "climate", etc.)
                           - location: location keyword ("kitchen", "bedroom", "living room", etc.)
                           - Example: queryDevices("light", "kitchen") finds all kitchen lights
                           - REMEMBER the entity IDs returned for use in follow-up commands
                        
                        2. Controlling devices:
                           - ALWAYS use callService with the FULL entity_id
                           - callService(domain, service, entityId, temperature, brightness, hvac_mode, rgb_color)
                           - domain: "light", "switch", "climate", etc.
                           - service: "turn_on", "turn_off", "toggle", "set_temperature"
                           - entityId: REQUIRED - the full entity ID like "climate.living_room_thermostat"
                           - temperature: (optional) for climate.set_temperature (e.g., 72.0)
                           - brightness: (optional) for lights, 0-255
                           - hvac_mode: (optional) "heat", "cool", "auto", "off"
                           - rgb_color: (optional) [r,g,b] array for light colors
                        
                        3. Context tracking:
                           - If user says "turn it down" or "set it to 70", look back in conversation for the entity_id
                           - Example conversation:
                             User: "What's the living room thermostat temperature?"
                             You: [query climate.living_room_thermostat]
                             User: "Set it to 72"
                             You: [use climate.living_room_thermostat from earlier]
                        
                        4. Getting device info:
                           - Use getState(entityId) for detailed information
                        
                        5. Presenting results:
                           - Show friendly names when available
                           - Show current state (on/off/temperature/etc.)
                           - Be concise and helpful
                        
                        Available functions:
                        - queryDevices(domain, location): Filter devices by type and location
                        - getState(entityId): Get detailed info for one device
                        - callService(domain, service, entityId, temperature, brightness, hvac_mode, rgb_color): Control a device
                        - getServices(): List all available services
                        """)
                    .defaultFunctions("queryDevices", "getState", "callService", "getServices")
                    .build();
                
                log.info("ChatClient initialized with Home Assistant functions");
            }
            
            // Get or create conversation history for this session
            List<Message> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
            
            // Add user message to history
            history.add(new UserMessage(userMessage));
            
            log.info("\n" +
                    "═══════════════════════════════════════════════════════════════\n" +
                    "📨 USER QUERY\n" +
                    "Session: {}\n" +
                    "Message: {}\n" +
                    "History Size: {} messages\n" +
                    "═══════════════════════════════════════════════════════════════",
                    sessionId, userMessage, history.size());
            
            // Build prompt with conversation history
            var promptBuilder = chatClient.prompt()
                .messages(history);
            
            // Call the AI with function calling enabled
            String response = promptBuilder
                .call()
                .content();
            
            // Add assistant response to history
            history.add(new AssistantMessage(response));
            
            // Trim history if it gets too large
            if (history.size() > MAX_HISTORY_SIZE) {
                // Keep the most recent messages
                List<Message> trimmed = new ArrayList<>(history.subList(history.size() - MAX_HISTORY_SIZE, history.size()));
                conversationHistory.put(sessionId, trimmed);
            }
            
            log.info("\n" +
                    "───────────────────────────────────────────────────────────────\n" +
                    "🤖 AI RESPONSE\n" +
                    "Response: {}\n" +
                    "History Size: {} messages\n" +
                    "───────────────────────────────────────────────────────────────",
                    response.length() > 200 ? response.substring(0, 200) + "..." : response,
                    history.size());
            return response;
        } catch (Exception e) {
            log.error("Error in AI chat", e);
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }

    public void clearHistory() {
        clearHistory("default");
    }
    
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        log.info("Cleared conversation history for session: {}", sessionId);
    }
    
    public int getHistorySize(String sessionId) {
        List<Message> history = conversationHistory.get(sessionId);
        return history != null ? history.size() : 0;
    }
}
