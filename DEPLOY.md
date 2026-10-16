# 部署说明

本项目是「前端 + 后端」同源的单体应用：前端构建产物打进后端，
后端对外只暴露一个 8080 端口。

有两种运行姿态，先选一种再往下看：

| 姿态 | 数据存放 | 适用场景 | 需要的变量 |
|---|---|---|---|
| **A. 生产模式**（推荐） | MySQL 8 | 正式上线、要保数据 | `SPRING_PROFILES_ACTIVE=prod` + 数据库 + 密钥 |
| **B. 演示模式** | 容器内 H2 文件 | 只想先看效果、答辩演示 | 只设 `JWT_SECRET`，**不要**设 prod |

> 演示模式下容器是无状态的，重新部署（换版本）数据会丢，别用来存真实数据。

## 一、环境变量

### 必需（两种姿态都要）

| 变量 | 说明 | 示例 |
|---|---|---|
| `JWT_SECRET` | 登录令牌签名密钥，**至少 32 位随机串，不要用可猜的短语** | `Zx9...` |

### 生产模式（A）额外必需

| 变量 | 说明 | 示例 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | 固定填 `prod` | `prod` |
| `DB_URL` | MySQL 连接串 | `jdbc:mysql://内网地址:3306/study_assistant?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 | `root` / `******` |

> 缺 `JWT_SECRET` 或 `DB_PASSWORD` 时，服务启动会直接失败，
> 并在日志里给出「生产环境缺少必需的环境变量：…」的明确提示。

### 建议设置

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DEEPSEEK_API_KEY` | 系统默认的大模型 Key。**可以不设**，此时用户需在「个人中心」填自己的 Key | 空 |
| `CRYPTO_KEY` | 个人 AI Key 的加密密钥。不设时回退用 `JWT_SECRET` 派生（会打印告警） | 空 |
| `UPLOAD_DIR` | 上传目录；容器无状态，重启会丢文件 | `${user.home}/.study-assistant/uploads` |
| `ALLOW_REGISTER` | 是否开放注册，生产建议 `false` | `true` |
| `AI_DAILY_LIMIT` | 每用户每天使用系统 Key 的次数（`0` 不限制） | `50` |
| `BOOTSTRAP_ADMIN` | 首次部署时设为 `true` 以创建初始管理员 | `false` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 初始管理员账号；不填密码则随机生成并打印一次 | `admin` / 空 |
| `TZ` | 容器时区，**建议设成 `Asia/Shanghai`** | 镜像内已是东八区 |
| `AI_MODEL` | 模型名 | `deepseek-chat` |
| `AI_TEMPERATURE` / `AI_MAX_TOKENS` | 生成参数 | `0.7` / `2048` |
| `AI_CONNECT_TIMEOUT` / `AI_READ_TIMEOUT` | 调用大模型的连接/读取超时（秒） | `30` / `120` |
| `JWT_EXPIRE_MINUTES` | 令牌有效期（分钟） | `120` |
| `ASYNC_PARSE` | 文档解析是否异步执行 | `true` |
| `USER_CACHE_SECONDS` | 用户信息缓存秒数（权限变更生效延迟上限） | `60` |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` | 登录失败上限与锁定时长 | `5` / `10` |
| `APP_ALLOWED_ORIGINS` | 需要跨域时填来源白名单（逗号分隔）；同源部署留空 | 空 |

> 每个用户也可以在自己的「个人中心 → 我的 AI Key」里填上自己的 DeepSeek Key，
> 填了之后 AI 调用走他自己的额度，不占用系统每日次数，Key 会加密后入库。

## 二、首次部署要做的三件事

1. **建库**（生产模式）：先在 MySQL 里建库并执行 `sql/mysql-init.sql`。
   已有库升级用 `sql/migrate.sql`（可重复执行，幂等）。
2. **创建管理员**：设置 `BOOTSTRAP_ADMIN=true`（可同时给 `ADMIN_PASSWORD`）启动一次，
   用这个账号登录后**立即修改密码**，然后把 `BOOTSTRAP_ADMIN` 改回 `false` 重新部署。
   ——或者保持 `ALLOW_REGISTER=true` 自己注册第一个账号。
3. **健康检查**：`GET /actuator/health` 返回 `{"status":"UP"}` 即为正常，
   平台做健康探针时可以用这个路径。

## 三、微信云托管（容器平台）逐步操作

### 1. 基础配置

| 界面字段 | 填什么 |
|---|---|
| Git 仓库 | `公开仓库`（本仓库是公开的，不需要 Token） |
| 仓库地址 | `https://github.com/suyuoout/ai-study-assistant.git` |
| 分支 | `main` |
| 服务名称 | `study-assistant` |
| 部署类型 | `容器型服务` |
| 访问端口 / 服务端口 | `80` / `8080`（程序监听 8080，不要都填 80） |
| 目标目录 | `.`（Dockerfile 就在仓库根目录） |
| Dockerfile 文件 | `有`，名称 `Dockerfile` |

