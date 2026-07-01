# kyonelife · 数据驱动的智能内容平台

> 一个把普通博客升级为「数据驱动 + AI 赋能」内容平台的全栈项目：埋点采集用户行为 → Redis Stream 异步落库 → 每日聚合出热门榜 → AI 基于真实数据做选题和创作 → 人工确认后发布，形成 **「数据 → AI 内容 → 再埋点」** 闭环。

后端 Spring Boot 3 + Java 21，前台/后台各一个 Vue 3 应用，一条 `docker compose up` 即可拉起全部依赖。

---

## ✨ 功能亮点

| 模块 | 能力 |
|---|---|
| **前台（blog-web）** | 文章浏览/搜索、合集、标签、评论与点赞、手机验证码登录、Banner 轮播、Markdown 渲染 + 代码高亮 |
| **后台（blog-admin）** | 文章管理（草稿/发布）、合集与标签、Banner、评论审核、用户管理、数据看板、操作日志 / 运行日志、AI 创作助手 |
| **RBAC 权限** | 管理员 — 角色 — 菜单三级模型，**菜单/按钮级权限动态下发**，前端路由与操作按钮按权限渲染 |
| **埋点与分析** | 事件上报 → **Redis Stream** 异步消费落库 → 每日定时聚合 → 热门文章/标签/搜索词榜单（实时用 Redis ZSet） |
| **AI 能力（两层）** | ①**内容辅助**：一键生成摘要、推荐标签；②**工具型 Agent**：基于埋点数据自主调用工具生成选题与文章草稿。详见 [`AI第二层-工具型Agent设计.md`](AI第二层-工具型Agent设计.md) |
| **工程基建** | Sa-Token 鉴权、`@RateLimit` 注解限流、敏感词过滤、ip2region 离线 IP 归属地、MinIO 图片/视频存储、AI 调用审计表 `ai_call_log`、统一异常与返回体 |

> AI 未配置 Key 时全部接口**自动降级为本地规则结果**，不影响其余功能，可零成本跑起来看效果。

---

## 🧱 技术栈

**后端**：Java 21 · Spring Boot 3.3.5 · MyBatis-Plus 3.5.5 · Sa-Token 1.37 · Spring AI 1.0.9（DeepSeek）· Redis(Lettuce) · MySQL 8 · MinIO 8.5 · Knife4j(OpenAPI3) 4.5 · ip2region 2.7

**前端**：Vue 3 · TypeScript · Vite · Element Plus（后台）· Axios · highlight.js / DOMPurify（前台）

**部署**：Docker / Docker Compose（MySQL + Redis + MinIO + 后端一键编排）

---

## 📁 项目结构

```
kyonelife/
├── blog/                     # 后端 Spring Boot
│   ├── src/main/java/com/yky/blog/
│   │   ├── api/              # 前台接口（文章/评论/上传/埋点上报…）
│   │   ├── admin/            # 后台接口（内容管理/RBAC/看板/AI 创作助手…）
│   │   ├── auth/             # 登录鉴权（前台验证码 / 后台账号）
│   │   └── common/           # 公共层：ai、config、redis、util、annotation…
│   │       └── ai/           # AI 底座：AgentLoopRunner、ContentAgentTools…
│   ├── sql/                  # 数据库脚本（init.sql 为整合建表 + 种子）
│   ├── Dockerfile            # 后端多阶段镜像
│   └── pom.xml
├── blog-admin/               # 后台前端（Vue3 + Element Plus）
├── blog-web/                 # 前台前端（Vue3）
├── docs/                     # 设计与学习文档（架构、埋点、AI 选型…）
├── docker-compose.yml        # 一键编排 MySQL + Redis + MinIO + 后端
└── .env.example              # 部署所需环境变量模板
```

---

## 🚀 快速开始

### 方式一：Docker Compose 一键启动（推荐）

前置：安装 Docker 与 Docker Compose。

