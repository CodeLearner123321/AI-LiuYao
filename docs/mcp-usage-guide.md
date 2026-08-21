# AI-LiuYao MCP 使用教程

本文基于当前仓库里的真实实现编写。

## 1. 当前 MCP 形态

这个项目当前的 MCP 是挂在 Spring Boot Web 服务里的 HTTP JSON-RPC 接口：

- canonical 入口：`POST /mcp`
- 兼容别名：`GET /mcp/tools`
- 协议协商：`MCP-Protocol-Version` 请求/响应头
- 访问认证：`Authorization: Bearer <APP_MCP_API_KEY>` 或 `X-MCP-API-Key`
- 服务名：`ai-liuyao-mcp`

当前支持的协议版本：

- `2025-11-25`
- `2025-06-18`
- `2025-03-26`
- `2024-11-05`
- `2024-10-07`

它不是 `stdio` 进程；如果你的客户端只支持 subprocess/stdin/stdout 方式，需要额外适配层。

## 2. 当前支持的 MCP 工具

目前有 2 个工具：

- `recognize_hexagram_with_analysis_image`
- `cast_today_fortune`

作用：

1. 接收一张公网可访问的六爻图片 URL
2. 调用多模态模型识别图片中的卦例
3. 结合项目分析链路生成算卦结论
4. 渲染断卦结果图并上传

输入字段：

- `imageUrl`，必填
- `question`，可选
- `background`，可选

输出字段：

- `isError`
- `structuredContent.sourceImageUrl`
- `structuredContent.imageUrl`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `content`

### `cast_today_fortune`

作用：

1. 按当前时间或指定时间自动起六爻卦
2. 输出今日运势分析
3. 结构化返回总评、事业、财运、感情、健康与宜忌信息

输入字段：

- `castTime`，可选
- `question`，可选
- `background`，可选

输出字段：

- `isError`
- `structuredContent.castTime`
- `structuredContent.question`
- `structuredContent.background`
- `structuredContent.hexagramText`
- `structuredContent.analysisText`
- `structuredContent.keyOutcome`
- `content`

## 3. 使用前准备

### 3.1 基础环境

- JDK 11
- Maven 3.6+
- MySQL
- Redis

说明：

- 当前 MCP 走的是整个 Spring Boot 应用，不是最小化独立进程
- 因此启动 MCP 时，本项目的数据库、Redis、邮件等常规 Spring 配置也会一并参与装配

### 3.2 必填配置

复制一份配置文件：

```powershell
Copy-Item src\main\resources\application-example.yml src\main\resources\application.yml
```

至少要补这些配置：

- `spring.datasource.*`
- `spring.redis.*`
- `ai.default-llm-service`
- `ai.dashscope.api.key`
- `jwt.secret`
- `app.mcp.api-key`

建议直接把默认 LLM 保持为：

```yaml
ai:
  default-llm-service: dashscope
```

原因是当前图片识别固定走 DashScope 多模态能力。

### 3.3 MinIO 对象存储环境变量

如果你要开启图片识别、海报渲染上传，需要配置 MinIO/S3 兼容对象存储。仓库的 `docker-compose.yml` 已内置 MinIO，本地默认地址是 `http://localhost:9000`，控制台是 `http://localhost:9001`。

Windows PowerShell 示例：

```powershell
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_ACCESS_KEY="minioadmin"
$env:MINIO_SECRET_KEY="minioadmin"
$env:MINIO_BUCKET="ai-liuyao"
$env:MINIO_REGION="us-east-1"
$env:MINIO_PRESIGNED_URL_EXPIRY_SECONDS="3600"
```

说明：

- `OSSUtil` 保留旧类名以兼容现有调用点，底层已经改为 MinIO Java SDK
- 未配置环境变量时会使用本地 MinIO 默认值
- 如在 Docker Compose 内运行应用，`MINIO_ENDPOINT` 使用 `http://minio:9000`

### 3.4 浏览器依赖

如果 `renderImage=true`，服务会调用本机浏览器无头截图生成海报。

