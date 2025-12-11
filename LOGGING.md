# Enhanced Logging

The Spring AI service now includes comprehensive logging that tracks all chat queries and MCP service calls with parameters.

## Log Format

### 1. User Query Logging
When a user sends a message, you'll see:
```
═══════════════════════════════════════════════════════════════
📨 USER QUERY
Session: web-session
Message: Turn on the kitchen lights
History Size: 5 messages
═══════════════════════════════════════════════════════════════
```

### 2. Function Call Logging
When the AI calls a Home Assistant function:
```
🏠 Function: callService(domain='light', service='turn_on', entityId='light.kitchen')
   ↳ Executing: light.turn_on on light.kitchen
```

### 3. MCP Tool Call Logging
When the function communicates with the MCP server:
```
┌─────────────────────────────────────────────────────────────┐
│ 🔧 MCP TOOL CALL                                            │
├─────────────────────────────────────────────────────────────┤
│ Tool: call_service
│ Arguments: {"domain":"light","service":"turn_on","entity_id":"light.kitchen"}
└─────────────────────────────────────────────────────────────┘
```

### 4. MCP Response Logging
When the MCP server responds:
```
┌─────────────────────────────────────────────────────────────┐
│ ✅ MCP TOOL RESPONSE                                        │
├─────────────────────────────────────────────────────────────┤
│ Tool: call_service
│ Response Size: 234 bytes
└─────────────────────────────────────────────────────────────┘
```

### 5. Function Success Logging
When the function completes:
```
✓ callService() succeeded: light.turn_on on light.kitchen
```

### 6. AI Response Logging
When the AI generates a response:
```
───────────────────────────────────────────────────────────────
🤖 AI RESPONSE
Response: I've turned on the kitchen lights.
History Size: 6 messages
───────────────────────────────────────────────────────────────
```

## Complete Example Flow

Here's what a complete interaction looks like in the logs:

```
═══════════════════════════════════════════════════════════════
📨 USER QUERY
Session: web-session
Message: Turn on the kitchen lights
History Size: 5 messages
═══════════════════════════════════════════════════════════════

🏠 Function: callService(domain='light', service='turn_on', entityId='light.kitchen') - Controlling device
   ↳ Executing: light.turn_on on light.kitchen

┌─────────────────────────────────────────────────────────────┐
│ 🔧 MCP TOOL CALL                                            │
├─────────────────────────────────────────────────────────────┤
│ Tool: call_service
│ Arguments: {"domain":"light","service":"turn_on","entity_id":"light.kitchen"}
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ ✅ MCP TOOL RESPONSE                                        │
├─────────────────────────────────────────────────────────────┤
│ Tool: call_service
│ Response Size: 234 bytes
└─────────────────────────────────────────────────────────────┘

✓ callService() succeeded: light.turn_on on light.kitchen

───────────────────────────────────────────────────────────────
🤖 AI RESPONSE
Response: I've turned on the kitchen lights.
History Size: 6 messages
───────────────────────────────────────────────────────────────
```

## Viewing Logs

### Real-time Logs (Development Mode)
If running with `mvn spring-boot:run`, logs appear in the terminal:
```bash
cd spring-service
mvn spring-boot:run
```

### Log File
When running as JAR in background:
```bash
tail -f ai-service.log
```

### Filtering Specific Events
```bash
# Only user queries
tail -f ai-service.log | grep "USER QUERY"

# Only function calls
tail -f ai-service.log | grep "Function:"

# Only MCP calls
tail -f ai-service.log | grep "MCP TOOL"

# Only errors
tail -f ai-service.log | grep "ERROR\|✗"
```

## Log Levels

The service uses the following log levels:

- **INFO**: Normal operations (user queries, function calls, MCP calls)
- **DEBUG**: Detailed Spring AI debugging (set in application.properties)
- **WARN**: Non-fatal issues (entity not found)
- **ERROR**: Failures (connection errors, service call failures)

## Configuration

Edit `application.properties` to adjust logging:

```properties
# Current settings
logging.level.com.homeassistant=DEBUG
logging.level.org.springframework.ai=DEBUG

# To reduce verbosity
logging.level.com.homeassistant=INFO
logging.level.org.springframework.ai=INFO

# For even more detail
logging.level.com.homeassistant=TRACE
logging.level.org.springframework.ai=TRACE
```

## Icons Used

- 📨 User input
- 🤖 AI response
- 🏠 Home Assistant function
- 🔧 MCP tool call
- ✅ Successful response
- ✓ Success
- ✗ Failure/Error

## Troubleshooting

### Not Seeing Function Calls?
The AI might not be calling functions. Check:
1. Is the model good at function calling? (mistral-small recommended)
2. Are functions registered? Look for "ChatClient initialized with Home Assistant functions"
3. Try more explicit queries: "Use the callService function to turn on light.kitchen"

### MCP Connection Issues?
Look for:
```
Failed to connect to MCP server at http://localhost:8081/mcp
```
Make sure the MCP server is running on port 8081.
