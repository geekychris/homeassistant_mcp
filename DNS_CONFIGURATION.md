# DNS Configuration Issue & Resolution

## Problem

Both services were configured to use `http://homeassistant.local:8123`, but this causes DNS resolution failures with the error:

```
Failed to resolve 'homeassistant.local' [A(1), AAAA(28)] 
DnsErrorCauseException: Query failed with NXDOMAIN
```

## Root Cause

**OkHttp** (used by MCP server) and **Reactor Netty** (used by Spring AI service's WebClient) both have issues resolving mDNS `.local` domains, even though:
- Browsers can access `http://homeassistant.local:8123` fine
- Standard tools like `ping` and `curl` work fine
- macOS Bonjour/mDNS works fine

The Java HTTP clients don't use the system's mDNS resolver and instead rely on standard DNS, which doesn't know about `.local` domains.

## Solution

Use the IP address directly instead of the hostname.

### Finding Your Home Assistant IP

```bash
# Option 1: Ping
ping -c 1 homeassistant.local | grep "PING" | awk '{print $3}' | tr -d '()'

# Option 2: DNS lookup
dns-sd -G v4 homeassistant.local

# Option 3: Check in Home Assistant UI
# Settings -> System -> Network
```

### Current Configuration

Both services are now configured with:
```properties
homeassistant.base-url=http://192.168.1.82:8123
```

**Files updated:**
- `mcp-server/src/main/resources/application.properties`
- `spring-service/src/main/resources/application.properties`

## If Your IP Address Changes

Home Assistant typically uses DHCP, so the IP might change after a router reboot. If the services stop working:

### Option 1: Update to New IP

1. Find the new IP:
   ```bash
   ping -c 1 homeassistant.local
   ```

2. Update both configuration files:
   ```bash
   # MCP Server
   vi mcp-server/src/main/resources/application.properties
   
   # Spring AI Service  
   vi spring-service/src/main/resources/application.properties
   ```

3. Rebuild:
   ```bash
   cd mcp-server && mvn clean package
   cd ../spring-service && mvn clean package
   ```

### Option 2: Set Static IP (Recommended)

Configure a static IP for Home Assistant:

1. In Home Assistant UI:
   - Settings -> System -> Network
   - Configure static IP (e.g., 192.168.1.82)

2. Or in your router:
   - Set DHCP reservation for Home Assistant's MAC address
   - Always assigns the same IP

### Option 3: Custom DNS Entry

Add a DNS entry in your router or `/etc/hosts`:

```bash
# Add to /etc/hosts
sudo nano /etc/hosts

# Add line:
192.168.1.82    homeassistant.local
```

Then you can use `homeassistant.local` in configuration.

**Note**: `/etc/hosts` solution works for OkHttp but may not work for Reactor Netty due to its custom DNS resolver.

## Architecture Note

Both services connect DIRECTLY to Home Assistant:

```
┌─────────────┐
│ MCP Server  │────┐
└─────────────┘    │
                   ├─→ Home Assistant (192.168.1.82:8123)
┌─────────────┐    │
│ Spring AI   │────┘
│ Service     │
└─────────────┘
```

They do NOT connect to each other - they're independent services that both happen to use Home Assistant.

## Testing Connection

### MCP Server
```bash
cd mcp-server
./test.sh
```

Should show successful tool calls to Home Assistant.

### Spring AI Service
```bash
cd spring-service
mvn spring-boot:run
# Open http://localhost:8080
# Should load device list without errors
```

## Alternative: Use Hostname (Advanced)

If you must use hostnames, configure custom DNS resolver:

### For OkHttp (MCP Server)
Use system DNS resolver:
```java
// Add dependency: com.squareup.okhttp3:okhttp-dnsoverhttps
OkHttpClient client = new OkHttpClient.Builder()
    .dns(dns -> InetAddress.getAllByName(hostname))
    .build();
```

### For Reactor Netty (Spring Service)
Configure JVM DNS cache:
```bash
java -Dsun.net.inetaddr.ttl=60 \
     -Dsun.net.inetaddr.negative.ttl=10 \
     -jar spring-service.jar
```

**Not recommended** - just use IP address instead!

---

**Current Status**: ✅ Both services configured with IP address `192.168.1.82`
