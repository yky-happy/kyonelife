# syntax=docker/dockerfile:1
# =========================================================
# 读者端（blog-web，Vue3 + Vite8）构建镜像。
# 只负责“构建静态文件并发布到共享卷”，不常驻：
#   build 阶段用 Node 22 打包出 dist；
#   运行阶段把 dist 拷进挂载的卷 /out，交给网关 nginx 托管。
# 生产构建不注入 VITE_API_BASE_URL => baseURL 为空 => 同源，API 由 nginx 反代。
# =========================================================

FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
# 用 npm install 而非 npm ci：容忍 lock 与 package.json 轻微不同步（缺 @emnapi/* 等可选原生依赖）
RUN npm install --no-audit --no-fund
COPY . .
RUN npm run build

# 运行阶段：极小镜像，仅承载 dist + 一条发布命令
FROM alpine:3.20
COPY --from=build /app/dist /dist
# 每次启动清空卷再拷入最新产物，保证前端更新后卷内容同步刷新
CMD ["sh", "-c", "rm -rf /out/* && cp -a /dist/. /out/ && echo 'blog-web dist published to /out'"]
