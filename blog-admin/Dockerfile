# syntax=docker/dockerfile:1
# =========================================================
# 管理后台（blog-admin，Vue3 + Vite8）构建镜像。
# 与 blog-web 同理：构建 dist 并发布到共享卷，由网关 nginx 在 admin 子域名托管。
# 生产构建不注入 VITE_API_BASE_URL => 同源，/api、/admin 由 nginx 反代。
# =========================================================

FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
# 用 npm install 而非 npm ci：容忍 lock 与 package.json 轻微不同步（缺 @emnapi/* 等可选原生依赖）
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

FROM alpine:3.20
COPY --from=build /app/dist /dist
CMD ["sh", "-c", "rm -rf /out/* && cp -a /dist/. /out/ && echo 'blog-admin dist published to /out'"]