### 2. 环境变量（按模式二选一）

**生产模式（有 MySQL，推荐）**

```
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=<32位以上随机串>
CRYPTO_KEY=<另一串随机串>
DB_URL=jdbc:mysql://<内网地址>:3306/study_assistant?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
DB_USERNAME=<用户名>
DB_PASSWORD=<密码>
DEEPSEEK_API_KEY=<你的 Key，可留空>
UPLOAD_DIR=/data/uploads
ALLOW_REGISTER=false
BOOTSTRAP_ADMIN=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<你设的强密码>
TZ=Asia/Shanghai
```

首次部署成功后，把 `BOOTSTRAP_ADMIN` 改成 `false`。

**演示模式（没有 MySQL，先跑起来看看）**

```
JWT_SECRET=<32位以上随机串>
CRYPTO_KEY=<另一串随机串>
ALLOW_REGISTER=true
UPLOAD_DIR=/data/uploads
TZ=Asia/Shanghai
```

不要设置 `SPRING_PROFILES_ACTIVE`，程序会用容器内的 H2 文件库。

### 3. 部署日志里如果卡在「检出代码仓库」

若出现：

```
fatal: could not read Username for 'https://github.com'
fatal: expected flush after ref listing
```

说明平台构建节点拉不到 GitHub（该校验与代码无关：本仓库可匿名克隆）。
按顺序尝试：

1. 重新授权一次仓库（平台 → 源码仓库 → 重新绑定/刷新授权），确认类型是 `公开仓库`；
2. 同步一份 Gitee 镜像，平台改成从 Gitee 拉取（国内最稳）：

   ```bash
   git remote add gitee https://gitee.com/<你的账号>/ai-study-assistant.git
   git push gitee main
   ```

3. 改用「上传代码包」或「镜像部署」：在 GitHub Actions 里构建镜像并推送到
   腾讯云 TCR / 云托管镜像仓库，平台按镜像创建版本，完全不依赖 git 克隆。

### 4. 构建慢或拉基础镜像失败

`Dockerfile` 已默认使用国内镜像加速：

- npm 走 `registry.npmmirror.com`
- Maven 走 `maven.aliyun.com/repository/public`
- 容器时区固定 `Asia/Shanghai`（避免凌晨 0-8 点把数据算成"昨天"）

需要走官方源时用构建参数覆盖：
`--build-arg NPM_REGISTRY=https://registry.npmjs.org --build-arg MAVEN_MIRROR=https://repo.maven.apache.org/maven2`。

## 四、自有服务器（Docker Compose 一键起）

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
java -Duser.timezone=Asia/Shanghai -jar study-assisiant-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 六、数据库

- 生产使用 MySQL 8，建表脚本：`sql/mysql-init.sql`（在目标库里执行）
- 数据库名建议 `study_assistant`
- 备份：`mysqldump -uroot -p study_assistant > backup_$(date +%F).sql`
- 注意：本地/离线模式用的是内置 H2 文件库，两边的数据互不相通

## 七、升级已有部署

1. 拉取新代码并重新构建镜像（或重新打包 jar）
2. 对已有数据库执行一次 `sql/migrate.sql`（幂等，可重复执行）
3. 新增的环境变量按上表补齐
4. 重启服务，访问 `/actuator/health` 确认状态

## 八、更新流程