```bash
# 1. 准备环境变量
cp .env.example .env
#    本地演示至少改这几项：MYSQL_PASSWORD / REDIS_PASSWORD / MINIO_ACCESS_KEY / MINIO_SECRET_KEY
#    并把 MINIO_PUBLIC_ENDPOINT 改成 http://localhost:9000
#    可选：填 DEEPSEEK_API_KEY 体验真实 AI（不填则自动降级）

# 2. 构建并启动（首次会自动执行 sql/init.sql 建库建表 + 写入种子数据）
docker compose up -d --build

# 3. 查看状态 / 日志
docker compose ps
docker compose logs -f blog
```

启动后：

| 入口 | 地址 |
|---|---|
| 后端接口文档（Knife4j） | http://localhost:8080/doc.html |
| MinIO 控制台 | http://localhost:9001 |

**默认管理员账号**（来自 `sql/init.sql` 种子）：用户名 `yky` / 密码 `admin123`（上线前务必修改）。

前端在宿主机启动（见方式二第 3 步）即可联调，默认已指向 `http://localhost:8080`。

停止 / 清理：

```bash
docker compose down            # 停止容器（保留数据卷 docker-data/）
docker compose down -v         # 连数据一起清（谨慎）
```

### 方式二：本地开发

**后端**（需本机有 MySQL / Redis / MinIO，或只用 compose 起依赖：`docker compose up -d mysql redis minio`）：

```bash
# 首次导入数据库
mysql -uroot -p < blog/sql/init.sql

# dev 环境的连接与密钥放在 blog/src/main/resources/application-dev.yml（已 gitignore，需自行创建）
cd blog
./mvnw spring-boot:run          # 默认 dev profile，端口 8080
```

**前端**：

```bash
cd blog-admin && npm install && npm run dev            # 后台，默认 http://localhost:5173
cd blog-web   && npm install && npm run dev -- --port 5174   # 前台
```

> 前端 API 地址目前写在各自 `src/utils/request.ts` 的 `baseURL`（默认 `http://localhost:8080`），如后端不在本机可在此调整。

---

## ⚙️ 环境变量说明

部署相关变量集中在 `.env`（模板见 `.env.example`），后端 `prod` profile 全部从环境变量读取，**不含任何明文密钥进版本库**：

| 变量 | 说明 |
|---|---|
| `MYSQL_PASSWORD` | MySQL root 密码（后端连库也用它） |
| `REDIS_PASSWORD` | Redis 密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 账号密码（secret ≥ 8 位） |
| `MINIO_PUBLIC_ENDPOINT` | 浏览器访问图片的公网地址（本地填 `http://localhost:9000`，生产填 CDN/域名） |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端域名，逗号分隔 |
| `DEEPSEEK_API_KEY` | DeepSeek Key（可选，不填 AI 自动降级） |

> 配置分层：`application.yml`（共享、无密钥）＋ `application-dev.yml`（本地、gitignore）＋ `application-prod.yml`（全走环境变量）。切换用 `SPRING_PROFILES_ACTIVE`。

---

## 🗄️ 数据库脚本

`blog/sql/` 下：

- `init.sql` — **整合脚本**：建库 `kyonelife` + 全部表 + 种子数据（管理员、角色、菜单、权限）。compose 首次启动会自动执行。
- 其余 `ai.sql` / `analytics_stat.sql` / `event_log*.sql` / `rbac_permission_patch.sql` 等为**分模块 / 增量脚本**，用于单独理解或对已有库打补丁。

---

## 📚 更多文档

设计与学习记录见 [`docs/`](docs/)：

- [数据驱动智能内容平台 — 架构设计](docs/数据驱动智能内容平台-架构设计.md)
- [埋点系统与 Redis 使用说明](docs/埋点系统与Redis使用说明.md)
- [数据聚合与 Redis 缓存开发记录](docs/2026.06.23-数据聚合与Redis缓存开发记录.md)
- [AI 能力规划与技术选型](docs/AI能力规划与技术选型.md)
- [AI 第二层 · 工具型 Agent 设计](AI第二层-工具型Agent设计.md)

---

## 📝 备注

- 架构设计文档中提及的 MQ / ClickHouse 为演进规划；当前实现的埋点管道以 **Redis Stream + MySQL 聚合表**落地，轻量且够用。
- 本项目为个人学习与作品集项目，欢迎参考。
