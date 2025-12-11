# Project Architecture

## Overview

This project contains TWO separate applications with different purposes:

## 1. MCP Server (`mcp-server/`)

**Purpose**: Provide MCP (Model Context Protocol) interface for external AI assistants

**Communication**: stdin/stdout (NOT HTTP)  
**Port**: None - it's a stdio-based protocol server  
**Use Case**: Integration with Claude Desktop, MCP clients, AI assistants

**How it works**:
- Runs as a command-line application
- Communicates via JSON-RPC over stdin/stdout  
- Talks to Home Assistant REST API
- Provides MCP tools: `get_states`, `get_state`, `call_service`, etc.

**Starting the MCP server**:
```bash
cd mcp-server
java -jar target/homeassistant-mcp-server-1.0.0.jar

# Or use the script
./run.sh
```

**Integration example** (Claude Desktop config):
```json
{
  "mcpServers": {
    "homeassistant": {
      "command": "java",
      "args": ["-jar", "/path/to/homeassistant-mcp-server-1.0.0.jar"]
    }
  }
}
```

## 2. Spring AI Service (`spring-service/`)

**Purpose**: Web application with UI and REST API for home control

**Communication**: HTTP  
**Port**: 8080  
**Use Case**: Direct web access, Vaadin UI, REST API

**How it works**:
- Spring Boot web application
- Vaadin Flow UI for interactive control
- REST API at `/api/homeassistant/*`
- Spring AI + Ollama for chat
- Talks DIRECTLY to Home Assistant REST API (does NOT use MCP server)

**Starting the Spring AI service**:
```bash
cd spring-service
mvn spring-boot:run

# Or use the script
./run.sh

# Access at http://localhost:8080
```

## Key Differences

| Feature | MCP Server | Spring AI Service |
|---------|------------|-------------------|
| **Purpose** | MCP protocol for AI tools | Web UI + REST API |
| **Communication** | stdin/stdout | HTTP (port 8080) |
| **UI** | None | Vaadin Flow web UI |
| **HA Connection** | Direct REST | Direct REST |
| **AI Integration** | None (client provides AI) | Spring AI + Ollama |
| **Intended Users** | AI assistants (Claude, etc.) | Web browsers, HTTP clients |

## Architecture Diagram

```
┌─────────────────────┐
│  Claude Desktop /   │
│  MCP Client         │
└──────────┬──────────┘
           │ stdin/stdout (MCP protocol)
           │
┌──────────▼──────────┐         ┌──────────────────┐
│   MCP Server        │────────▶│  Home Assistant  │
│   (port: none)      │  REST   │  :8123           │
└─────────────────────┘         └────────▲─────────┘
                                         │
                                         │ REST
┌─────────────────────┐                  │
│  Web Browser        │                  │
└──────────┬──────────┘                  │
           │ HTTP :8080                  │
           │                             │
┌──────────▼──────────┐                  │
│ Spring AI Service   │──────────────────┘
│ (Vaadin UI + API)   │
│                     │
│ ┌─────────────────┐ │
│ │ Ollama (AI)     │ │
│ └─────────────────┘ │
└─────────────────────┘
```

## Data Flow

### MCP Server
1. MCP Client (e.g., Claude) → JSON-RPC request via stdin
2. MCP Server → Home Assistant REST API
3. Home Assistant → Response
4. MCP Server → JSON-RPC response via stdout
5. MCP Client receives result

### Spring AI Service
1. Web Browser → HTTP request (port 8080)
2. Spring Service → Home Assistant REST API
3. Home Assistant → Response
4. Spring Service → HTTP response
5. Browser displays result

**For AI Chat**:
1. User types message in UI
2. Spring AI Service → Ollama (local LLM)
3. Ollama generates response with context
4. May trigger Home Assistant actions
5. Response shown in UI

## Configuration

### MCP Server
File: `mcp-server/src/main/resources/application.properties`
```properties
homeassistant.base-url=http://homeassistant.local:8123
homeassistant.access-token=your-token

spring.main.web-application-type=none  # No HTTP server
spring.main.banner-mode=off
```

### Spring AI Service
File: `spring-service/src/main/resources/application.properties`
```properties
server.port=8080  # HTTP server port

homeassistant.base-url=http://homeassistant.local:8123
homeassistant.access-token=your-token

spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2
```

## Common Misconceptions

❌ **WRONG**: "The Spring AI service needs to start the MCP server"  
✅ **CORRECT**: They are independent applications

❌ **WRONG**: "The MCP server listens on an HTTP port"  
✅ **CORRECT**: MCP server uses stdin/stdout, not HTTP

❌ **WRONG**: "Both services need to run together"  
✅ **CORRECT**: Run them independently based on your needs

## Use Cases

**Use MCP Server when**:
- Integrating with Claude Desktop
- Building MCP-compatible tools
- Want AI assistant to control Home Assistant

**Use Spring AI Service when**:
- Need a web UI for home control
- Want REST API endpoints
- Want built-in AI chat with Ollama
- Need direct browser access

**Use Both when**:
- Want all features
- Multiple access methods needed
- They don't interfere with each other

## Testing

**MCP Server**:
```bash
cd mcp-server
./test.sh
```

**Spring AI Service**:
```bash
cd spring-service
mvn spring-boot:run
# Open http://localhost:8080
```

## Future: HTTP-Based MCP Server

If you want an HTTP-based MCP server (SSE streaming), that would be a third, separate application that:
- Listens on an HTTP port (e.g., 8081)
- Implements MCP over HTTP with Server-Sent Events (SSE)
- Still talks to Home Assistant REST API

This is NOT currently implemented, but could be added as `mcp-http-server/`.

---

**Key Takeaway**: These are TWO independent services. The MCP server is for AI assistants (stdio), and the Spring AI service is a web application (HTTP).
