# Bug #001 — 管理员登录前端报"网络异常"，后端无错误日志

- **发现日期**：2026-05-22
- **影响范围**：blog-admin 全部 axios 请求（首个暴露问题的是 `POST /admin/auth/login`）
- **严重等级**：阻塞（无法登录就进不去任何管理页）
- **根因**：后端未配置 CORS，浏览器拦截了跨域响应
- **修复方案**：采用方案二（后端加 `CorsConfig`）

---

## 一、现象

1. blog-admin 登录页输入账号密码 → 点登录 → 弹 Element Plus 提示 `网络异常，请稍后再试`
2. 后端控制台日志：
   ```
   Tomcat started on port 8080 (http) with context path '/'
   Started BlogApplication in 4.63 seconds
   Initializing Spring DispatcherServlet 'dispatcherServlet'
   Completed initialization in 1 ms
   ```
   只有 DispatcherServlet 初始化的日志，**没有任何 Controller 业务相关输出**，也没有 `AuthController.login` 调用记录或异常堆栈。

## 二、定位过程

### 关键证据

| 证据 | 含义 |
|------|------|
| `DispatcherServlet 'dispatcherServlet'` 这条日志在收到登录请求时才出现 | 请求 **确实到达了 Tomcat**，不是网络不通 |
| Controller 没有任何日志或异常 | 请求没走到 `@PostMapping("/login")` 方法体 |
| 全工程 grep `Cors`、`@CrossOrigin`、`addCorsMapping`、`CorsFilter` 均为 0 命中 | 后端 **完全没有任何 CORS 相关配置** |
| 前端 `baseURL` 是 `http://localhost:8080`，dev server 在 `http://localhost:5173` | 协议+主机相同，端口不同 → 属于跨域请求 |

### 推导

浏览器对跨域的非简单请求（带 `Content-Type: application/json` + POST）会先发一个 **OPTIONS 预检**。
- Spring 处理了 OPTIONS，但响应里没有 `Access-Control-Allow-Origin` 头
- 浏览器判定预检失败 → **直接丢弃响应，并阻止真正的 POST 发出**
- axios 拿不到任何响应 → 走 `response.interceptors` 的 `err` 分支
- `err.response` 是 undefined（请求被浏览器层面 abort，没有 HTTP 响应）
- 命中 `request.ts` 里的 `else` 分支：`ElMessage.error('网络异常，请稍后再试')`

这也解释了为什么 Controller 没日志：**OPTIONS 预检根本不会路由到业务方法**，业务方法只接收实际的 POST。

### 浏览器侧表现（验证用）

打开 F12 → Network 标签重试一次登录，能看到：
- 一个红色的 `OPTIONS /admin/auth/login` 请求，状态可能是 `(failed) CORS error` 或 200 但被标红
- Console 里有 `Access to XMLHttpRequest at 'http://localhost:8080/admin/auth/login' from origin 'http://localhost:5173' has been blocked by CORS policy: ...`
- 真正的 `POST /admin/auth/login` 根本没发出

---

## 三、修复方案对比

### 方案 A：前端 Vite Proxy 代理（开发期快速绕过）

**思路**：让浏览器把请求打到自己（5173），由 Vite dev server 在 Node 侧转发到 8080，对浏览器而言是同源请求，绕过 CORS。

**改动**：

`blog-admin/vite.config.ts`：
```ts
export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    proxy: {
      '/admin': { target: 'http://localhost:8080', changeOrigin: true },
      '/web':   { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
```

`blog-admin/src/utils/request.ts`：把 `baseURL: 'http://localhost:8080'` 改成 `baseURL: ''`。

**优点**
- 零后端改动，前端一处即可生效
- 顺带解决 cookie 跨域 SameSite 问题
- 不需要管 `allowedOrigins` 配置安全性

**缺点**
- ❌ **只在 dev server 下生效**，npm run build 后部署到 nginx 就没了
- ❌ 生产环境必须额外配 nginx 反代或后端 CORS，本质是把问题推迟到上线时
- 多人协作时容易出现"我本地能登录，部署后登录不了"的迷惑现象

**适合**：纯本地调试、临时验证、上线前的过渡。

---

### 方案 B：后端加 CORS 配置（生产推荐 ✅）

**思路**：让后端在响应里正确返回 `Access-Control-Allow-Origin` 等头部，浏览器就会放行跨域请求。

**改动**：在 `blog/src/main/java/com/yky/blog/common/config/` 下新建 `CorsConfig.java`：

```java
package com.yky.blog.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

**关键点**
- 必须用 `allowedOriginPatterns("*")`，不能用 `allowedOrigins("*")`。Spring 5.3+ 规定带 `allowCredentials(true)` 时通配符必须走 patterns 否则启动报错。
- `exposedHeaders("Authorization")`：前端如果要从响应头读 token（部分 Sa-Token 用法），必须显式暴露，否则浏览器对前端隐藏该头。
- `maxAge(3600)`：让浏览器缓存预检结果 1 小时，减少 OPTIONS 数量。
- Sa-Token 的 `SaTokenConfig` 是基于 `HandlerInterceptor`，CORS 的 `addCorsMappings` 走 Spring MVC 自带的 CORS 处理器，两者**不冲突**。

**优点**
- ✅ **dev 和 prod 都生效**，一次配置走天下
- ✅ 上线后无需依赖 nginx 配 CORS，简化部署
- ✅ 后端是 CORS 的"权威源"，前端无论换什么域名都不用动

**缺点**
- 现在 `allowedOriginPatterns("*")` 是开发期宽松配置，上线前**应当**收窄到具体域名（例如 `https://blog-admin.yourdomain.com`），否则任意网站都能跨域调你的接口
- 需要一次后端重启

