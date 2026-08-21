# AI-LiuYao 项目导览（给后续 AI）

## 1. 项目定位
- 这是一个 `Spring Boot 2.7 + MyBatis-Plus + Redis + MySQL` 的后端项目，核心业务是六爻起卦与 AI 解卦。
- 前端静态资源已打包到 `src/main/resources/static`。
- 业务主线是“提交起卦请求 -> 创建异步任务 -> LLM 分析 -> 查询任务结果/历史”。

## 2. 快速目录地图
- 启动入口: `src/main/java/com/divination/liuyao/AiLiuyaoApplication.java`
- 控制器: `src/main/java/com/divination/liuyao/controller`
- 核心服务:
  - `TaskService`（任务创建/查询）
  - `AiAnalysisService`（异步 AI 分析、落库历史）
  - `HexagramService`（起卦与图片识别后转卦）
  - `PaymentService`（余额、免费额度、扣费）
  - `AuthService`（注册登录、验证码、改密）
- LLM 适配层:
  - 接口: `src/main/java/com/divination/liuyao/service/LLMService.java`
  - 工厂: `src/main/java/com/divination/liuyao/service/factory/LLMServiceFactory.java`
  - 实现: `.../impl/DashScopeLLMServiceImpl.java`、`.../impl/VolcengineLLMServiceImpl.java`
- 鉴权与限流:
  - JWT 拦截器: `src/main/java/com/divination/liuyao/interceptor/JwtInterceptor.java`
  - Web 配置: `src/main/java/com/divination/liuyao/config/WebConfig.java`
  - 限流切面: `src/main/java/com/divination/liuyao/aspect/RateLimitAspect.java`
- SQL: `sql/*.sql`
- Mapper XML: `src/main/resources/mapper/*.xml`
- Prompt 模板: `src/main/resources/templates/*.ftl`
- Redis Lua 脚本: `src/main/resources/lua/*.lua`

## 3. 核心请求链路
### 3.1 文本起卦
1. `POST /api/liuyao/cast` 接收 `CastDto`（问题、背景、起卦参数、可选模型参数）。
2. `TaskService.createLiuyaoTask`:
   - 用 Redis 锁限制同用户并发任务。
   - 处理收费逻辑（免费额度/余额/用户自带 API Key）。
   - 写入 `task` 表，状态 `PENDING`。
3. `AiAnalysisService.executeAiAnalysis`（`@Async`）:
   - 根据 `CastDto` 计算卦象。
   - 通过 `LLMServiceFactory` 调模型。
   - 解析 AI 文本为 `AiResult`，更新任务状态和金额，写历史 `ai_liuyao_history`。
4. 前端轮询 `GET /api/liuyao/task/{taskId}` 取结果。

### 3.2 图片起卦
1. `POST /api/liuyao/recognize` 上传图片。
2. `HexagramService.recognizeTextByImage`:
   - 上传 MinIO/S3 兼容对象存储。
   - 调用多模态模型抽取结构化卦象。
   - 转换成 `Hexagram` 返回，并按图片任务收费。

## 4. API 面概览
- 认证: `/api/auth/*`（注册、登录、登出、验证码、余额、改密）
- 六爻: `/api/liuyao/*`（起卦、查任务、算八字、生成卦象、识图、权限）
- 历史: `/api/liuyao/history/*`
- 卡密: `/api/cardkey/*`
- 文件: `/api/file/*`

注意:
- 除 `WebConfig` 白名单外，`/api/**` 统一走 JWT。
- 统一返回体是 `RespEntity`，字段为 `code/msg/data`。
- 多接口叠加 `@RateLimit`，底层由 Redis Lua 滑动窗口实现。

## 5. 数据模型（关键表）
- `user`: 用户、密码盐、余额、冻结余额。
- `task`: 异步任务请求参数、状态、结果、扣费信息。
- `ai_liuyao_history`: 历史问卦记录与反馈准确率。
- `card_key`: 充值卡密。
- `file_info`: 书籍/图片文件元数据。

## 6. 配置与启动
### 6.1 关键配置来源
- 示例配置: `src/main/resources/application-example.yml`
- 当前仓库的 `application.yml` 只保留了部分配置（端口、multipart、mybatis-plus）。

### 6.2 启动前至少需要补齐
- `spring.datasource.*`（MySQL）
- `spring.redis.*`
- `jwt.secret`、`jwt.token-expiration`
- `ai.*`（DashScope/Volcengine key 与默认服务）
- `default.apiKey`（系统兜底 key，`TaskService` 会读取）
- `spring.mail.*`、`email.*`（若使用邮箱验证码）

### 6.3 推荐本地步骤
1. 建库并执行 `sql/*.sql`。
2. 复制 `application-example.yml` 到本地环境配置并填入密钥。
3. 运行 `mvn spring-boot:run` 或 IDE 启动 `AiLiuyaoApplication`。

## 7. 需要重点注意的代码事实（后续 AI 常见坑）
- `WebConfig` 放行了 `/api/auth/refresh`，但当前代码没有对应接口实现。
- `AuthService.login` 保存 token 到 Redis 时使用 `DEFAULT_DEVICE_FINGERPRINT`，可能与 token 中真实设备指纹不一致。
- HTTP 与 LLM 工厂的默认服务统一为 `dashscope`。
- `payment_type` 与 `custom_time` 已同步到 Mapper、独立 SQL 和 Docker 初始化 SQL。
- 管理权限由 `app.admin-usernames` / `APP_ADMIN_USERNAMES` 统一配置。
- `/mcp` 使用独立的 `app.mcp.api-key` / `APP_MCP_API_KEY` 鉴权。

## 8. 如果你是来改代码（给后续 AI 的落地建议）
- 改接口行为: 从对应 `controller` 看入参，再跟到 `service` 主流程。
- 改鉴权/限流: 先看 `WebConfig`、`JwtInterceptor`、`RateLimitAspect`。
- 改模型接入: 先改 `LLMService` 接口，再补 `LLMServiceFactory` 分流与枚举。
- 改扣费: 关注 `PaymentService + UserMapper.xml + TaskMapper.xml` 的一致性。
- 改历史字段: 同步更新 `entity/dto/mapper xml/sql`，避免“代码与初始化 SQL 脱节”。