当前浏览器发现逻辑按优先级如下：

1. 环境变量 `MCP_BROWSER_PATH`
2. Windows 常见浏览器安装路径

Windows 裸跑时，当前版本会尝试这些常见路径：

- `C:\Program Files\Microsoft\Edge\Application\msedge.exe`
- `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`
- `C:\Program Files\Google\Chrome\Application\chrome.exe`
- `C:\Program Files (x86)\Google\Chrome\Application\chrome.exe`

如果浏览器不在默认位置，手工指定：

```powershell
$env:MCP_BROWSER_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"
```

如果你是在 Docker 容器里运行，建议显式把浏览器路径配置成容器内路径，例如：

```powershell
$env:MCP_BROWSER_PATH="/usr/bin/chromium"
```

## 4. 启动项目

在项目根目录执行：

```powershell
mvn spring-boot:run
```

默认端口是：

```text
http://localhost:8080
```

## 5. 先确认 MCP 接口是否正常

### 5.1 查看工具列表

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools" -Method Get `
  -Headers @{ "Authorization" = "Bearer $env:APP_MCP_API_KEY" }
```

预期会返回一个 `tools` 数组，里面至少包含：

```json
{
  "name": "recognize_hexagram_with_analysis_image",
  "description": "识别网络图片中的六爻卦例，直接生成算卦结论，并渲染带结论的结果图片。"
}
```

### 5.2 初始化 MCP 会话

```powershell
$body = @{
  jsonrpc = "2.0"
  id = 1
  method = "initialize"
  params = @{
    protocolVersion = "2025-11-25"
    capabilities = @{}
    clientInfo = @{
      name = "local-test-client"
      version = "1.0.0"
    }
  }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ "MCP-Protocol-Version" = "2025-11-25"; "Authorization" = "Bearer $env:APP_MCP_API_KEY" } `
  -Body $body
```

预期返回：

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

## 6. 调用工具

### 6.1 列出工具

```powershell
$body = @{
  jsonrpc = "2.0"
  id = 2
  method = "tools/list"
  params = @{}
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ "MCP-Protocol-Version" = "2025-11-25"; "Authorization" = "Bearer $env:APP_MCP_API_KEY" } `
  -Body $body
```

### 6.2 识别六爻图片并生成断卦结果图

```powershell
$body = @{
  jsonrpc = "2.0"
  id = 3
  method = "tools/call"
  params = @{
    name = "recognize_hexagram_with_analysis_image"
    arguments = @{
      imageUrl = "https://你的公网图片地址/example.png"
      question = "这次合作是否可行？"
      background = "准备与新客户签年度合同。"
    }
  }
} | ConvertTo-Json -Depth 20

Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ "MCP-Protocol-Version" = "2025-11-25"; "Authorization" = "Bearer $env:APP_MCP_API_KEY" } `
  -Body $body
```

### 6.3 测今日运势

```powershell
$body = @{
  jsonrpc = "2.0"
  id = 4
  method = "tools/call"
  params = @{
    name = "cast_today_fortune"
    arguments = @{
      castTime = "2026-05-28T10:30:00"
      question = "今天适合推进吗？"
      background = "准备推进一项合作。"
    }
  }
} | ConvertTo-Json -Depth 20

Invoke-RestMethod -Uri "http://localhost:8080/mcp" `
  -Method Post `
  -ContentType "application/json" `
  -Headers @{ "MCP-Protocol-Version" = "2025-11-25"; "Authorization" = "Bearer $env:APP_MCP_API_KEY" } `
  -Body $body
```

## 7. 返回结果怎么读

成功响应的主体在：

- `result.isError`
- `result.structuredContent`
- `result.content`

