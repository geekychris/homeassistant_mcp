# Quick Start Guide

Get up and running with Home Assistant MCP Server and AI Control Service in minutes!

## Prerequisites Check

Before starting, ensure you have:

1. **Java 21**: 
   ```bash
   java -version
   # Should show Java 21 or higher
   ```

2. **Maven**:
   ```bash
   mvn -version
   ```

3. **Home Assistant** running at `http://homeassistant.local:8123`

4. **Ollama** (for Spring service only):
   ```bash
   # Install on macOS
   brew install ollama
   
   # Start Ollama
   ollama serve
   
   # In another terminal, pull a model
   ollama pull llama3.2
   ```

## Option 1: Spring Boot Service (Recommended for Quick Demo)

The Spring Boot service provides a complete web UI with AI chat.

### Start the Service

```bash
cd spring-service
./run.sh
```

Or manually:
```bash
cd spring-service
mvn spring-boot:run
```

### Access the UI

Open your browser to: **http://localhost:8080**

You'll see:
- **Left side**: AI chat interface
- **Right side**: Device list with control buttons

### Try It Out

1. Click on device buttons to turn them on/off
2. Chat with the AI:
   - "What lights are on?"
   - "Turn off the bedroom lights"
   - "Show me all switches"

### Use the REST API

```bash
# Get all device states
curl http://localhost:8080/api/homeassistant/states

# Turn on a light
curl -X POST http://localhost:8080/api/homeassistant/services/light/turn_on \
  -H "Content-Type: application/json" \
  -d '{"entity_id": "light.living_room"}'

# Chat with AI
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What devices are currently on?"}'
```

## Option 2: MCP Server

The MCP server provides a protocol-compliant interface for AI assistants.

### Build and Run

```bash
cd mcp-server
./run.sh
```

Or manually:
```bash
cd mcp-server
mvn clean package
java -jar target/homeassistant-mcp-server-1.0.0.jar
```

### Test the MCP Server

The MCP server uses stdin/stdout for communication. Send JSON-RPC messages:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05"}}' | java -jar target/homeassistant-mcp-server-1.0.0.jar
```

### Integration with Claude Desktop

Add to your Claude Desktop config (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "homeassistant": {
      "command": "java",
      "args": ["-jar", "/path/to/homeassistant-mcp-server-1.0.0.jar"],
      "env": {
        "HA_BASE_URL": "http://homeassistant.local:8123",
        "HA_ACCESS_TOKEN": "your-token-here"
      }
    }
  }
}
```

## Configuration

### Change Home Assistant URL/Token

**Spring Service**: Edit `spring-service/src/main/resources/application.properties`

**MCP Server**: Set environment variables:
```bash
export HA_BASE_URL="http://your-homeassistant:8123"
export HA_ACCESS_TOKEN="your-token-here"
```

### Change Ollama Model

Edit `spring-service/src/main/resources/application.properties`:
```properties
spring.ai.ollama.chat.options.model=llama3.2
# Or use: mistral, codellama, etc.
```

## Troubleshooting

### "Cannot connect to Home Assistant"

Test connection manually:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://homeassistant.local:8123/api/states
```

If this fails:
- Check Home Assistant is running
- Verify the URL is correct
- Check your access token in Home Assistant settings

### "Cannot connect to Ollama"

```bash
# Check if Ollama is running
ps aux | grep ollama

# If not, start it
ollama serve

# Verify model is installed
ollama list

# Pull model if missing
ollama pull llama3.2
```

### "Port 8080 already in use"

Change the port in `application.properties`:
```properties
server.port=8088
```

Or run with:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8088
```

### Maven build fails

Clear Maven cache:
```bash
mvn clean
rm -rf ~/.m2/repository/org/springframework/ai
mvn clean install
```

## Next Steps

- Read the main [README.md](README.md) for detailed documentation
- Explore the REST API at http://localhost:8080/api/
- Customize the AI chat prompts in `AiChatService.java`
- Add more device types to the UI in `MainView.java`
- Integrate the MCP server with your favorite AI tools

## Getting Help

Check the individual README files:
- [MCP Server README](mcp-server/README.md)
- [Spring Service README](spring-service/README.md)

Common issues and solutions are documented in the main README.
