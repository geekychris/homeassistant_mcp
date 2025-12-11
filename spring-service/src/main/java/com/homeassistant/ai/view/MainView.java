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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
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
        
        // Voice input button (Speech-to-Text)
        Button voiceButton = new Button("🎤");
        voiceButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        voiceButton.getElement().setAttribute("title", "Voice input (click and speak)");
        voiceButton.setId("voice-input-btn");
        
        // Speaker button (Text-to-Speech toggle)
        Button speakerButton = new Button("🔊");
        speakerButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        speakerButton.getElement().setAttribute("title", "Enable/disable voice responses");
        speakerButton.setId("speaker-toggle-btn");

        inputLayout.add(messageField, voiceButton, sendButton, speakerButton, clearButton);
        layout.add(inputLayout);
        
        // Add Web Speech API JavaScript
        initializeSpeechAPIs(messageField, sendButton, voiceButton, speakerButton);
        
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

    private void initializeSpeechAPIs(TextField messageField, Button sendButton, Button voiceButton, Button speakerButton) {
        // Set IDs first
        messageField.setId("chat-input-field");
        sendButton.setId("send-button");
        
        // Initialize TTS state
        getElement().executeJs("window.ttsEnabled = true;");
        
        // Speech Recognition (Speech-to-Text) - with better error handling
        voiceButton.getElement().executeJs(
            "setTimeout(() => {" +
            "  try {" +
            "    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;" +
            "    if (!SpeechRecognition) {" +
            "      console.error('Speech Recognition not supported in this browser');" +
            "      alert('Voice input is not supported in your browser. Please use Chrome, Edge, or Safari.');" +
            "      return;" +
            "    }" +
            "    const recognition = new SpeechRecognition();" +
            "    recognition.continuous = false;" +
            "    recognition.interimResults = false;" +
            "    recognition.lang = 'en-US';" +
            "    let isListening = false;" +
            "    " +
            "    this.addEventListener('click', () => {" +
            "      if (!isListening) {" +
            "        try {" +
            "          recognition.start();" +
            "          isListening = true;" +
            "          this.style.backgroundColor = '#ff4444';" +
            "          this.style.color = 'white';" +
            "          console.log('Speech recognition started');" +
            "        } catch(e) {" +
            "          console.error('Error starting recognition:', e);" +
            "          isListening = false;" +
            "        }" +
            "      }" +
            "    });" +
            "    " +
            "    recognition.onresult = (event) => {" +
            "      const transcript = event.results[0][0].transcript;" +
            "      console.log('Recognized:', transcript);" +
            "      const inputField = document.getElementById('chat-input-field');" +
            "      if (inputField) {" +
            "        inputField.value = transcript;" +
            "        const changeEvent = new Event('change', { bubbles: true });" +
            "        inputField.dispatchEvent(changeEvent);" +
            "        const sendBtn = document.getElementById('send-button');" +
            "        if (sendBtn) sendBtn.click();" +
            "      }" +
            "      isListening = false;" +
            "      this.style.backgroundColor = '';" +
            "      this.style.color = '';" +
            "    };" +
            "    " +
            "    recognition.onerror = (event) => {" +
            "      console.error('Speech recognition error:', event.error);" +
            "      isListening = false;" +
            "      this.style.backgroundColor = '';" +
            "      this.style.color = '';" +
            "    };" +
            "    " +
            "    recognition.onend = () => {" +
            "      console.log('Speech recognition ended');" +
            "      isListening = false;" +
            "      this.style.backgroundColor = '';" +
            "      this.style.color = '';" +
            "    };" +
            "  } catch(e) {" +
            "    console.error('Failed to initialize speech recognition:', e);" +
            "  }" +
            "}, 500);"
        );
        
        // Text-to-Speech toggle - clearer feedback
        speakerButton.getElement().executeJs(
            "setTimeout(() => {" +
            "  window.ttsEnabled = true;" +
            "  console.log('TTS initialized, enabled by default');" +
            "  " +
            "  this.addEventListener('click', () => {" +
            "    window.ttsEnabled = !window.ttsEnabled;" +
            "    console.log('TTS toggled:', window.ttsEnabled);" +
            "    " +
            "    if (window.ttsEnabled) {" +
            "      this.textContent = '🔊';" +
            "      this.style.backgroundColor = '';" +
            "      alert('Voice responses ENABLED - AI will speak responses');" +
            "    } else {" +
            "      this.textContent = '🔇';" +
            "      this.style.backgroundColor = '#ffcccc';" +
            "      window.speechSynthesis.cancel();" +
            "      alert('Voice responses DISABLED - AI will not speak');" +
            "    }" +
            "  });" +
            "}, 500);"
        );
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
        
        // Auto-scroll to bottom
        messageDiv.getElement().executeJs("setTimeout(() => this.scrollIntoView({behavior: 'smooth', block: 'end'}), 100);");
        
        // Text-to-Speech for Assistant responses
        if (!isUser && sender.equals("Assistant")) {
            messageDiv.getElement().executeJs(
                "console.log('TTS check - window.ttsEnabled:', window.ttsEnabled);" +
                "if (window.ttsEnabled) {" +
                "  console.log('Speaking Assistant response');" +
                "  const text = '" + message.replace("'", "\\'").replace("\n", " ") + "';" +
                "  const utterance = new SpeechSynthesisUtterance(text);" +
                "  utterance.rate = 1.0;" +
                "  utterance.pitch = 1.0;" +
                "  utterance.volume = 1.0;" +
                "  utterance.onstart = () => console.log('TTS started speaking');" +
                "  utterance.onend = () => console.log('TTS finished speaking');" +
                "  utterance.onerror = (e) => console.error('TTS error:', e);" +
                "  window.speechSynthesis.speak(utterance);" +
                "} else {" +
                "  console.log('TTS disabled, not speaking');" +
                "}"
            );
        }
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

                    // Show controllable entities AND sensors for monitoring
                    if (domain.equals("light") || domain.equals("switch") || 
                        domain.equals("fan") || domain.equals("cover") ||
                        domain.equals("media_player") || domain.equals("climate") ||
                        domain.equals("lock") || domain.equals("vacuum") ||
                        domain.equals("scene") || domain.equals("script") ||
                        domain.equals("input_boolean") || domain.equals("input_select") ||
                        domain.equals("automation") ||
                        domain.equals("sensor") || domain.equals("binary_sensor")) {
                        addDeviceCard(state);  // Pass entire state object with attributes
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

    private void addDeviceCard(JsonNode state) {
        String entityId = state.get("entity_id").asText();
        String stateValue = state.get("state").asText();
        String domain = entityId.split("\\.")[0];
        JsonNode attributes = state.has("attributes") ? state.get("attributes") : null;
        
        VerticalLayout card = new VerticalLayout();
        card.setWidth("100%");
        card.getStyle().set("padding", "10px");
        card.getStyle().set("border", "1px solid #ddd");
        card.getStyle().set("border-radius", "4px");
        card.getStyle().set("margin-bottom", "8px");
        card.setSpacing(true);
        card.setPadding(false);

        // Entity name and state
        Paragraph name = new Paragraph(entityId);
        name.getStyle().set("font-weight", "bold");
        name.getStyle().set("margin", "0 0 5px 0");

        Paragraph stateText = new Paragraph("State: " + stateValue);
        stateText.getStyle().set("margin", "0 0 10px 0");
        stateText.getStyle().set("color", "#666");
        stateText.getStyle().set("font-size", "0.9em");

        card.add(name, stateText);

        // Domain-specific controls
        if (domain.equals("sensor") || domain.equals("binary_sensor")) {
            // Sensors are read-only, show additional attributes if available
            if (attributes != null) {
                if (attributes.has("unit_of_measurement")) {
                    String unit = attributes.get("unit_of_measurement").asText();
                    Span unitSpan = new Span("Unit: " + unit);
                    unitSpan.getStyle().set("font-size", "0.85em");
                    unitSpan.getStyle().set("color", "#888");
                    card.add(unitSpan);
                }
                if (attributes.has("device_class")) {
                    String deviceClass = attributes.get("device_class").asText();
                    Span classSpan = new Span("Type: " + deviceClass);
                    classSpan.getStyle().set("font-size", "0.85em");
                    classSpan.getStyle().set("color", "#888");
                    card.add(classSpan);
                }
            }
        } else if (domain.equals("climate")) {
            addClimateControls(card, entityId, stateValue, attributes);
        } else if (domain.equals("light") && attributes != null && attributes.has("brightness")) {
            addDimmableLightControls(card, entityId, stateValue, attributes);
        } else if (domain.equals("media_player")) {
            addMediaPlayerControls(card, entityId, stateValue, attributes);
        } else if (domain.equals("lock")) {
            addLockControls(card, entityId, domain);
        } else if (domain.equals("scene") || domain.equals("script")) {
            addSceneControls(card, entityId, domain);
        } else {
            addOnOffControls(card, entityId, domain);
        }

        deviceList.add(card);
    }

    private void addClimateControls(VerticalLayout card, String entityId, String stateValue, JsonNode attributes) {
        if (attributes == null) return;
        
        // Show current temperature
        if (attributes.has("current_temperature")) {
            Span tempLabel = new Span("Current: " + attributes.get("current_temperature").asDouble() + "°F");
            tempLabel.getStyle().set("font-size", "0.9em");
            card.add(tempLabel);
        }
        
        // Temperature control
        NumberField tempField = new NumberField("Set Temperature");
        if (attributes.has("temperature")) {
            tempField.setValue(attributes.get("temperature").asDouble());
        }
        tempField.setMin(50.0);
        tempField.setMax(90.0);
        tempField.setStep(1.0);
        tempField.setWidth("150px");
        
        Button setTempButton = new Button("Set");
        setTempButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        setTempButton.addClickListener(e -> {
            setThermostatTemperature(entityId, tempField.getValue());
        });
        
        HorizontalLayout tempLayout = new HorizontalLayout(tempField, setTempButton);
        tempLayout.setAlignItems(Alignment.BASELINE);
        card.add(tempLayout);
    }
    
    private void addDimmableLightControls(VerticalLayout card, String entityId, String stateValue, JsonNode attributes) {
        // On/Off buttons
        HorizontalLayout onOffLayout = new HorizontalLayout();
        Button onButton = new Button("On");
        onButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        onButton.addClickListener(e -> controlDevice(entityId, "light", "turn_on"));
        
        Button offButton = new Button("Off");
        offButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        offButton.addClickListener(e -> controlDevice(entityId, "light", "turn_off"));
        
        onOffLayout.add(onButton, offButton);
        card.add(onOffLayout);
        
        // Brightness slider
        if (attributes.has("brightness")) {
            int currentBrightness = attributes.get("brightness").asInt();
            NumberField brightnessField = new NumberField("Brightness (0-255)");
            brightnessField.setValue((double) currentBrightness);
            brightnessField.setMin(0.0);
            brightnessField.setMax(255.0);
            brightnessField.setStep(10.0);
            brightnessField.setWidth("150px");
            
            Button setBrightButton = new Button("Set");
            setBrightButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            setBrightButton.addClickListener(e -> {
                setLightBrightness(entityId, brightnessField.getValue().intValue());
            });
            
            HorizontalLayout brightLayout = new HorizontalLayout(brightnessField, setBrightButton);
            brightLayout.setAlignItems(Alignment.BASELINE);
            card.add(brightLayout);
        }
    }
    
    private void addMediaPlayerControls(VerticalLayout card, String entityId, String stateValue, JsonNode attributes) {
        // Playback controls
        HorizontalLayout playbackLayout = new HorizontalLayout();
        Button playButton = new Button("Play");
        playButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        playButton.addClickListener(e -> controlDevice(entityId, "media_player", "media_play"));
        
        Button pauseButton = new Button("Pause");
        pauseButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        pauseButton.addClickListener(e -> controlDevice(entityId, "media_player", "media_pause"));
        
        Button stopButton = new Button("Stop");
        stopButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        stopButton.addClickListener(e -> controlDevice(entityId, "media_player", "media_stop"));
        
        playbackLayout.add(playButton, pauseButton, stopButton);
        card.add(playbackLayout);
        
        // Volume control
        if (attributes != null && attributes.has("volume_level")) {
            double currentVolume = attributes.get("volume_level").asDouble();
            NumberField volumeField = new NumberField("Volume (0-100)");
            volumeField.setValue(currentVolume * 100);
            volumeField.setMin(0.0);
            volumeField.setMax(100.0);
            volumeField.setStep(5.0);
            volumeField.setWidth("150px");
            
            Button setVolButton = new Button("Set");
            setVolButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            setVolButton.addClickListener(e -> {
                setMediaPlayerVolume(entityId, volumeField.getValue() / 100.0);
            });
            
            HorizontalLayout volLayout = new HorizontalLayout(volumeField, setVolButton);
            volLayout.setAlignItems(Alignment.BASELINE);
            card.add(volLayout);
        }
    }
    
    private void addLockControls(VerticalLayout card, String entityId, String domain) {
        HorizontalLayout buttons = new HorizontalLayout();
        Button lockButton = new Button("Lock");
        lockButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        lockButton.addClickListener(e -> controlDevice(entityId, domain, "lock"));
        
        Button unlockButton = new Button("Unlock");
        unlockButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        unlockButton.addClickListener(e -> controlDevice(entityId, domain, "unlock"));
        
        buttons.add(lockButton, unlockButton);
        card.add(buttons);
    }
    
    private void addSceneControls(VerticalLayout card, String entityId, String domain) {
        Button activateButton = new Button("Activate");
        activateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        activateButton.addClickListener(e -> controlDevice(entityId, domain, "turn_on"));
        card.add(activateButton);
    }
    
    private void addOnOffControls(VerticalLayout card, String entityId, String domain) {
        HorizontalLayout buttons = new HorizontalLayout();
        Button onButton = new Button("Turn On");
        onButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        onButton.addClickListener(e -> controlDevice(entityId, domain, "turn_on"));

        Button offButton = new Button("Turn Off");
        offButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        offButton.addClickListener(e -> controlDevice(entityId, domain, "turn_off"));

        buttons.add(onButton, offButton);
        card.add(buttons);
    }
    
    private void setThermostatTemperature(String entityId, Double temperature) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("domain", "climate");
            args.put("service", "set_temperature");
            args.put("entity_id", entityId);
            
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("temperature", temperature);
            args.put("data", serviceData);
            
            log.info("Setting thermostat {} to {} degrees", entityId, temperature);
            mcpClient.callTool("call_service", args);
            
            loadDevices();
            addChatMessage("System", String.format("Set %s to %.0f°F", entityId, temperature), false);
        } catch (Exception e) {
            log.error("Error setting thermostat", e);
            addChatMessage("System", "Error: " + e.getMessage(), false);
        }
    }
    
    private void setLightBrightness(String entityId, int brightness) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("domain", "light");
            args.put("service", "turn_on");
            args.put("entity_id", entityId);
            
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("brightness", brightness);
            args.put("data", serviceData);
            
            log.info("Setting light {} brightness to {}", entityId, brightness);
            mcpClient.callTool("call_service", args);
            
            loadDevices();
            addChatMessage("System", String.format("Set %s brightness to %d", entityId, brightness), false);
        } catch (Exception e) {
            log.error("Error setting light brightness", e);
            addChatMessage("System", "Error: " + e.getMessage(), false);
        }
    }
    
    private void setMediaPlayerVolume(String entityId, double volumeLevel) {
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("domain", "media_player");
            args.put("service", "volume_set");
            args.put("entity_id", entityId);
            
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("volume_level", volumeLevel);
            args.put("data", serviceData);
            
            log.info("Setting media player {} volume to {}", entityId, volumeLevel);
            mcpClient.callTool("call_service", args);
            
            loadDevices();
            addChatMessage("System", String.format("Set %s volume to %.0f%%", entityId, volumeLevel * 100), false);
        } catch (Exception e) {
            log.error("Error setting media player volume", e);
            addChatMessage("System", "Error: " + e.getMessage(), false);
        }
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
