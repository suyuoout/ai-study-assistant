# 智能学习助手（基于大语言模型与 RAG）

![CI](https://github.com/suyuoout/ai-study-assistant/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![Vue](https://img.shields.io/badge/Vue-3-42b883)
![MySQL](https://img.shields.io/badge/MySQL-8-4479a1)

> 部署到自有服务器或云托管平台的完整步骤见 [DEPLOY.md](DEPLOY.md)。

面向高校学生，提供课程资料管理、基于个人资料的智能问答（带来源引用）、
AI 学习计划生成、每日任务追踪与学习数据复盘。

## 技术栈

- 后端：Spring Boot 4.1.1、Java 25、MyBatis、MySQL 8 / H2、JWT、BCrypt、
  OkHttp + Fastjson2、PDFBox、Apache POI、Bean Validation、Actuator
- 前端：Vue 3、Vite、TypeScript、Vue Router、Axios、Element Plus、ECharts、
  markdown-it + highlight.js + DOMPurify
- AI：DeepSeek（OpenAI 风格接口，支持流式输出）
- 质量保障：JUnit 5 + MockMvc、ESLint、Prettier、Vitest、GitHub Actions

## 目录结构

```
study-assisiant/                后端工程（Spring Boot）
  src/main/java/com/study/assisiant/
    auth/      登录、JWT、注册登录 DTO、登录失败锁定
    user/      用户实体、UserVO、用户短缓存
    ai/        DeepSeek 调用、限流、用量统计、流式输出
    chat/      对话会话与消息
    document/  资料上传解析、片段检索、RAG 问答
    plan/      学习计划生成与管理
    task/      每日任务、统计、学习报告
    admin/     管理后台
    common/    统一响应、异常处理、加解密、SSE、异步线程池
    mapper/    MyBatis Mapper（集中放一处，便于查找）
study-assisiant-web/            前端工程（Vue 3 + Vite）
  src/components/               复用组件（学习趋势图等）
  src/utils/                    日期、Markdown 渲染等工具
  src/api/                      axios 封装与 SSE 流式客户端
sql/                            schema.sql 全新库 / migrate.sql 增量升级 / mysql-init.sql 容器初始化
```

## 快速开始

### 1. 后端（本地零依赖起步）

不设置任何环境变量时，后端使用内置 H2 文件库，开箱即用：

```powershell
cd study-assisiant
mvn spring-boot:run
```

需要 AI 功能时，提供 DeepSeek Key（或用个人中心里的「我的 AI Key」）：

```powershell
$env:DEEPSEEK_API_KEY="你的key"
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`，健康检查：`http://localhost:8080/actuator/health`。

### 2. 创建管理员账号

出于安全考虑，**默认不会自动创建任何账号**。需要管理员时显式开启：

```powershell
$env:BOOTSTRAP_ADMIN="true"        # 允许创建初始管理员
$env:ADMIN_PASSWORD="你自己设置的强密码"   # 不设置则随机生成并打印在启动日志里
mvn spring-boot:run
```

初始管理员首次登录后会被要求修改密码。

### 3. 前端

```powershell
cd study-assisiant-web
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发服务器已配置 `/api` 到 8080 的代理。

### 4. 使用 MySQL（可选）

```sql
CREATE DATABASE study_assistant DEFAULT CHARACTER SET utf8mb4;
-- 全新库执行 sql/schema.sql；已有库执行 sql/migrate.sql（可重复执行）
```

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 主要功能

**账号与权限**

- 注册、登录（JWT）、资料修改、修改密码
- 每用户可配置自己的 DeepSeek Key（**加密存储**，页面只显示掩码），
  使用系统 Key 时按天限额；账号可被管理员禁用
- 登录失败次数限制（默认 5 次失败锁定 10 分钟）

**AI 对话**

- 普通问答与「基于资料」问答（RAG，带来源编号与片段预览）
- **流式输出**（SSE，边生成边显示，可随时中断）
- Markdown 渲染 + 代码高亮（渲染结果经 DOMPurify 过滤）
- 会话重命名、删除、搜索、导出为 Markdown，历史消息分页加载

**知识库**

- 上传 PDF / TXT / DOCX（20MB 以内），**后台异步解析**，前端轮询状态
- 解析失败会记录并展示失败原因，可一键重新解析
- 资料详情页可查看切分后的文本片段，可指定只在部分资料中提问

**学习计划与任务**

- AI 生成结构化计划（JSON 模式输出），自动拆解为每日任务
- 计划重命名、状态流转（进行中 / 已完成 / 已归档）、删除
- 每日任务视图与本周视图、手动新增任务、编辑、删除、状态流转、
  实际用时记录、逾期待办清单

**统计与复盘**

- 学习看板：任务完成率、学习时长、16 周热力图、近 14 天趋势图（ECharts）
- **AI 学习复盘**：按区间聚合真实任务数据，由 AI 输出复盘与建议
- 管理后台：用户/资料/计划分页管理、用户禁用、AI 用量统计

## 接口一览

统一返回格式：`{ "code": 200, "message": "success", "data": {} }`，
错误响应同时使用真实 HTTP 状态码（401 / 403 / 404 / 429 / 500）。

| 模块 | 接口 |
|---|---|
| 账号 | `POST /api/users/register`、`POST /api/users/login`、`GET /api/users/me`、`PUT /api/users/me`、`PUT /api/users/me/password`、`PUT /api/users/me/ai-key`、`GET /api/users/me/ai-usage` |
| 对话 | `POST /api/ai/chat`、`POST /api/ai/chat/stream`(SSE)、`GET /api/chat/sessions`、`GET /api/chat/sessions/{id}/messages`、`PUT /api/chat/sessions/{id}`、`DELETE /api/chat/sessions/{id}` |
| 知识库 | `POST /api/documents`、`GET /api/documents`、`GET /api/documents/{id}`、`GET /api/documents/{id}/chunks`、`DELETE /api/documents/{id}`、`POST /api/documents/{id}/reprocess` |
| RAG | `POST /api/ai/rag/chat`、`POST /api/ai/rag/chat/stream`(SSE) |
| 计划 | `POST /api/plans/generate`、`GET /api/plans`、`GET /api/plans/{id}`、`GET /api/plans/{id}/tasks`、`PUT /api/plans/{id}`、`DELETE /api/plans/{id}` |
| 任务 | `GET /api/tasks`、`GET /api/tasks/upcoming`、`POST /api/tasks`、`PUT /api/tasks/{id}`、`PATCH /api/tasks/{id}/status`、`DELETE /api/tasks/{id}` |
| 统计 | `GET /api/statistics/overview`、`GET /api/statistics/daily`、`GET /api/statistics/report` |
| 管理 | `GET /api/admin/overview`、`GET /api/admin/ai-usage`、`GET /api/admin/users`、`PUT /api/admin/users/{id}/role`、`PUT /api/admin/users/{id}/enabled`、`GET /api/admin/documents`、`DELETE /api/admin/documents/{id}`、`GET /api/admin/plans`、`DELETE /api/admin/plans/{id}` |
| 运维 | `GET /actuator/health`、`GET /actuator/info` |

所有 `/api/**` 接口（除登录、注册）都需要 `Authorization: Bearer <token>`。

## 配置项

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DEEPSEEK_API_KEY` | 系统默认的 AI Key（用户没配自己的 Key 时使用） | 空 |
| `AI_MODEL` | 模型名 | `deepseek-chat` |
| `AI_TEMPERATURE` / `AI_MAX_TOKENS` | 生成参数 | `0.7` / `2048` |
| `AI_DAILY_LIMIT` | 每用户每天使用系统 Key 的次数（0 表示不限制） | `50` |
| `JWT_SECRET` / `JWT_EXPIRE_MINUTES` | 令牌密钥与有效期 | 开发默认值 / `120` |
| `CRYPTO_KEY` | 个人 AI Key 的加密密钥（建议显式设置） | 回退用 `JWT_SECRET` 派生并打印告警 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 连接（prod 环境必填） | — |
| `UPLOAD_DIR` | 上传目录 | `${user.home}/.study-assistant/uploads` |
| `ALLOW_REGISTER` | 是否开放注册 | `true`（生产建议 `false`） |
| `BOOTSTRAP_ADMIN` / `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 初始管理员创建 | `false` / `admin` / 空（随机生成） |
| `ASYNC_PARSE` | 文档解析是否异步执行 | `true` |
| `USER_CACHE_SECONDS` | 用户信息缓存秒数（角色/禁用状态的生效延迟上限） | `60` |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` | 登录失败上限与锁定时长 | `5` / `10` |
| `APP_ALLOWED_ORIGINS` | 允许跨域的来源（同源部署留空即可） | 空 |

## 测试与质量

```powershell
# 后端：单元测试 + 接口权限回归测试（使用内存 H2，不依赖外部数据库）
cd study-assisiant
mvn test

# 前端：Lint / 单元测试 / 类型检查 + 构建
cd study-assisiant-web
npm run lint
npm run test
npm run build
```

CI（`.github/workflows/ci.yml`）会在每次 push / PR 时跑上述全部检查。

## 安全说明

已实现的加固措施：

- 密码用 BCrypt 保存；登录失败次数限制；账号可被禁用
- 令牌只保存身份信息，**角色与启用状态每次请求从数据库读取**，
  因此管理员被降权后立即失效（缓存上限 60 秒）
- 用户自带 AI Key 使用 AES-256-GCM 加密存储，历史明文数据兼容读取
- 所有涉及用户数据的查询都带 `user_id` 条件，避免越权读写
- 上传文件按扩展名与大小校验，且删除/读取前会校验路径位于上传目录内
- 统一异常处理同时返回真实 HTTP 状态码并记录服务端日志，避免把 401/429 伪装成 200
- 前端渲染 AI 输出前关闭 HTML 直出并经过 DOMPurify 过滤

仍然建议在生产环境补充：

- 通过 HTTPS 反向代理访问，并显式设置 `JWT_SECRET`、`CRYPTO_KEY`
- 多实例部署时把登录限流与 AI 限额的计数换成 Redis
- 定期备份 MySQL 与上传目录

## 已知限制与后续方向

- RAG 目前是**关键词检索**（候选集受限的中文二字切分 + 打分），
  不是向量检索；知识库规模变大后建议引入 embedding + 向量索引
- 限流计数保存在数据库/内存中，单实例部署足够，多实例需外置存储
- 前端仍是 Element Plus 全量引入，产物偏大；可改为按需引入进一步瘦身
- 邮件/微信提醒、闪卡与间隔重复复习尚未实现，可作为下一步迭代方向
