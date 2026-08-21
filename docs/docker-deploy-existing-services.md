# Docker 部署到已有 MySQL / Redis / MinIO

本方案只启动 AI-LiuYao 应用容器，复用服务器上已经存在的容器：

- MySQL: `mysql8`
- Redis: `qqbot-redis`
- MinIO: `minio`

应用容器对外使用 `8081:8080`，容器内自带 Chromium 用于 MCP 海报截图。

## 1. 上传项目

在服务器创建部署目录：

```bash
mkdir -p /home/linyouyun/ai-liuyao
```

把本项目上传到该目录，然后进入目录：

```bash
cd /home/linyouyun/ai-liuyao
```

## 2. 接入现有 Docker 网络

创建共享网络，并把已有容器接入：

```bash
docker network create ai-liuyao-net || true
docker network connect ai-liuyao-net mysql8 || true
docker network connect ai-liuyao-net qqbot-redis || true
docker network connect ai-liuyao-net minio || true
```

## 3. 初始化 MySQL

进入 MySQL：

```bash
docker exec -it mysql8 mysql -uroot -p
```

执行以下 SQL，把密码替换成自己的强密码：

```sql
CREATE DATABASE IF NOT EXISTS ai_liuyao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'ai_liuyao'@'%' IDENTIFIED BY 'replace_with_ai_liuyao_mysql_password';
GRANT ALL PRIVILEGES ON ai_liuyao.* TO 'ai_liuyao'@'%';
FLUSH PRIVILEGES;
```

导入表结构：

```bash
docker exec -i mysql8 mysql -uai_liuyao -preplace_with_ai_liuyao_mysql_password ai_liuyao < docker/mysql/init/001-init.sql
```

## 4. 配置环境变量

复制模板：

```bash
cp .env .env
```

编辑 `.env`：

```bash
nano .env
```

重点确认：

- `MYSQL_PASSWORD` 是刚创建的 MySQL 专用账号密码
- `MINIO_ENDPOINT` 是外部浏览器能打开的地址，例如 `http://服务器IP:9000`
- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` 是现有 MinIO 账号
- `JWT_SECRET` 至少 64 个随机字符
- `AI_DASHSCOPE_API_KEY` 填真实 DashScope Key

## 5. 启动应用

```bash
docker compose -f docker-compose.deploy.yml --env-file .env up -d --build
```

查看状态和日志：

```bash
docker compose -f docker-compose.deploy.yml ps
docker compose -f docker-compose.deploy.yml logs -f app
```

## 6. 验证RecognizeHexagramWithAnalysisImageTool.java

Web/API：

```bash
curl http://127.0.0.1:8081/mcp/tools
```

如果服务器防火墙放行了 8081，也可以访问：

```text
http://服务器IP:8081
```

MinIO 上传或 MCP 海报生成后，返回的预签名 URL 应该以 `.env` 中的 `MINIO_ENDPOINT` 开头，并且能在外部浏览器打开。

## 常用命令

重新构建并启动：

```bash
docker compose -f docker-compose.deploy.yml --env-file .env up -d --build
```

停止应用容器：

```bash
docker compose -f docker-compose.deploy.yml down
```

查看实时日志：

```bash
docker compose -f docker-compose.deploy.yml logs -f app
```
