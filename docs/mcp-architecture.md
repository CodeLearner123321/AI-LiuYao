# MCP Architecture

## Current Shape

AI-LiuYao 当前的 MCP 形态是挂在 Spring Boot Web 服务里的 HTTP JSON-RPC 接口，兼容 MCP 的 tools 协议：

- canonical endpoint: `POST /mcp`
- compatibility alias: `GET /mcp/tools`
- tool discovery: `tools/list`
- tool invocation: `tools/call`

默认支持的协议版本与当前实现一致，支持版本协商和 `MCP-Protocol-Version` 响应头。

## Request Flow

`Claude Desktop / Codex / 其他 MCP Client`
-> `POST /mcp`
-> `McpApiKeyFilter`
-> `initialize` / `tools/list` / `tools/call`
-> `ToolRegistry`
-> `RecognizeHexagramWithAnalysisImageTool`
-> `HexagramImageRecognitionService`
-> `AiAnalysisService`
-> `HexagramAnalysisPosterRenderService`
-> `OSSUtil`
-> MCP `result`

## Tool Contract

当前公开的工具是：

- `recognize_hexagram_with_analysis_image`
- `cast_today_fortune`

输入：

- `imageUrl: string`
- `question?: string`
- `background?: string`

今日运势工具输入：

- `castTime?: string`
- `question?: string`
- `background?: string`

输出：

1. `structuredContent`
   - `sourceImageUrl`
   - `imageUrl`
   - `hexagramText`
   - `analysisText`
2. `content`
   - 面向人类阅读的摘要文本
   - 以及结构化 JSON 文本，便于客户端继续编排

## Error Model

- unknown tool / invalid protocol params -> JSON-RPC error
- tool business failure -> `result.isError=true`

## Notes

- 当前实现已经覆盖图片识别、AI 断卦和结果图渲染。
- 如果后续要接桌面端 subprocess MCP Client，再补 `stdio` server 即可。
