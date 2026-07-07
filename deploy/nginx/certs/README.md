# HTTPS 证书放这里

网关按固定文件名读取证书，请把证书放成下面 4 个文件（PEM 格式）：

```
deploy/nginx/certs/
  main.crt     # 主域名 your-domain.com 的证书（含证书链 fullchain）
  main.key     # 主域名私钥
  admin.crt    # 后台 admin.your-domain.com 的证书
  admin.key    # 后台私钥
```

## 方式一：腾讯云免费证书（最省事，推荐）

1. 腾讯云控制台 → SSL 证书 → 申请免费证书，为 `your-domain.com` 和
   `admin.your-domain.com` 各申请一张（免费证书是单域名，需要各申请一张）。
2. 签发后下载「Nginx」格式，解压得到 `xxx_bundle.crt` 和 `xxx.key`。
3. 按上面的文件名放好：主域名那张 → `main.crt` / `main.key`；
   后台那张 → `admin.crt` / `admin.key`。
4. 回到项目根目录 `docker compose restart gateway` 即可生效。

> 如果你申请的是一张覆盖两个域名的多域名/通配证书，
> 可以把 main.* 和 admin.* 指向同一份文件（复制成两份即可）。

## 方式二：Let's Encrypt / certbot（自动续期）

见项目根目录 `README-部署.md` 的「附：用 Let's Encrypt 申请免费证书」一节。

> 本目录除本 README 外不进版本库（证书是私钥，切勿提交到 git）。