示例：

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "isError": false,
    "structuredContent": {
      "sourceImageUrl": "https://example.com/hexagram.png",
      "imageUrl": "https://minio.example.com/ai-liuyao/poster.png",
      "hexagramText": "问题: ...\n六爻: ...",
      "analysisText": "..."
    },
    "content": [
      {
        "type": "text",
        "text": "断卦结果图已生成，图片地址: https://minio.example.com/ai-liuyao/poster.png"
      },
      {
        "type": "text",
        "text": "{\"sourceImageUrl\":\"...\",\"imageUrl\":\"...\",\"hexagramText\":\"...\",\"analysisText\":\"...\"}"
      }
    ]
  }
}
```

可以这样理解：

- `structuredContent`：给程序继续处理
- `content`：给人读，也给老客户端兜底
- `structuredContent.imageUrl`：断卦结果图地址

`cast_today_fortune` 的结构化输出还会额外包含：

- `structuredContent.castTime`
- `structuredContent.question`
- `structuredContent.background`
- `structuredContent.keyOutcome`

## 8. curl 示例

如果你更习惯 `curl`：

```bash
curl -X POST "http://localhost:8080/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $APP_MCP_API_KEY" \
  -H "MCP-Protocol-Version: 2025-11-25" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "recognize_hexagram_with_analysis_image",
      "arguments": {
        "imageUrl": "https://你的公网图片地址/example.png"
      }
    }
  }'
```

## 9. 常见报错

### 9.1 `imageUrl is required`

原因：

- 没传 `imageUrl`
- 传了空字符串

### 9.2 `imageUrl must be an http/https URL`

原因：

- 传了本地路径
- 传了 `file://`
- 传了不合法 URL

正确做法：

- 只传公网可访问的 `http/https` 图片地址

### 9.3 `未找到可用浏览器，请配置 MCP_BROWSER_PATH 或安装 Edge/Chrome`

原因：

- 本机或容器里找不到 Chromium / Chrome / Edge

处理方式：

```powershell
$env:MCP_BROWSER_PATH="你的浏览器完整路径"
```

### 9.4 图片识别失败

常见原因：

- `ai.dashscope.api.key` 未配置或错误
- 图片地址外网不可访问
- DashScope 模型调用失败
- 模型返回的 JSON 不符合项目要求

建议排查顺序：

1. 先确认图片 URL 在浏览器里能直接打开
2. 检查 `ai.dashscope.api.key`
3. 查看 Spring Boot 日志

### 9.5 海报上传失败

常见原因：

- MinIO 未启动或 `MINIO_ENDPOINT` 无法访问
- MinIO access key / secret key 无权限
- 上传成功但预签名地址过期

注意：

- 当前返回的是对象存储预签名地址，不是永久直链
- 默认过期时间为 3600 秒，可通过 `MINIO_PRESIGNED_URL_EXPIRY_SECONDS` 调整

### 9.6 协议版本不支持

常见原因：

- `MCP-Protocol-Version` 头和 `params.protocolVersion` 不一致
- 传了当前实现不支持的协议版本

建议：

- 优先使用 `2025-11-25`
- 头和参数值保持一致

## 10. 如何接入 MCP Client

### 10.1 可以直接接入的情况

如果你的客户端支持“HTTP JSON-RPC 自定义 MCP 端点”，那么直接指向：

```text
http://localhost:8080/mcp
```

即可。

### 10.2 不能直接接入的情况

很多桌面 MCP Client 只支持：

- `stdio`
- Streamable HTTP
- 官方 MCP Server 进程拉起方式

如果客户端不支持直连 HTTP JSON-RPC，可以在外面套一层轻量代理，或者后续补 `stdio` server。

## 11. 推荐联调顺序

建议按这个顺序来：

1. 启动 Spring Boot
2. `GET /mcp/tools` 确认工具存在
3. `initialize`
4. `tools/list`
5. `tools/call`

## 12. 当前实现和架构文档的关系

`docs/mcp-architecture.md` 里写的是当前实现的架构说明，不是旧版 `stdio` 草案。

## 13. 一句话总结

当前项目的 MCP 使用方式可以概括为：

1. 启动 Spring Boot
2. 调 `http://localhost:8080/mcp`
3. 先 `initialize`
4. 再用 `tools/list` / `tools/call` 调 `recognize_hexagram_with_analysis_image`
