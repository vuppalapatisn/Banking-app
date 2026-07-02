# mcp-server

A Spring AI **Model Context Protocol (MCP)** server that exposes core-banking data to
LLM clients as callable *tools*. It runs over HTTP + Server-Sent Events (SSE) using the
`spring-ai-starter-mcp-server-webmvc` starter.

- Package: `com.banking.mcp`
- Port: `8087`
- MCP server name: `banking-mcp-server` (version `1.0.0`)

## Tools exposed

| Tool | Description |
|------|-------------|
| `getAccountBalance(accountNumber)` | Current balance for a 10-digit NUBAN account number. |
| `listRecentTransactions(accountNumber)` | Most recent transactions for an account. |
| `getProductCatalog()` | Bank product catalog with interest rates and fees. |

Each tool returns simulated in-memory data so the server runs with no external
dependencies. If the core-banking service is reachable at `CORE_BANKING_URI`
(default `http://localhost:8081`) the tool uses its response instead; any error
falls back to the simulated data.

Tools are declared with `@Tool` / `@ToolParam` on `BankingTools` and registered as a
`ToolCallbackProvider` bean built via `MethodToolCallbackProvider.builder().toolObjects(...).build()`.

## Running

```bash
mvn -B -ntp -pl mcp-server -am -DskipTests package
java -jar mcp-server/target/*.jar
# or
docker build -t banking-mcp-server -f mcp-server/Dockerfile .
docker run -p 8087:8087 banking-mcp-server
```

## Connecting an MCP client (e.g. Claude Desktop)

This server speaks MCP over WebMVC SSE. The transport exposes an SSE endpoint at
`http://localhost:8087/sse` (with messages POSTed back to the server-provided
message endpoint). Point any SSE-capable MCP client at that URL.

For clients that only launch local **stdio** MCP servers (such as the Claude Desktop
config file), bridge to this HTTP/SSE server with a proxy such as `mcp-remote`:

```jsonc
// claude_desktop_config.json
{
  "mcpServers": {
    "banking": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8087/sse"]
    }
  }
}
```

Once connected, the client can invoke `getAccountBalance`, `listRecentTransactions`,
and `getProductCatalog`.

## Actuator

`health`, `info`, and `metrics` are exposed under `/actuator`.
