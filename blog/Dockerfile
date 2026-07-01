# syntax=docker/dockerfile:1
# =========================================================
# 后端（Spring Boot 3.3 / Java 21）多阶段镜像
#   build   阶段：用 Maven 打包成可执行 jar（跳过测试，测试交给 CI）
#   runtime 阶段：只带 JRE，非 root 运行，体积小、攻击面小
# 构建上下文为本目录（blog/），ip2region.xdb 随 resources 一并打进 jar。
# =========================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只拷贝 pom：依赖下载与源码改动解耦，源码变更不必重新拉依赖
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# 再拷贝源码并打包
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# 容器内时区与中文博客一致，避免统计/时间字段错乱
ENV TZ=Asia/Shanghai

# 非 root 运行
RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/logs && chown -R app:app /app

COPY --from=build /build/target/*.jar /app/app.jar
RUN chown app:app /app/app.jar
USER app

EXPOSE 8080

# MaxRAMPercentage：让 JVM 跟随容器内存上限，避免容器里堆算错
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
