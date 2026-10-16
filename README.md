# 智能学习助手（基于大语言模型与 RAG）

![CI](https://github.com/suyuoout/ai-study-assistant/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![Vue](https://img.shields.io/badge/Vue-3-42b883)
![MySQL](https://img.shields.io/badge/MySQL-8-4479a1)

> 部署到自有服务器或云托管平台的完整步骤见 [DEPLOY.md](DEPLOY.md)。

面向高校学生，提供课程资料管理、基于个人资料的智能问答、AI 学习计划生成与学习进度追踪。

## 技术栈

- 后端：Spring Boot 4.1.1、Java 25、MyBatis、MySQL 8、JWT、BCrypt、OkHttp + Fastjson2、PDFBox、Apache POI
- 前端：Vue 3、Vite、TypeScript、Vue Router、Axios、ECharts
- AI：DeepSeek（OpenAI 风格接口）

## 目录结构

- `study-assisiant/`：后端工程
- `study-assisiant-web/`：前端工程
- `sql/`：`schema.sql`（全新建库）、`migrate.sql`（现有库增量升级）

## 快速开始

1. 准备 MySQL，创建数据库并初始化表：

```sql
CREATE DATABASE study_assistant DEFAULT CHARACTER SET utf8mb4;
-- 然后执行 sql/migrate.sql，或对全新库执行 sql/schema.sql
```

2. 配置密钥。后端敏感配置通过环境变量或 `application-local.yml` 提供：

```powershell
# 方式一：使用本地 profile（application-local.yml 已 gitignore）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 方式二：手动设置环境变量后启动
$env:DEEPSEEK_API_KEY="你的key"
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="自定义随机字符串"
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

3. 启动前端：

```powershell
cd study-assisiant-web
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，登录后即可使用。

## 主要功能

- 用户：注册、登录（JWT）、个人资料修改
- AI 对话：普通问答、多轮会话、历史消息
- 知识库：上传 PDF/TXT/DOCX、解析、切分、基于资料的 RAG 问答（带来源引用）
- 学习计划：AI 生成结构化计划并自动拆解为每日任务
- 任务与统计：每日任务、状态流转、完成率、学习时长、ECharts 看板

## 主要接口

- 用户：`POST /api/users/register`、`POST /api/users/login`、`GET/PUT /api/users/me`
- 对话：`POST /api/ai/chat`、`GET /api/chat/sessions`、`GET /api/chat/sessions/{id}/messages`
- 知识库：`POST/GET /api/documents`、`GET/DELETE /api/documents/{id}`、`POST /api/documents/{id}/reprocess`
- RAG：`POST /api/ai/rag/chat`
- 计划：`POST /api/plans/generate`、`GET /api/plans`、`GET /api/plans/{id}`、`GET /api/plans/{id}/tasks`
- 任务：`GET /api/tasks?date=`、`PUT /api/tasks/{id}`、`PATCH /api/tasks/{id}/status`
- 统计：`GET /api/statistics/overview`、`GET /api/statistics/daily`

统一返回格式：`{ "code": 200, "message": "success", "data": {} }`。

## 安全说明

- 密码使用 BCrypt 加密存储，登录签发 JWT，接口按用户归属做权限校验。
- DeepSeek API Key、数据库密码、JWT 密钥均通过环境变量或 gitignore 的本地配置注入，不要提交到仓库。
