# AI-LiuYao 项目索引

## 项目概览
- Java 11 / Spring Boot 2.7 Maven 后端项目，入口为 `src/main/java/com/divination/liuyao/AiLiuyaoApplication.java`。
- 业务目标：AI 赋能六爻预测，支持起卦、AI 解卦、异步任务、历史记录、卡密、文件/MinIO、MCP 工具。
- 当前分支主要涉及 MCP：识别卦象图片、生成断卦结果图、海报 HTML/渲染、浏览器截图链路。

## 主要目录
- `src/main/java/com/divination/liuyao/controller`：HTTP API 控制器，含认证、起卦、历史、卡密、文件接口。
- `src/main/java/com/divination/liuyao/service`：业务服务与实现，含用户、AI 分析、历史、任务、卡密、文件、支付等。
- `src/main/java/com/divination/liuyao/mapper`：MyBatis-Plus Mapper 接口，对应 XML 在 `src/main/resources/mapper`。
- `src/main/java/com/divination/liuyao/pojo`：DTO、VO、Entity、Enum、Model。
- `src/main/java/com/divination/liuyao/util`：AI、Redis、JWT、对象存储、六爻/八字、FreeMarker、JSON 等工具。
- `src/main/java/com/divination/liuyao/mcp`：MCP controller、tool registry/schema/handler、图片识别与海报渲染服务。
- `src/main/resources/templates`：AI prompt 与海报 FreeMarker 模板。
- `src/main/resources/lua`：Redis 限流脚本。
- `sql`：初始化表结构脚本。
- `docs`：MCP 架构、调用和使用文档。

## 构建与运行
- 构建/测试：`mvn test`
- 启动：`mvn spring-boot:run`
- 打包：`mvn package`
- 配置文件：`src/main/resources/application.yml`、`application-dev.yml`、`application-prod.yml`、`application-example.yml`。
- 本地运行通常需要 MySQL、Redis、LLM API Key、MinIO/邮件等外部配置；不要把真实密钥写入仓库。

## 开发注意事项
- 修改数据库访问时同步检查 Mapper 接口、XML、Entity/DTO/VO 与 `sql/*.sql`。
- 修改认证、限流或用户权限时重点检查 `JwtInterceptor`、`RateLimitAspect`、`RedisUtil`、`UserContextHolder`。
- 修改 AI 分析链路时重点检查 `AIUtil`、LLM service、prompt 模板与异步任务/历史保存。
- 修改 MCP 工具时重点检查 `mcp/tool/impl`、`ToolRegistry`、`ToolSchemaGenerator`、`McpController`，以及 `docs/mcp-*.md`。
- 修改海报/截图链路时重点检查 `Hexagram*Poster*Service`、`BrowserScreenshotService`、`templates/*poster.ftl` 与相关测试。
- UI 静态产物在 `src/main/resources/static`；不要手改压缩后的构建产物，除非用户明确要求。

## 测试关注点
- 现有测试包括对象存储工具与 MCP 海报渲染/图片识别相关测试，位于 `src/test/java`。
- 涉及外部服务的测试可能需要测试资源配置或跳过真实凭据；优先使用 `src/test/resources/application.properties` 中的测试配置。
- 改动后优先运行针对性测试，再视改动范围运行 `mvn test`。
