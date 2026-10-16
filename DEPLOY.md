# 部署说明

本项目是「前端 + 后端」同源的单体应用：前端构建产物打进后端，
后端对外只暴露一个 8080 端口，数据用 MySQL。

## 一、环境变量

生产环境（`SPRING_PROFILES_ACTIVE=prod`）下这几个变量必须提供：

| 变量 | 说明 | 示例 |
|---|---|---|
| `DEEPSEEK_API_KEY` | 系统默认的大模型 API Key | `sk-xxxx` |
| `JWT_SECRET` | 登录令牌签名密钥，随便一长串随机字符 | `Zx9...` |
| `CRYPTO_KEY` | 个人 AI Key 的加密密钥（与 JWT_SECRET 不同，且勿泄露） | `Qw8...` |
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://127.0.0.1:3306/study_assistant?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `root` / `******` |
| `ALLOW_REGISTER` | 是否开放注册（生产建议 `false`） | `false` |
| `UPLOAD_DIR` | 上传文件目录（建议挂载数据卷） | `/data/uploads` |
| `AI_DAILY_LIMIT` | 每用户每天使用「系统默认 Key」调用 AI 的次数（`0` 表示不限制） | `50` |

可选变量：

| 变量 | 说明 | 默认值 |
|---|---|---|
| `AI_MODEL` | 模型名 | `deepseek-chat` |
| `AI_TEMPERATURE` / `AI_MAX_TOKENS` | 生成参数 | `0.7` / `2048` |
| `AI_CONNECT_TIMEOUT` / `AI_READ_TIMEOUT` | 调用大模型的连接/读取超时（秒） | `30` / `120` |
| `JWT_EXPIRE_MINUTES` | 令牌有效期（分钟） | `120` |
| `BOOTSTRAP_ADMIN` | 是否创建初始管理员 | `false` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 初始管理员账号；不填密码则随机生成并打印一次 | `admin` / 空 |
| `ASYNC_PARSE` | 文档解析是否异步执行 | `true` |
| `USER_CACHE_SECONDS` | 用户信息缓存秒数（权限变更生效延迟上限） | `60` |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` | 登录失败上限与锁定时长 | `5` / `10` |
| `APP_ALLOWED_ORIGINS` | 需要跨域时填写来源白名单（逗号分隔） | 空 |

> 每个用户也可以在自己的「个人中心 → 我的 AI Key」里填上自己的 DeepSeek Key，
> 填了之后 AI 调用走他自己的额度，不占用上面的每日次数，Key 会加密后入库。

> 本地开发和离线运行不需要这些：不设置 `SPRING_PROFILES_ACTIVE` 时默认用内置 H2 文件库。

## 二、首次部署要做的两件事

1. **建库**：全新库执行 `sql/mysql-init.sql` 或 `sql/schema.sql`；
   已有库执行 `sql/migrate.sql`（可重复执行，幂等）。
2. **创建管理员**：设置 `BOOTSTRAP_ADMIN=true`（可同时指定 `ADMIN_PASSWORD`）
   启动一次即可，登录后请立即修改密码；之后建议把 `BOOTSTRAP_ADMIN` 关掉。

健康检查地址：`GET /actuator/health`，返回 `{"status":"UP"}` 表示服务正常。

## 三、方案 A：云端容器托管（推荐，最省事）

适用于微信云托管 / 腾讯云 CloudBase 这类「从 Git 仓库构建容器」的平台。

1. 仓库根目录已有 `Dockerfile`，平台会自动用它做多阶段构建
   （Node 构建前端 → Maven 打包后端 → JRE 运行）。
2. 在平台新建部署时：
   - **Git 仓库**：选择本仓库，分支 `main`
   - **服务端口填 8080**（访问端口一般填 80）——不要填 80，程序监听的是 8080
   - **环境变量**：填入上表变量，并把 `SPRING_PROFILES_ACTIVE` 设为 `prod`
3. 数据库用云数据库 MySQL，提前执行一次 `sql/mysql-init.sql` 建表。
4. 点部署，完成后平台会给一个公网访问地址。

## 四、方案 B：自有服务器（Docker Compose 一键起）

适用于你有一台云服务器（Ubuntu）或自己的机器。

```bash
# 1. 安装 Docker 后，在项目根目录
cp .env.example .env
# 编辑 .env，填好 DB_PASSWORD / JWT_SECRET / CRYPTO_KEY / DEEPSEEK_API_KEY

# 2. 启动（含 MySQL，首次会自动执行 sql/mysql-init.sql 建表）
docker compose up -d --build

# 3. 访问 http://服务器IP:8080
```

配域名 + HTTPS（用 Caddy 最省心，自动申请证书）：

```
# /etc/caddy/Caddyfile
your-domain.com {
    reverse_proxy 127.0.0.1:8080
}
```

## 五、不用 Docker 的方式

本机构建好 jar 后，在服务器上用 systemd 常驻运行：

```bash
# 本机构建（先构建前端并拷进后端 static）
cd study-assisiant-web && npm ci && npm run build
cp -r dist/* ../study-assisiant/src/main/resources/static/
cd .. && mvn -f study-assisiant/pom.xml clean package -DskipTests

# 上传 target/study-assisiant-0.0.1-SNAPSHOT.jar 到服务器后
java -jar study-assisiant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 六、数据库

- 生产使用 MySQL 8，建表脚本：`sql/mysql-init.sql`
- 数据库名建议 `study_assistant`
- 备份：`mysqldump -uroot -p study_assistant > backup_$(date +%F).sql`
- 注意：本地/离线模式用的是内置 H2 文件库，两边的数据互不相通。

## 七、升级已有部署

1. 拉取新代码并重新构建镜像（或重新打包 jar）
2. 对已有数据库执行一次 `sql/migrate.sql`（幂等，可重复执行）
3. 新增的环境变量按上表补齐，特别是 `CRYPTO_KEY`
   ——它用于加密个人 AI Key，**更换会导致已保存的 Key 无法解密**（用户重新填写即可）
4. 重启服务，访问 `/actuator/health` 确认状态

## 八、更新流程

1. 本地改完代码，跑一遍 `mvn test` 与 `npm run lint && npm run test`
2. `git add . && git commit -m "..." && git push`
3. 平台勾了自动部署的话会自动重新构建发布；否则手动点一次部署
4. 服务器方式：`git pull && docker compose up -d --build`

## 九、常见问题

- **构建报找不到镜像 `maven:3.9-eclipse-temurin-25`**：Java 25 较新，把 Dockerfile
  里的镜像改成 `maven:3.9-eclipse-temurin-21`，同时把 `study-assisiant/pom.xml`
  里的 `<java.version>` 改成 `21`。
- **部署成功但访问 502/超时**：多半是服务端口填错了，必须是 **8080**。
- **AI 用不了**：检查 `DEEPSEEK_API_KEY` 是否设置、服务器能否访问外网；
  接口会返回上游的具体错误信息（例如 `AI 服务返回错误（401）`）。
- **上传资料一直显示「解析中」**：查看后端日志中 `doc-parse-` 线程的输出，
  解析失败的原因也会写在资料列表的悬停提示里。
- **登录提示「登录失败次数过多」**：默认 5 次失败锁定 10 分钟，
  可通过 `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` 调整。
- **重新部署后数据没了**：容器是无状态的，说明你用的是内置 H2；改用 MySQL 即可持久化。
