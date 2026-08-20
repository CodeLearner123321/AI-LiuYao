# Security Policy

安全修复目前仅面向 `main` 分支的最新版本。

## 报告安全问题

请使用 GitHub 仓库的 Private vulnerability reporting 功能。若尚未启用，请私下联系仓库维护者，并只提供复现所需的最少信息。

请勿在公开 Issue、日志或截图中包含 API Key、JWT、邮箱验证码、用户问题、数据库内容或对象存储签名 URL。维护者确认并修复问题前，请不要公开利用细节。

## 部署责任

公开部署前必须替换所有示例密钥、配置可信 CORS Origin 和独立 MCP API Key、限制 MySQL/Redis/MinIO 的公网访问、启用 HTTPS，并定期轮换 LLM、JWT、邮件和对象存储凭据。