1. 本地改完代码，跑一遍 `mvn test` 与 `npm run lint && npm run test`
2. `git add . && git commit -m "..." && git push`
3. 平台勾了自动部署的话会自动重新构建发布；否则手动点一次部署
4. 服务器方式：`git pull && docker compose up -d --build`

## 九、微信云托管「版本配置」要点

| 配置项 | 建议值 | 原因 |
|---|---|---|
| 服务端口 | `8080` | 程序监听 8080（访问端口另填 80） |
| Dockerfile 名称 | `Dockerfile` | 仓库根目录已有 |
| 目标目录 | `/` 或 `.` | 都指向仓库根，两者等价 |
| CMD / ENTRYPOINT | **留空** | 留空即使用镜像自带启动命令（已包含东八区参数） |
| 日志采集路径 | `stdout` | Spring Boot 默认输出到标准输出，平台直接采集 |
| 容器规格 | 1 核 2 G 起 | 单人/小规模足够；文档解析并发受内部线程池限制，不会打满 CPU |
| 实例副本数 | 演示模式必须 `1 ~ 1` | 见下方说明 |
| 公网访问 | **必须开启**（或配置 VPC + NAT） | 关闭后容器没有公网出站能力，**AI 功能会全部失败** |
| 存储挂载 | 建议启用 | 见下方说明 |

### 1. 公网访问必须开启

云托管里「公网访问」控制的是**出站**能力：关闭时容器访问不了 `api.deepseek.com`，
表现就是所有 AI 功能报「AI 服务连接失败，请稍后重试」。
如果因为合规原因必须关闭，就要按平台提示配置 VPC + NAT 网关实现公网出站。

### 2. 存储挂载建议启用

容器是无状态的，不挂存储时下列数据在**每次重新部署后都会丢失**：

| 挂载路径 | 存放内容 | 是否必需 |
|---|---|---|
| `/data/uploads` | 用户上传的 PDF/TXT/DOCX | 想保留资料就要挂 |
| `/root/.study-assistant` | 演示模式的 H2 数据库文件（生产模式用 MySQL，不需要） | 演示模式想保数据就要挂 |

### 3. 实例副本数：演示模式必须是 1

- **演示模式（H2）**：H2 是单文件数据库，多个实例同时读写同一个文件会损坏数据，
  因此必须限制为「最小 1、最大 1」，并关闭自动扩缩容。
- **生产模式（MySQL）**：可以多副本，AI 用量计数存在数据库里所以是准的；
  唯一的例外是登录失败次数限制仍然是进程内计数（每个实例各算一份），
  多实例部署时建议改用 Redis（见「安全说明」）。

## 十、常见问题

- **启动日志出现「生产环境缺少必需的环境变量：…」**：按提示补齐变量后重新部署。
  这是刻意设计的快速失败，避免用弱密钥或空密码把服务跑起来。
- **本地能跑、容器里 502/超时**：确认「服务端口」填的是 **8080**，
  且没有把「访问端口」也填成 8080（访问端口通常是 80）。
- **AI 用不了**：接口会返回上游的具体错误（例如 `AI 服务返回错误（401）`）。
  没有配置 `DEEPSEEK_API_KEY` 时，需要用户在「个人中心」填写自己的 Key。
- **上传资料一直「解析中」**：看后端日志里 `doc-parse-` 线程的输出，
  失败原因也会显示在资料列表的悬停提示里。
- **登录提示「登录失败次数过多」**：默认 5 次失败锁定 10 分钟，
  可用 `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` 调整。
- **重新部署后数据没了**：说明用的是演示模式（H2）或没给上传目录挂数据卷；
  改用 MySQL 并把 `UPLOAD_DIR` 挂到持久化存储即可。
- **构建节点拉不到基础镜像**：`Dockerfile` 用到的 4 个标签都已确认存在于 Docker Hub
  （`node:22-alpine`、`maven:3.9-eclipse-temurin-25`、`eclipse-temurin:25-jre`、`mysql:8.4`）；
  若平台构建节点访问 Docker Hub 受限，给构建配置镜像加速，或改用镜像部署方式。
- **前端构建报原生模块（napi/rolldown）相关错误**：把 `Dockerfile` 第一行的
  `node:22-alpine` 换成 `node:22-slim`（glibc 版，体积略大但兼容性更好）。
