# AI-LiuYao MCP External Integration Guide

This document is for external MCP clients and integrators.

## 1. Service Overview

AI-LiuYao exposes MCP over HTTP JSON-RPC 2.0.

- Canonical endpoint: `POST /mcp`
- Compatibility alias: `GET /mcp/tools`
- Protocol header: `MCP-Protocol-Version`
- Authentication: `Authorization: Bearer <APP_MCP_API_KEY>` or `X-MCP-API-Key`
- Server name: `ai-liuyao-mcp`

Supported protocol versions:

- `2025-11-25`
- `2025-06-18`
- `2025-03-26`
- `2024-11-05`
- `2024-10-07`

## 2. Request Flow

1. Send `initialize`
2. Call `tools/list`
3. Call a tool with `tools/call`

Use `Content-Type: application/json`, include `MCP-Protocol-Version`, and authenticate every request with the configured MCP API key.

## 3. Initialize

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-11-25",
    "capabilities": {},
    "clientInfo": {
      "name": "your-client",
      "version": "1.0.0"
    }
  }
}
```

Typical response:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2025-11-25",
    "capabilities": {
      "tools": {}
    },
    "serverInfo": {
      "name": "ai-liuyao-mcp",
      "version": "0.2.0"
    }
  }
}
```

## 4. Available Tools

### 4.1 `recognize_hexagram_with_analysis_image`

Purpose:

- Recognize a six-yao chart from an image URL
- Generate an analysis result
- Optionally render a result image

Input:

- `imageUrl` required
- `question` optional
- `background` optional

Output:

- `isError`
- `structuredContent.sourceImageUrl`
- `structuredContent.imageUrl`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `content`

Example call:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "recognize_hexagram_with_analysis_image",
    "arguments": {
      "imageUrl": "https://example.com/hexagram.png",
      "question": "Is this deal feasible?",
      "background": "Preparing for a yearly contract."
    }
  }
}
```

### 4.2 `cast_today_fortune`

Purpose:

- Create a six-yao cast for today or a specified time
- Return a daily fortune reading

Input:

- `castTime` optional
  - supported formats: `2026-05-28T10:30:00`, `2026-05-28 10:30:00`, `2026-05-28 10:30`
- `question` optional
- `background` optional

Output:

- `isError`
- `structuredContent.castTime`
- `structuredContent.question`
- `structuredContent.background`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `structuredContent.keyOutcome`
- `content`

Example call:

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "cast_today_fortune",
    "arguments": {
      "castTime": "2026-05-28T10:30:00",
      "question": "Should I push this forward today?",
      "background": "Preparing to advance a cooperation."
    }
  }
}
```

## 5. `tools/list`

Request:

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/list",
  "params": {}
}
```

Use this response to build client-side schemas dynamically.

## 6. Response Model

### Protocol errors

Returned as JSON-RPC `error`:

- unsupported protocol version
- invalid request
- unknown method
- invalid tool name or invalid tool params

### Tool execution errors

Returned as a normal JSON-RPC `result` with:

- `isError: true`
- human-readable text in `content`

## 7. cURL Examples

### List tools

```bash
curl -X POST "http://host:port/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_MCP_API_KEY" \
  -H "MCP-Protocol-Version: 2025-11-25" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

### Call daily fortune

```bash
curl -X POST "http://host:port/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_MCP_API_KEY" \
  -H "MCP-Protocol-Version: 2025-11-25" \
  -d '{
    "jsonrpc":"2.0",
    "id":2,
    "method":"tools/call",
    "params":{
      "name":"cast_today_fortune",
      "arguments":{
        "question":"Should I push this forward today?"
      }
    }
  }'
```

## 8. Practical Notes

- Use public `http/https` URLs for image inputs.
- Keep `MCP-Protocol-Version` consistent between header and request params if you set both.
- If your client only supports `stdio` or Streamable HTTP, use an adapter/proxy layer.
