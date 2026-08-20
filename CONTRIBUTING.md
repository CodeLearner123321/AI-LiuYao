# 贡献指南

感谢你参与 AI-LiuYao。提交代码、文档或素材前，请确认你拥有相应权利，并同意按 Apache License 2.0 提供贡献。

## 开发流程

1. Fork 并克隆仓库：

   ```bash
   git clone https://github.com/CodeLearner123321/AI-LiuYao.git
   cd AI-LiuYao
   ```

2. 从 `main` 创建分支：

   ```bash
   git switch -c feature/your-change
   ```

3. 按 README 配置本地依赖，完成修改并运行 `./mvnw test`（Windows 使用 `mvnw.cmd test`）。
4. 向 `main` 提交 Pull Request，说明动机、行为变化、验证方式和相关 Issue。

## 变更要求

- 不得提交 API Key、密码、Token、生产地址、个人数据或用户问卦内容。
- 修改数据库访问时同步更新 Entity、Mapper/XML 和 `sql`、`docker/mysql/init` 脚本。
- 修改 MCP 工具契约时同步更新 `docs/mcp-*.md` 和测试。
- 浏览器、MinIO、真实 LLM 等环境测试必须显式标记为集成测试。
- 不要直接修改压缩后的前端产物；应从对应前端源项目重新构建。
- 新增图片、字体、书籍或其他资源时，在 PR 中注明作者、来源和再分发许可证。

提交信息建议遵循 Conventional Commits，例如 `feat: add mcp tool`、`fix: validate history ownership`。

普通缺陷和功能建议可使用 GitHub Issues。安全问题请遵循 [SECURITY.md](SECURITY.md)，不要公开披露凭据或可利用细节。
