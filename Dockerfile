# 1) 构建前端
FROM node:22-alpine AS web
WORKDIR /web
COPY study-assisiant-web/package.json study-assisiant-web/package-lock.json ./
RUN npm ci || npm install
COPY study-assisiant-web/ ./
RUN npm run build

# 2) 构建后端（把前端产物并入 static）
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY study-assisiant/pom.xml ./
COPY study-assisiant/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

# 3) 运行（仅 JRE，镜像更小）
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/target/study-assisiant-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
