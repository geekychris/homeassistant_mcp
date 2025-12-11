package com.homeassistant.ai.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.homeassistant.ai.mcp.McpClient;
import com.homeassistant.ai.service.AiChatService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Route("")
public class MainView extends VerticalLayout {

    private final AiChatService aiChatService;
    private final McpClient mcpClient;
    private final VerticalLayout chatMessages;
    private final VerticalLayout deviceList;
    private final String sessionId = "web-session"; // Could be per-user in real app

    public MainView(AiChatService aiChatService, McpClient mcpClient) {
        this.aiChatService = aiChatService;
        this.mcpClient = mcpClient;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        H1 header = new H1("Home Assistant AI Control");
        add(header);

        // Main content layout
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);

        // Left side - Chat interface
        VerticalLayout chatSection = createChatSection();
        chatSection.setWidth("60%");

        // Right side - Device list
        VerticalLayout devicesSection = createDevicesSection();
        devicesSection.setWidth("40%");

        mainLayout.add(chatSection, devicesSection);
        add(mainLayout);

        this.chatMessages = (VerticalLayout) chatSection.getComponentAt(1);
        this.deviceList = (VerticalLayout) devicesSection.getComponentAt(1);

        // Load initial device states
        loadDevices();
    }

    private VerticalLayout createChatSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.getStyle().set("border", "1px solid #ddd");
        layout.getStyle().set("border-radius", "8px");

        H2 title = new H2("AI Assistant");
        layout.add(title);

        // Chat messages area
        VerticalLayout messages = new VerticalLayout();
        messages.setWidth("100%");
        messages.getStyle().set("overflow-y", "auto");
        messages.getStyle().set("max-height", "500px");
        messages.getStyle().set("border", "1px solid #eee");
        messages.getStyle().set("padding", "10px");
        messages.getStyle().set("border-radius", "4px");
        layout.add(messages);

        // Input area
        HorizontalLayout inputLayout = new HorizontalLayout();
        inputLayout.setWidth("100%");

        TextField messageField = new TextField();
        messageField.setPlaceholder("Ask me anything about your home...");
        messageField.setWidth("100%");

        Button sendButton = new Button("Send");
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearButton = new Button("Clear History");
        clearButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        clearButton.getElement().setAttribute("title", "Clear conversation history");

        inputLayout.add(messageField, sendButton, clearButton);
        layout.add(inputLayout);
        
        // Add history info
        Div historyInfo = new Div();
        historyInfo.getStyle().set("font-size", "0.9em");
        historyInfo.getStyle().set("color", "#666");
        historyInfo.getStyle().set("margin-top", "5px");
        historyInfo.setText("Conversation memory enabled - your chat history is remembered");
        layout.add(historyInfo);

        // Send button action
        sendButton.addClickListener(e -> {
            String message = messageField.getValue();
            if (message != null && !message.trim().isEmpty()) {
                addChatMessage("You", message, true);
                messageField.clear();

                // Get AI response with session-based memory
                String response = aiChatService.chat(sessionId, message);
                addChatMessage("Assistant", response, false);
                
                // Update history info
                int historySize = aiChatService.getHistorySize(sessionId);
                historyInfo.setText(String.format("Conversation memory: %d messages stored", historySize));

                // Refresh device list
                loadDevices();
            }
        });

        // Clear button action
        clearButton.addClickListener(e -> {
            messages.removeAll();
            aiChatService.clearHistory(sessionId);
            historyInfo.setText("Conversation memory cleared - starting fresh");
        });

        // Enter key support
        messageField.addKeyPressListener(event -> {
            if (event.getKey().toString().equals("Enter")) {
                sendButton.click();
            }
        });

        return layout;
    }

    private VerticalLayout createDevicesSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.getStyle().set("border", "1px solid #ddd");
        layout.getStyle().set("border-radius", "8px");

        H2 title = new H2("Devices");
        Button refreshButton = new Button("Refresh");
        refreshButton.addClickListener(e -> loadDevices());

        HorizontalLayout headerLayout = new HorizontalLayout(title, refreshButton);
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        layout.add(headerLayout);

        VerticalLayout devices = new VerticalLayout();
        devices.setWidth("100%");
        devices.getStyle().set("overflow-y", "auto");
        devices.getStyle().set("max-height", "600px");
        layout.add(devices);

        return layout;
    }

    private void addChatMessage(String sender, String message, boolean isUser) {
        Div messageDiv = new Div();
        messageDiv.getStyle().set("padding", "10px");
        messageDiv.getStyle().set("margin", "5px");
        messageDiv.getStyle().set("border-radius", "8px");
        messageDiv.getStyle().set("background-color", isUser ? "#e3f2fd" : "#f5f5f5");

        Paragraph senderLabel = new Paragraph(sender + ":");
        senderLabel.getStyle().set("font-weight", "bold");
        senderLabel.getStyle().set("margin", "0 0 5px 0");

        Paragraph messageText = new Paragraph(message);
        messageText.getStyle().set("margin", "0");
        messageText.getStyle().set("white-space", "pre-wrap");

        messageDiv.add(senderLabel, messageText);
        chatMessages.add(messageDiv);
    }

    private void loadDevices() {
        try {
            log.info("Loading devices from Home Assistant via MCP...");
            JsonNode states = mcpClient.callTool("get_states", new HashMap<>());
            deviceList.removeAll();

            if (states != null && states.isArray()) {
                log.info("Retrieved {} entities from Home Assistant", states.size());
                for (JsonNode state : states) {
                    String entityId = state.get("entity_id").asText();
                    String stateValue = state.get("state").asText();
                    String domain = entityId.split("\\.")[0];

                    // Show all controllable entities (exclude sensors, binary_sensors, etc.)
                    if (domain.equals("light") || domain.equals("switch") || 
                        domain.equals("fan") || domain.equals("cover") ||
                        domain.equals("media_player") || domain.equals("climate") ||
                        domain.equals("lock") || domain.equals("vacuum") ||
                        domain.equals("scene") || domain.equals("script") ||
                        domain.equals("input_boolean") || domain.equals("input_select") ||
                        domain.equals("automation")) {
                        addDeviceCard(entityId, stateValue, domain);
                    }
                }
            } else {
                log.warn("States is null or not an array");
            }
        } catch (Exception e) {
            log.error("Error loading devices: " + e.getClass().getName() + ": " + e.getMessage(), e);
            Paragraph error = new Paragraph("Error loading devices: " + e.getMessage());
            error.getStyle().set("color", "red");
            deviceList.add(error);
            
            // Add more detailed error info
            if (e.getCause() != null) {
                Paragraph causeError = new Paragraph("Caused by: " + e.getCause().getMessage());
                causeError.getStyle().set("color", "red");
                causeError.getStyle().set("font-size", "0.9em");
                deviceList.add(causeError);
            }
        }
    }

    private void addDeviceCard(String entityId, String state, String domain) {
        HorizontalLayout card = new HorizontalLayout();
        card.setWidth("100%");
        card.getStyle().set("padding", "10px");
        card.getStyle().set("border", "1px solid #ddd");
        card.getStyle().set("border-radius", "4px");
        card.getStyle().set("margin-bottom", "8px");
        card.setJustifyContentMode(JustifyContentMode.BETWEEN);
        card.setAlignItems(Alignment.CENTER);

        VerticalLayout info = new VerticalLayout();
        info.setSpacing(false);
        info.setPadding(false);

        Paragraph name = new Paragraph(entityId);
        name.getStyle().set("font-weight", "bold");
        name.getStyle().set("margin", "0");

        Paragraph stateText = new Paragraph("State: " + state);
        stateText.getStyle().set("margin", "0");
        stateText.getStyle().set("color", "#666");

        info.add(name, stateText);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);

        // Add domain-specific controls
        if (domain.equals("media_player")) {
            Button playButton = new Button("Play");
            playButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            playButton.addClickListener(e -> controlDevice(entityId, domain, "media_play"));
            
            Button pauseButton = new Button("Pause");
            pauseButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            pauseButton.addClickListener(e -> controlDevice(entityId, domain, "media_pause"));
            
            Button stopButton = new Button("Stop");
            stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            stopButton.addClickListener(e -> controlDevice(entityId, domain, "media_stop"));
            
            buttons.add(playButton, pauseButton, stopButton);
        } else if (domain.equals("lock")) {
            Button lockButton = new Button("Lock");
            lockButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            lockButton.addClickListener(e -> controlDevice(entityId, domain, "lock"));
            
            Button unlockButton = new Button("Unlock");
            unlockButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            unlockButton.addClickListener(e -> controlDevice(entityId, domain, "unlock"));
            
            buttons.add(lockButton, unlockButton);
        } else if (domain.equals("scene") || domain.equals("script")) {
            Button activateButton = new Button("Activate");
            activateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            activateButton.addClickListener(e -> controlDevice(entityId, domain, "turn_on"));
            
            buttons.add(activateButton);
        } else {
            // Default on/off buttons for lights, switches, fans, etc.
            Button onButton = new Button("Turn On");
            onButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            onButton.addClickListener(e -> controlDevice(entityId, domain, "turn_on"));

            Button offButton = new Button("Turn Off");
            offButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            offButton.addClickListener(e -> controlDevice(entityId, domain, "turn_off"));

            buttons.add(onButton, offButton);
        }

        card.add(info, buttons);
        deviceList.add(card);
    }

    private void controlDevice(String entityId, String domain, String service) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("domain", domain);
            args.put("service", service);
            args.put("entity_id", entityId);
            
            log.info("Calling MCP service: {}.{} on {}", domain, service, entityId);
            mcpClient.callTool("call_service", args);
            
            // Refresh device list after action
            loadDevices();
            
            addChatMessage("System", "Executed: " + service + " on " + entityId, false);
        } catch (Exception e) {
            log.error("Error controlling device via MCP", e);
            addChatMessage("System", "Error: " + e.getMessage(), false);
        }
    }
}
