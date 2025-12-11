# Conversation Memory

The Spring AI service now includes **conversation memory** that remembers your chat history across interactions.

## Features

### Session-Based Memory
- Each chat session maintains its own conversation history
- Messages are stored in memory and passed to the AI with each request
- The AI can reference previous messages in the conversation

### Automatic History Management
- Stores up to **50 messages** per session (25 user + 25 assistant pairs)
- Automatically trims older messages when the limit is reached
- Memory is maintained as long as the service is running

### UI Indicators
- Shows "Conversation memory enabled" message below the chat input
- Displays the number of messages stored: "Conversation memory: X messages stored"
- "Clear History" button to start a fresh conversation

## How It Works

### Architecture
```
User sends message
    ↓
AiChatService stores message in session history
    ↓
Sends entire conversation history to Ollama
    ↓
AI responds with context from previous messages
    ↓
Response is added to session history
```

### Example Conversation

**You:** "What lights are in the kitchen?"
**Assistant:** "I found 3 lights in the kitchen: ceiling light, under-cabinet light, and pendant light."

**You:** "Turn on the first one"
**Assistant:** *[Remembers "first one" refers to ceiling light from previous message]*
"I've turned on the kitchen ceiling light."

**You:** "And turn it off"
**Assistant:** *[Remembers we're talking about the kitchen ceiling light]*
"I've turned off the kitchen ceiling light."

## Implementation Details

### AiChatService.java
- Uses `ConcurrentHashMap<String, List<Message>>` to store histories
- Each session has a unique ID (default: "web-session")
- Messages are Spring AI `Message` objects (UserMessage, AssistantMessage)
- History is passed to the AI via `.messages(history)` in the prompt

### Session Management
```java
// Chat with memory
String response = aiChatService.chat(sessionId, message);

// Clear history for a session
aiChatService.clearHistory(sessionId);

// Get history size
int size = aiChatService.getHistorySize(sessionId);
```

## Testing the Memory

1. Start both services:
   ```bash
   # Terminal 1 - MCP Server
   cd mcp-server
   java -jar target/homeassistant-mcp-server-1.0.0.jar
   
   # Terminal 2 - Spring AI Service
   cd spring-service
   java -jar target/homeassistant-ai-service-1.0.0.jar
   ```

2. Open browser: http://localhost:8080

3. Try a conversation:
   - "What switches do you see?"
   - "Turn on the first one" (AI remembers the list)
   - "Now turn it off" (AI remembers which switch)

4. Click "Clear History" to start fresh

## Multi-User Support

The current implementation uses a single session ID ("web-session") for all users. To support multiple users:

```java
// In MainView.java, change:
private final String sessionId = "web-session";

// To something like:
private final String sessionId = VaadinSession.getCurrent().getSession().getId();
```

This would give each browser session its own conversation history.

## Memory Persistence

Currently, conversation history is stored **in-memory only** and is lost when the service restarts. To persist conversations:

1. **Option 1: Database** - Store messages in a database table
2. **Option 2: Redis** - Use Redis for session storage
3. **Option 3: File System** - Serialize histories to JSON files

## Configuration

Edit `AiChatService.java` to adjust memory settings:

```java
private static final int MAX_HISTORY_SIZE = 50; // Default: 50 messages
```

Larger values = more context but increased token usage and API costs.
