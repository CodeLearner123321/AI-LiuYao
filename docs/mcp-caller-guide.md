# AI-LiuYao MCP 调用方接入文档

本文面向 MCP 调用方，基于当前代码编写。

## 1. 服务形态

当前 MCP 通过 HTTP + JSON-RPC 2.0 提供服务，不是 `stdio` 进程：

- RPC 入口：`POST /mcp`
- 工具发现：`GET /mcp/tools`（兼容别名）或 RPC `tools/list`
- 协议版本：通过 `MCP-Protocol-Version` 头协商
- 认证：`Authorization: Bearer <APP_MCP_API_KEY>` 或 `X-MCP-API-Key`

当前支持的协议版本：

- `2025-11-25`
- `2025-06-18`
- `2025-03-26`
- `2024-11-05`
- `2024-10-07`

## 2. 基础约定

调用 `POST /mcp` 时建议携带：

- Header：`Content-Type: application/json`
- Header：`MCP-Protocol-Version`
- Header：`Authorization: Bearer <APP_MCP_API_KEY>`
- Body：JSON（不能为空）

推荐请求骨架：

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2025-11-25",
    "capabilities": {},
    "clientInfo": {
      "name": "local-test-client",
      "version": "1.0.0"
    }
  }
}
```

## 3. 可用 RPC 方法

- `initialize`
- `ping`
- `tools/list`
- `tools/call`
- `notifications/initialized`

## 4. 工具发现

### 4.1 tools/list

请求：

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

当前工具：

- `recognize_hexagram_with_analysis_image`
- `cast_today_fortune`

输入：

- `imageUrl`，必填
- `question`，可选
- `background`，可选

输出：

- `isError`
- `structuredContent.sourceImageUrl`
- `structuredContent.imageUrl`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `content`

### 4.2 cast_today_fortune

请求：

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "cast_today_fortune",
    "arguments": {
      "castTime": "2026-05-28T10:30:00",
      "question": "今天适合推进吗？",
      "background": "准备推进一项合作。"
    }
  }
}
```

输入：

- `castTime`，可选，支持 `2026-05-28T10:30:00` / `2026-05-28 10:30:00` / `2026-05-28 10:30`
- `question`，可选
- `background`，可选

输出：

- `isError`
- `structuredContent.castTime`
- `structuredContent.question`
- `structuredContent.background`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `structuredContent.keyOutcome`
- `content`

### 4.3 兼容别名

也可以直接请求：

- `GET /mcp/tools`

返回结构等价于 `tools/list` 的 `result` 部分。

## 5. 调用工具

### 5.1 请求示例

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "recognize_hexagram_with_analysis_image",
    "arguments": {
      "imageUrl": "https://example.com/hexagram.png",
      "question": "这次合作是否可行？",
      "background": "准备与新客户签年度合同。"
    }
  }
}
```

### 5.2 识别图片成功响应示例

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "isError": false,
    "structuredContent": {
      "sourceImageUrl": "https://example.com/hexagram.png",
      "imageUrl": "https://xxx/mcp/renderedHexagramAnalysis/xxx.png?...",
      "hexagramText": "问题: ...\n六爻: ...",
      "analysisText": "..."
    },
    "content": [
      {
        "type": "text",
        "text": "断卦结果图已生成，图片地址: https://xxx/mcp/renderedHexagramAnalysis/xxx.png?..."
      },
      {
        "type": "text",
        "text": "{\"sourceImageUrl\":\"...\",\"imageUrl\":\"...\",\"hexagramText\":\"...\",\"analysisText\":\"...\"}"
      }
    ]
  }
}
```

### 5.3 今日运势成功响应示例

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "isError": false,
    "structuredContent": {
      "castTime": "2026-05-28T10:30:00",
      "question": "今天适合推进吗？",
      "background": "准备推进一项合作。",
      "hexagramText": "问题: ...\n六爻: ...",
      "analysisText": "...",
      "keyOutcome": "稳中有进"
    },
    "content": [
      {
        "type": "text",
        "text": "今日运势已生成\n起卦时间: 2026-05-28T10:30:00\n判辞: 稳中有进"
      },
      {
        "type": "text",
        "text": "{\"castTime\":\"2026-05-28T10:30:00\",\"question\":\"今天适合推进吗？\",\"background\":\"准备推进一项合作。\",\"hexagramText\":\"...\",\"analysisText\":\"...\",\"keyOutcome\":\"稳中有进\"}"
      }
    ]
  }
}
```

### 5.4 字段语义

- `structuredContent.sourceImageUrl`
  - 输入的原始图片 URL
- `structuredContent.imageUrl`
  - 最终结果图 URL
- `structuredContent.hexagramText`
  - 识别后的卦象文本
- `structuredContent.analysisText`
  - AI 断卦分析文本
- `structuredContent.castTime`
  - 今日运势实际使用的起卦时间
- `structuredContent.question`
  - 今日运势问题
- `structuredContent.background`
  - 今日运势背景
- `structuredContent.keyOutcome`
  - AI 提取出的判辞
- `content`
  - 第一段是给人看的摘要
  - 第二段是结构化 JSON 文本

## 6. 错误模型

- 协议层错误：返回 JSON-RPC `error`
  - 例如 unknown tool、缺少 tool name、协议版本不支持
- 工具业务错误：返回 `result.isError=true`
  - 例如图片下载失败、识别失败、渲染失败

## 7. 常见错误与处理

### 7.1 请求体缺失

错误特征：`Required request body is missing`

处理：

- 确认使用 `POST /mcp`
- 确认 `Content-Type: application/json`
- 确认 body 非空

### 7.2 图片下载失败

错误特征：`InvalidParameter: Failed to download multimodal content`

处理：

- 确认 `imageUrl` 为公网可访问 URL
- 确认签名 URL 未过期

### 7.3 结果图生成失败

处理：

- 确保部署机有可用浏览器并可无头启动

## 8. 最佳实践

- 首次接入先调用 `tools/list`，按 `inputSchema/outputSchema` 自动生成客户端参数与解析逻辑
- 对 `tools/call` 建议设置超时和仅对可重试错误的重试策略
- 对 URL 类入参建议在客户端预检可达性与有效期
