# Bug #002 — 前端发请求后端报 401，控制台抛 NotLoginException

- **发现日期**：2026-05-24
- **影响范围**：blog-admin 全部跨域请求（GET/POST/PUT/DELETE 均受影响）
- **严重等级**：阻塞（登录后所有业务接口均无法调用）
- **根因**：Sa-Token 拦截器拦截了浏览器的 OPTIONS 预检请求，OPTIONS 不带 token，导致直接 401
- **修复文件**：`blog/src/main/java/com/yky/blog/common/config/SaTokenConfig.java`

---

## 一、现象

1. 浏览器已正常登录，Sa-Token token 存于 Pinia store
2. 进入标签管理页，前端发起 `GET /admin/tag/page` 请求
3. 前端弹出"请求失败"或跳转到登录页
4. 后端控制台打印：

```
ERROR o.a.c.c.C.[Tomcat].[localhost] : Exception Processing [ErrorPage[errorCode=0, location=/error]]
jakarta.servlet.ServletException: Request processing failed: cn.dev33.satoken.exception.NotLoginException: 未能读取到有效 token
    at com.yky.blog.common.config.SaTokenConfig.lambda$addInterceptors$0(SaTokenConfig.java:14)
    at cn.dev33.satoken.interceptor.SaInterceptor.preHandle(SaInterceptor.java:114)
```

---

## 二、定位过程

### 关键证据

| 证据 | 含义 |
|------|------|
| 后端报 `NotLoginException: 未能读取到有效 token` | Sa-Token 拦截器在校验 token，但没读到 |
| 报错堆栈指向 `SaTokenConfig.lambda$addInterceptors$0` | 是 `SaInterceptor` 里的 `checkLogin()` 抛出的 |
| F12 Network 里能看到该请求方法是 `OPTIONS` | 这是浏览器的 CORS 预检请求，不是真正的业务请求 |
| 前端 `request.ts` 拦截器会自动给请求头加 `Authorization` | 但 OPTIONS 预检是浏览器自动发出的，前端代码无法干预 |

### 推导

浏览器对跨域的非简单请求（如带 `Authorization` 头的 GET、POST 等），会先发一个 **OPTIONS 预检请求**探路。这个 OPTIONS 请求由浏览器自动发出，**不携带任何业务 header（包括 Authorization/token）**。

```
浏览器发请求流程：
  1. OPTIONS /admin/tag/page   ← 预检，无 token
  2. GET     /admin/tag/page   ← 真实请求，带 token（等预检通过后才发）
```

Sa-Token 的拦截器配置了 `.addPathPatterns("/**")`，把 OPTIONS 预检也拦截了，读不到 token，直接抛 `NotLoginException`，真实请求根本没机会发出。

---

## 三、修复方案

在 `SaInterceptor` 里重写 `preHandle`，检测到 OPTIONS 方法直接放行，不做 token 校验：

**`SaTokenConfig.java`**：

```java
registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()) {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        return super.preHandle(request, response, handler);
    }
})
.addPathPatterns("/**")
.excludePathPatterns(...);
```

**为什么这样改是安全的**：OPTIONS 预检本身不携带任何业务数据，也不会触发任何业务逻辑，放行它只是让浏览器完成握手，真正的 GET/POST 请求依然经过 token 校验。

---

## 四、与 Bug #001 的关系

| | Bug #001 | Bug #002 |
|-|----------|----------|
| **现象** | 前端报"网络异常" | 前端报 401 / 跳转登录页 |
| **OPTIONS 请求的命运** | 后端没有 CORS 头，浏览器直接拦截，OPTIONS 返回 200 但无 CORS 响应头 | 后端有 CORS 配置，OPTIONS 能到达 Sa-Token 拦截器，被 401 拦回 |
| **修复层** | 后端加 `CorsConfig`，返回 CORS 响应头 | 后端 `SaTokenConfig` 放行 OPTIONS 方法 |
| **修复顺序** | 必须先修 001 | 001 修完才会暴露 002 |

两个 bug 都和 OPTIONS 预检有关，是同一个 CORS 调用链上的两道关卡：
```
浏览器 OPTIONS 预检
  → 关卡1：有没有 CORS 响应头？（Bug #001）
  → 关卡2：有没有被 Sa-Token 401？（Bug #002）
  → 预检通过，浏览器发真实请求
```

---

## 五、教训

1. 配置 Sa-Token 拦截器时，`/**` 会覆盖所有 HTTP 方法，**必须单独处理 OPTIONS**，否则跨域场景下必然复现。
2. 后续新项目脚手架应把"OPTIONS 放行"写进 `SaTokenConfig` 模板，和 `CorsConfig` 一起列入"通用配置三件套"。
3. 这类问题在 Postman 测试时**完全不会出现**（Postman 不发 OPTIONS 预检），只有浏览器联调时才暴露，是跨域场景独有的坑。
