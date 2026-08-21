---
name: ai-liuyao
description: Use this skill when working in the AI-LiuYao Java 11 Spring Boot project, especially for quickly understanding the repository, changing LiuYao casting, AI analysis, async tasks, history, card keys, auth/rate limiting, file upload/object storage, MinIO/OSS compatibility, MCP tools, image recognition, or poster rendering.
---

# AI-LiuYao Project Skill

## First Pass

1. Read `AGENTS.md` for the latest project index and collaboration notes.
2. Run `rg --files` or focused `rg` searches before opening large files; avoid reading built assets under `src/main/resources/static` unless the user asks.
3. Identify the flow by starting at `controller`, following into `service`, then `mapper`, `pojo`, templates, and SQL.
4. Treat `AI_PROJECT_GUIDE.md` as the longer project map when you need deeper onboarding context.

## Core Shape

- Java 11 / Spring Boot 2.7 app entry: `src/main/java/com/divination/liuyao/AiLiuyaoApplication.java`.
- Main business: 六爻起卦, AI 解卦, async task execution, history, payments/card keys, file upload, MCP image recognition and poster rendering.
- HTTP controllers live in `src/main/java/com/divination/liuyao/controller`.
- Business logic lives in `src/main/java/com/divination/liuyao/service` and `service/impl`.
- MyBatis-Plus mappers live in `src/main/java/com/divination/liuyao/mapper`; XML lives in `src/main/resources/mapper`.
- DTO/VO/entity/model/enum classes live in `src/main/java/com/divination/liuyao/pojo`.
- Prompts and poster templates live in `src/main/resources/templates`.
- MCP code lives in `src/main/java/com/divination/liuyao/mcp`; docs live in `docs/mcp-*.md`.

## Key Flows

### Text LiuYao Analysis

1. `LiuyaoController` receives cast/task requests.
2. `TaskService` creates async tasks, handles Redis lock/payment checks, and persists task state.
3. `AiAnalysisService` performs `@Async` model analysis and saves history.
4. `LLMServiceFactory` chooses `DashScopeLLMServiceImpl` or `VolcengineLLMServiceImpl`.
5. Prompt rendering uses FreeMarker templates in `src/main/resources/templates`.

### Image Recognition

1. `HexagramService.recognizeTextByImage` uploads the image through `OSSUtil`.
2. DashScope visual model extracts a structured `Prediction`.
3. `HexagramService.calculateLiuYaoByImage` converts the recognized gua into a `Hexagram`.
4. Payment is confirmed through `PaymentService`.

### MCP And Posters

- Inspect `McpController`, `ToolRegistry`, `ToolSchemaGenerator`, and `mcp/tool/impl` for tool contracts.
- For rendering, inspect `Hexagram*PosterHtmlService`, `Hexagram*PosterRenderService`, `PosterBackgroundService`, `BrowserScreenshotService`, and `templates/*poster.ftl`.
- Update `docs/mcp-*.md` whenever tool inputs, outputs, storage URLs, or render behavior change.

## Object Storage

- `OSSUtil` is a compatibility name; the current implementation uses MinIO Java SDK / S3-compatible object storage.
- Main env vars: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MINIO_REGION`, `MINIO_PRESIGNED_URL_EXPIRY_SECONDS`.
- Docker Compose includes a `minio` service. In containers use `MINIO_ENDPOINT=http://minio:9000`; on host use `http://localhost:9000`.
- Existing file upload, image recognition, and MCP poster upload still call `OSSUtil.uploadFile(...)` and `OSSUtil.getFileUrl(...)`.

## Change Checklists

### Persistence

- Keep Mapper interface, Mapper XML, Entity/DTO/VO, and `sql/*.sql` synchronized.
- Check both `mybatis-plus` config and old `mybatis` config before assuming mapper behavior.

### Auth, Rate Limit, User Context

- Inspect `WebConfig`, `JwtInterceptor`, `RateLimitAspect`, `RedisUtil`, and `UserContextHolder`.
- Note that admin/root checks are not centralized; confirm the current code path before changing permissions.

### AI/LLM

- Inspect `LLMService`, provider implementations, `LLMServiceFactory`, prompt templates, `AiResult`, task persistence, and payment handling.
- Be careful with default provider values; different classes may specify different fallbacks.

### File Upload And MinIO

- Validate `FileServiceImpl`, `HexagramService`, and MCP render services together.
- Do not require real cloud OSS credentials for local development; use the MinIO defaults or Docker Compose service.
- For integration testing against MinIO, enable the guarded test with `-Dminio.integration.enabled=true`.

## Commands

- Targeted test: `mvn -Dtest=ClassName test`
- MinIO integration test: `mvn -Dtest=OSSUtilTest -Dminio.integration.enabled=true test`
- All tests: `mvn test`
- Run app: `mvn spring-boot:run`
- Package: `mvn package`
- Local dependencies: `docker compose up -d mysql redis minio`

## Safety Notes

- Never commit real DB, Redis, JWT, email, LLM, OSS, or MinIO production credentials.
- Do not hand-edit compressed static build output in `src/main/resources/static` unless explicitly requested.
- Expect MySQL, Redis, LLM APIs, MinIO, mail, and browser screenshot code to need environment-specific configuration.

## Completion Checklist

- Run the narrowest relevant test first.
- If API or MCP behavior changed, update matching docs/examples.
- If object storage changed, verify both URL generation and at least one upload path when MinIO is available.
- If poster layout changed, run the render tests or explain why they could not run.