**适合**：要上线、要稳定、要 dev 和 prod 一致的场景。**本项目选用此方案。**

---

## 四、上线前收尾 TODO

- [ ] 把 `allowedOriginPatterns("*")` 改为白名单形式，例如：
  ```java
  .allowedOriginPatterns("http://localhost:5173", "https://admin.yourdomain.com")
  ```
- [ ] 如果 nginx 也配了 CORS 头，确认前后端只在一处返回（重复返回浏览器会拒绝）
- [ ] 验证 `Sa-Token` 的 `tokenName`（默认 `satoken`）是否在 `allowedHeaders` 范围内 —— 当前配置 `allowedHeaders("*")` 已经覆盖

## 五、教训

1. 看到"网络异常"但后端 Tomcat 已启动、又有 DispatcherServlet 日志，第一反应应该是 CORS / 预检，而不是网络不通。
2. 项目初始化时 `Knife4jConfig`、`SaTokenConfig` 都建了，**唯独漏了 `CorsConfig`** —— 后续新项目脚手架应当把 CORS 默认列入"通用配置三件套"清单。
3. 前端 `request.ts` 的错误兜底文案"网络异常"对 CORS 错误太模糊，可考虑在 `err.response == null && err.message` 中包含 `Network Error` 时提示"跨域或后端不可达"，更利于定位。

---

## 附录：CORS 是什么（概念扫盲）

### 一句话理解

CORS = **Cross-Origin Resource Sharing**（跨域资源共享），是**浏览器**的一个安全规则：默认情况下，A 网站的 JS 代码不允许去请求 B 网站的接口。CORS 是这个规则的"放行机制"——B 网站必须明确表态"我允许 A 来调我"，浏览器才放行。

### 为什么浏览器要做这件事

设想没有这个规则的世界：

- 用户刚在 `bank.com` 登录完，cookie 还在浏览器里
- 用户打开一个新标签页访问 `evil.com`
- `evil.com` 的 JS 偷偷调用 `bank.com/transfer?to=hacker&amount=10000`
- 浏览器**自动带上用户的 cookie**，钱被转走

为了防止这种事，浏览器规定：**JS 发起的跨域请求，必须经过目标服务器的明确许可**。

### 什么算"跨域"

URL 的 **协议 + 域名 + 端口** 三者完全一致才叫"同源"，任意一个不同都是"跨域"：

| 当前页面 | 请求目标 | 是否同源 |
|---------|---------|---------|
| `http://localhost:5173` | `http://localhost:5173/api` | ✅ 同源 |
| `http://localhost:5173` | `http://localhost:8080/api` | ❌ 端口不同 |
| `https://kyonelife.com` | `http://kyonelife.com` | ❌ 协议不同 |
| `https://blog.kyonelife.com` | `https://api.kyonelife.com` | ❌ 子域名不同 |

**本 bug 命中的就是第二种**：前端 5173、后端 8080 端口不同 → 跨域 → 浏览器拦截。

### 服务器如何"明确许可"

服务器在响应里多塞几个 HTTP 头：

```
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
```

浏览器看到这些头，对照本次请求是否被允许 —— 是 → 放行，否 → 拦截并报 CORS 错误。

这就是本项目 `CorsConfig.java` 里那几行配置在做的事：

```java
.allowedOriginPatterns("*")    // 允许哪些来源（生产应改成白名单）
.allowedMethods("*")            // 允许哪些 HTTP 方法
.allowedHeaders("*")            // 允许哪些请求头
.allowCredentials(true)         // 允许携带 cookie
```

Spring 收到请求时，会自动给响应加上对应的 `Access-Control-Allow-*` 头。

### 预检请求（OPTIONS）

对"非简单请求"（带 `Content-Type: application/json` 的 POST、PUT、DELETE 等），浏览器会先发一个 OPTIONS 请求**探路**：

```
1. 浏览器：嘿服务器，我可以从 localhost:5173 发个 POST /admin/auth/login 吗？带 Content-Type 头
   （这就是 OPTIONS 预检）
2. 服务器：可以，看我返回的 Access-Control-Allow-* 头
3. 浏览器：好，那我把真正的 POST 请求发了
```

本次报错就死在第 2 步——服务器没回 CORS 头，浏览器直接放弃，**连真正的 POST 都没发出去**。所以后端 Controller 看不到任何业务日志，只有最初的 `DispatcherServlet` 初始化日志。

### 常见误区

- ❌ **"CORS 是后端的安全机制"** → 不是，是**浏览器**的机制。用 Postman、curl 调接口完全没 CORS 一说，CORS 只在浏览器里生效。
- ❌ **"加了 CORS 接口就不安全了"** → 反了。CORS 是浏览器**额外加的一层**保护，没 CORS 反而是默认禁止。
- ❌ **"反正同公司的接口，开 `*` 就好了"** → 开发可以，生产**绝对不能**。等于任何网站都能跨域调你的接口，钓鱼网站、自动化脚本可以利用登录用户的 cookie 干坏事。

### 在本项目里的全貌

```
本地开发：
  浏览器 (localhost:5173) ─ 跨域 ─> Spring Boot (localhost:8080)
                                       ↑ 必须配 CORS

生产环境（推荐方案）：
  浏览器 (admin.kyonelife.com) ─ 同域 ─> Nginx
                                          ├─ /         → 前端静态文件
                                          └─ /admin/*  → 反代到 127.0.0.1:8080
                                          ↑ 这种方式下根本没有跨域，CORS 都不需要了
```

生产环境如果用 nginx 反代，理论上 CORS 都可以不开。但留着也无害，多一层保险，也兼容未来前后端分别独立部署到不同子域名的情况。
