# ---------- 1) 构建前端 ----------
FROM node:22-alpine AS web

# 国内构建节点访问 npm 官方源常常很慢，默认走 npmmirror；
# 需要走官方源时用 --build-arg NPM_REGISTRY=https://registry.npmjs.org
ARG NPM_REGISTRY=https://registry.npmmirror.com

WORKDIR /web
COPY study-assisiant-web/package.json study-assisiant-web/package-lock.json ./
RUN npm config set registry "$NPM_REGISTRY" \
    && (npm ci || npm install)
COPY study-assisiant-web/ ./
RUN npm run build

# ---------- 2) 构建后端（把前端产物并入 static） ----------
FROM maven:3.9-eclipse-temurin-25 AS build

# 默认用阿里云 Maven 镜像加速依赖下载；
# 需要走官方源时用 --build-arg MAVEN_MIRROR=https://repo.maven.apache.org/maven2
ARG MAVEN_MIRROR=https://maven.aliyun.com/repository/public

WORKDIR /src
COPY study-assisiant/pom.xml ./
COPY study-assisiant/src ./src
COPY --from=web /web/dist ./src/main/resources/static

RUN mkdir -p /root/.m2 \
    && printf '<settings><mirrors><mirror><id>central-mirror</id><mirrorOf>central</mirrorOf><url>%s</url></mirror></mirrors></settings>' "$MAVEN_MIRROR" > /root/.m2/settings.xml \
    && mvn -B -q -DskipTests package

# ---------- 3) 运行（仅 JRE，镜像更小） ----------
FROM eclipse-temurin:25-jre

# 容器默认时区是 UTC，会让运维凌晨 0-8 点看到"昨天"的数据（LocalDate.now()），
# 这里统一切到东八区，并同时固定 JVM 时区。
ENV TZ=Asia/Shanghai

WORKDIR /app
COPY --from=build /src/target/study-assisiant-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
