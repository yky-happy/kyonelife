# kyonelife 腾讯云 Docker 部署指南（域名 + HTTPS）

目标：把项目部署到腾讯云服务器，**点击域名即可访问**读者端博客，`admin` 子域名访问管理后台，全站 HTTPS。

## 一、架构

对公网开放的只有一个 **网关（nginx）** 容器，其余全部在内网：

```
                         腾讯云服务器（公网 IP）
  https://你的域名  ┐                                    ┌─ mysql   (内网)
  https://admin.你的域名 ┼──►  gateway(nginx) 80/443 ──►  blog:8080  ├─ redis   (内网)
                    ┘        · 托管前端静态                          └─ minio   (内网, 图片经 /minio 反代)
                             · 反代 /api /admin /minio
                             · TLS 终止（证书）
```

- **读者端** `blog-web` → `https://你的域名`
- **管理后台** `blog-admin` → `https://admin.你的域名`
- 后端 `blog`、`mysql`、`redis`、`minio` 只在 Docker 内网互通，公网访问不到（数据库调试端口仅绑定服务器本机 `127.0.0.1`）。

## 二、前置条件

1. **一台腾讯云服务器**（建议 2 核 4G 起；前端 + 后端首次构建较吃内存，1G 内存需加 swap，见文末）。
2. **域名已备案**（大陆服务器 80/443 访问未备案域名会被拦截）。
3. **两条 DNS 解析**都指向服务器公网 IP：
   | 主机记录 | 类型 | 记录值 |
   |---|---|---|
   | `@`（或你想用的主域名，如 `www`） | A | 服务器公网 IP |
   | `admin` | A | 服务器公网 IP |
4. **安全组放行** 80、443（以及 22 用于 SSH）。数据库端口无需放行。

## 三、部署步骤

以下命令都在**服务器**上、项目根目录 `kyonelife/` 执行。

### 1. 安装 Docker（若未装）

```bash
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
systemctl enable --now docker
docker compose version   # 确认自带 compose v2
```

### 2. 拉取代码

```bash
git clone <你的仓库地址> kyonelife && cd kyonelife
# 或用 scp/rsync 把本地项目传上去
```

### 3. 配置环境变量

```bash
cp .env.example .env
vim .env
```

至少改这几项：

```ini
DOMAIN=你的域名.com
ADMIN_DOMAIN=admin.你的域名.com

MYSQL_PASSWORD=改成强密码
REDIS_PASSWORD=改成强密码
MINIO_ACCESS_KEY=改成强密码
MINIO_SECRET_KEY=改成强密码

# 图片公网地址：走网关反代，填主域名 + /minio
MINIO_PUBLIC_ENDPOINT=https://你的域名.com/minio

CORS_ALLOWED_ORIGINS=https://你的域名.com,https://admin.你的域名.com

# 可选：DeepSeek AI（不填则 AI 接口自动降级，不影响其他功能）
DEEPSEEK_API_KEY=sk-xxxx
```

### 4. 放置 HTTPS 证书

在腾讯云控制台申请免费证书（为主域名和 `admin` 子域名各申请一张），下载 **Nginx 格式**，按固定文件名放到 `deploy/nginx/certs/`：

```
deploy/nginx/certs/main.crt    # 主域名证书(bundle)
deploy/nginx/certs/main.key    # 主域名私钥
deploy/nginx/certs/admin.crt   # 后台子域名证书
deploy/nginx/certs/admin.key   # 后台私钥
```

详见 `deploy/nginx/certs/README.md`。（想用 Let's Encrypt 自动续期见文末附录。）

### 5. 一键启动

```bash
docker compose up -d --build
```

首次会：构建后端 jar（Maven）、构建两个前端（npm）、拉起 MySQL/Redis/MinIO/网关。数据库首次启动自动建表 + 种子数据（含管理员 `yky / admin123`）。

查看状态与日志：

```bash
docker compose ps
docker compose logs -f gateway    # 网关
docker compose logs -f blog       # 后端
```

### 6. 访问

- 读者端： **https://你的域名.com**
- 管理后台：**https://admin.你的域名.com**（默认账号 `yky / admin123`，**登录后请立刻改密**）

## 四、日常运维

```bash
# 改了代码后重新部署（会重建镜像、刷新前端静态）
git pull
docker compose up -d --build

# 只更新证书后让网关重载
docker compose restart gateway

# 停止 / 启动 / 查看
docker compose down
docker compose up -d
docker compose ps

# 数据都在 ./docker-data/ 下（mysql/redis/minio/日志），备份直接打包该目录
```

> 前端更新原理：`web-build` / `admin-build` 容器每次 `up` 会重新打包并把 `dist`
> 刷新到共享卷，网关直接托管，无需重启网关。

## 五、常见问题

- **打开域名转圈/502**：`docker compose logs blog` 看后端是否起来；后端依赖 MySQL 健康检查通过后才启动，首次建表需等十几秒。
- **图片打不开**：确认 `.env` 里 `MINIO_PUBLIC_ENDPOINT=https://主域名/minio`，改后需 `docker compose up -d`（让 blog 重读环境变量）。
- **证书不生效/警告**：确认 4 个证书文件名正确、是 PEM 格式，然后 `docker compose restart gateway`。
- **域名能 ping 通但打不开**：检查腾讯云安全组是否放行 80/443、域名是否已备案。
- **要支持 www**：把 nginx 模板 `server_name ${DOMAIN};` 改成 `server_name ${DOMAIN} www.${DOMAIN};` 并加一条 `www` 的 DNS 解析。

## 附：用 Let's Encrypt 申请免费证书（自动续期，替代腾讯云证书）

先临时用 HTTP 起网关（或用 `--webroot`）。最简做法是用一次性 certbot 容器签发到共享目录，再软链到 `certs/`：

```bash
# 1) 确保 DNS 已解析、80 端口可访问
docker run --rm -p 80:80 \
  -v $PWD/docker-data/certbot/etc:/etc/letsencrypt \
  certbot/certbot certonly --standalone \
  -d 你的域名.com -d admin.你的域名.com \
  --email 你的邮箱 --agree-tos --no-eff-email

# 2) 拷贝到网关约定的文件名
cd deploy/nginx/certs
cp ../../../docker-data/certbot/etc/live/你的域名.com/fullchain.pem main.crt
cp ../../../docker-data/certbot/etc/live/你的域名.com/privkey.pem   main.key
cp main.crt admin.crt && cp main.key admin.key   # 一张多域名证书可两边共用
cd ../../.. && docker compose restart gateway
```

续期：`certbot renew` 后重复第 2 步并 `docker compose restart gateway`（可写进 crontab）。

## 附：小内存服务器加 swap（1G 内存构建前端易 OOM）

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile
mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```
