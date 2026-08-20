# AI-LiuYao

AI-LiuYao 是一个基于 Java 11 和 Spring Boot 2.7 的六爻起卦与 AI 分析服务。项目包含用户认证、异步分析任务、历史记录、卡密、图片识卦、MinIO 对象存储，以及可供 AI 客户端调用的 MCP 工具。

> 本项目用于传统文化研究与软件技术交流，输出内容不构成医疗、法律、投资或其他专业建议。

## 功能

- 时间、随机和手动起卦
- DashScope 与火山引擎 LLM 适配
- 异步分析任务、余额/免费额度和历史记录
- 图片卦象识别与结果海报渲染
- MCP JSON-RPC 接口与今日运势工具
- JWT 认证、Redis 限流、MySQL 持久化和 MinIO 文件存储
- Docker Compose 本地运行

## 技术要求

- JDK 11、Maven 3.6+
- MySQL 8.0、Redis 6+
- MinIO 或其他 S3 兼容对象存储
- Chromium/Chrome/Edge（仅海报渲染需要）
- 至少一个可用的 LLM API Key

## Docker 快速启动

```bash
cp .env.example .env
# 编辑 .env，替换所有 replace/change_me 占位值，并配置至少一个 LLM API Key
docker compose up --build
```

默认地址：Web/API `http://localhost:8080`，MinIO API `http://localhost:9000`，MinIO Console `http://localhost:9001`。

首次创建 MySQL 数据卷时，`docker/mysql/init/001-init.sql` 会初始化表结构。修改初始化 SQL 后如需重新初始化，请先备份数据再重建数据卷。

## 本地开发

```bash
docker compose up -d mysql redis minio
cp src/main/resources/application-example.yml src/main/resources/application-dev.yml
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

填写 `application-dev.yml` 后再启动。不要提交 `.env`、环境配置或任何真实凭据。

## 关键配置

| 环境变量 | 用途 | 是否必需 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 签名密钥，至少 64 个随机字符 | 是 |
| `AI_DASHSCOPE_API_KEY` | 阿里百练 API Key | 二选一 |
| `AI_VOLCENGINE_API_KEY` | 火山引擎 API Key | 二选一 |
| `DEFAULT_API_KEY` | 系统支付链路使用的默认模型 Key | 使用系统额度时 |
| `APP_MCP_API_KEY` | MCP 接口访问密钥 | 是 |
| `APP_ADMIN_USERNAMES` | 逗号分隔的管理员用户名 | 管理功能需要 |
| `APP_CORS_ALLOWED_ORIGINS` | 允许携带凭证访问的前端 Origin | 跨域前端需要 |
| `MINIO_ENDPOINT` | S3 兼容对象存储地址 | 图片功能需要 |
| `MCP_BROWSER_PATH` | Chromium/Chrome/Edge 可执行文件 | 海报功能需要 |

完整示例见 [.env.example](.env.example) 和 [application-example.yml](src/main/resources/application-example.yml)。

## MCP

MCP 入口为 `POST /mcp`，工具列表可通过 `GET /mcp/tools` 查询。所有请求必须携带独立密钥：

```http
Authorization: Bearer <APP_MCP_API_KEY>
```

也支持 `X-MCP-API-Key` 请求头。更多说明：

- [MCP 使用教程](docs/mcp-usage-guide.md)
- [MCP 调用方指南](docs/mcp-caller-guide.md)
- [MCP 外部接入](docs/mcp-external-integration.md)
- [MCP 架构](docs/mcp-architecture.md)

## 测试

```bash
# 默认单元测试，不依赖浏览器或外部 MinIO
./mvnw test

# Chromium 海报集成测试
./mvnw -Pintegration-tests -Dtest='Hexagram*PosterRenderServiceTest' test

# MinIO 集成测试
./mvnw -Dtest=OSSUtilTest -Dminio.integration.enabled=true test
```

## 安全与贡献

- 安全问题请按 [SECURITY.md](SECURITY.md) 私下报告。
- 贡献流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。
- 项目采用 [Apache License 2.0](LICENSE)。贡献者必须有权按该许可证提供相关代码和素材。
