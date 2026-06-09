# MinIO、操作日志、运行日志分析功能测试

## 一、测试范围

本测试文档覆盖当前工程化增强阶段的四个模块：

```text
1. MinIO 文件上传系统
2. 后台操作日志系统
3. 运行日志分析系统
4. 行为埋点与数据分析看板
```

目标是验证后台内容生产链路和日志分析链路是否可用：

```text
后台登录 -> 上传文件 -> 写文章 -> 记录操作日志 -> 生成运行日志 -> 前台访问上报埋点 -> 后台分析数据
```

## 二、测试前准备

### 2.1 基础服务

确认 MySQL 已启动，并已执行：

```text
blog/sql/init.sql
blog/sql/operation_log.sql
blog/sql/event_log.sql
```

确认 MinIO 已启动：

```bash
docker compose -f docker-compose.minio.yml up -d
```

确认后端配置与 MinIO 一致：

```text
minio.endpoint=http://localhost:9000
minio.bucket=kyonelife
minio.access-key=kyonelife
minio.secret-key=kyonelife123456
```

### 2.2 启动项目

后端：

```bash
cd blog
./mvnw spring-boot:run
```

后台管理端：

```bash
cd blog-admin
npm run dev
```

前台展示端：

```bash
cd blog-web
npm run dev
```

### 2.3 测试账号

使用初始化脚本中的管理员账号登录后台。

如果登录失败，先确认：

```text
admin 表中存在管理员
密码与初始化脚本一致
Sa-Token 返回 Authorization
前端请求头携带 Authorization
```

## 三、MinIO 文件上传测试

### 3.1 图片上传接口

测试接口：

```text
POST /admin/file/upload
```

测试步骤：

```text
1. 登录后台
2. 进入 写文章 页面
3. 在封面图区域上传 jpg/png/webp/gif 图片
4. 等待上传完成
5. 检查封面 URL 是否自动回填
6. 打开返回的 URL，确认图片可以访问
7. 登录 MinIO 控制台，确认 bucket 中存在对应文件
```

预期结果：

```text
接口返回 code = 200
返回 data.url 和 data.filename
文章编辑页封面可以回显
MinIO 中能看到上传文件
```

### 3.2 Markdown 正文图片上传

测试步骤：

```text
1. 进入 写文章 页面
2. 在 Markdown 编辑器中使用图片上传
3. 上传一张合法图片
4. 检查正文中是否插入图片 Markdown 链接
5. 保存并发布文章
6. 进入前台文章详情页
7. 检查正文图片是否正常展示
```

预期结果：

```text
Markdown 正文中插入图片 URL
文章保存成功
前台详情页能正常展示图片
```

### 3.3 视频上传并插入正文

测试步骤：

```text
1. 进入 写文章 页面
2. 点击 上传视频并插入正文
3. 上传 mp4/webm/mov 文件
4. 检查正文中是否插入 video 标签
5. 保存并发布文章
6. 进入前台文章详情页
7. 检查视频是否可以播放
```

预期结果：

```text
视频上传成功
正文插入 video 标签
前台可以加载视频资源
```

### 3.4 非法文件类型拦截

测试步骤：

```text
1. 在图片上传入口上传 .exe/.txt/.zip 文件
2. 在视频上传入口上传非视频文件
```

预期结果：

```text
上传失败
接口返回明确错误信息
MinIO 中不会新增非法文件
```

### 3.5 文件大小限制

测试步骤：

```text
1. 上传超过 image-max-size 的图片
2. 上传超过 video-max-size 的视频
```

预期结果：

```text
上传失败
返回文件大小限制相关提示
后台页面不会回填 URL
```

## 四、操作日志系统测试

### 4.1 登录操作记录

测试步骤：

```text
1. 打开后台登录页
2. 输入正确账号密码登录
3. 进入 系统管理 -> 操作日志
4. 按模块或操作类型筛选登录相关日志
```

预期结果：

```text
能查询到登录操作日志
管理员名称正确
请求路径、请求方式、IP、User-Agent 有值
执行结果为成功
```

### 4.2 内容管理操作记录

测试步骤：

```text
1. 新建标签
2. 编辑标签
3. 新建合集
4. 编辑合集
5. 新建文章
6. 发布文章
7. 下架文章
8. 删除一篇测试文章
9. 进入操作日志页面查询
```

预期结果：

```text
每个关键操作都有日志
module 和 operation 与实际操作匹配
success = 1
costTime 有值
requestParams 中不出现明文 password/token/secret
```

### 4.3 上传操作记录

测试步骤：

```text
1. 上传文章封面
2. 上传 Markdown 图片
3. 上传视频
4. 进入操作日志页面查询 文件上传 模块
```

预期结果：

```text
能看到上传图片、上传视频日志
MultipartFile 本体不会被写入 requestParams
日志记录接口耗时和响应结果
```

### 4.4 失败操作记录

测试步骤：

```text
1. 构造非法文件上传
2. 或调用一个会触发业务异常的后台接口
3. 查询操作日志
```

预期结果：

```text
失败操作被记录
success = 0
errorMessage 有异常摘要
不会影响原接口正常返回错误信息
```

## 五、运行日志分析系统测试

### 5.1 运行状态概览

测试接口：

```text
GET /admin/log/summary?slowThreshold=100
```

后台页面：

```text
系统管理 -> 运行日志
```

测试步骤：

```text
1. 登录后台
2. 多次访问后台文章列表、前台首页、文章详情
3. 进入运行日志页面
4. 查看今日请求、WARN、ERROR、慢请求数量
5. 查看最近启动时间、最近关闭时间、日志文件路径
```

预期结果：

```text
今日请求数随访问增加
WARN/ERROR 数量能反映当天日志
慢请求数量按阈值统计
启动/关闭时间能从日志文件中解析
日志文件路径指向 logs/kyonelife-blog.log
```

### 5.2 最近日志列表

测试接口：

```text
GET /admin/log/recent?lines=200
```

测试步骤：

```text
1. 进入运行日志页面
2. 查看最近日志表格
3. 切换 lines 为 50/100/200
4. 按 INFO/WARN/ERROR 筛选
```

预期结果：

```text
日志按文件顺序展示最近记录
级别筛选生效
HTTP 请求日志能解析出 method/path/status/costTime/ip
非 HTTP 日志仍能展示原始 message
```

### 5.3 关键词搜索

测试接口：

```text
GET /admin/log/search?keyword=article&lines=200
```

测试步骤：

```text
1. 在运行日志页面输入关键词 article
2. 点击查询
3. 输入关键词 文章不存在
4. 点击查询
5. 叠加 WARN 级别筛选
```

预期结果：

```text
只展示包含关键词的日志
中文关键词可以匹配业务异常日志
关键词和日志级别可以组合筛选
```

### 5.4 慢请求列表

测试接口：

```text
GET /admin/log/slow-requests?threshold=20&limit=10
```

测试步骤：

```text
1. 将慢请求阈值调低到 20ms
2. 刷新运行日志页面
3. 查看慢请求列表
4. 再将阈值调高到 100ms
```

预期结果：

```text
阈值降低后慢请求数量增加
阈值升高后慢请求数量减少
慢请求按耗时从高到低展示
```

### 5.5 接口访问排行

测试接口：

```text
GET /admin/log/top-apis?limit=10
```

测试步骤：

```text
1. 多次访问 /api/article/page
2. 多次访问 /api/article/{id}
3. 回到运行日志页面刷新
4. 查看接口访问排行
```

预期结果：

```text
访问次数高的接口排在前面
接口路径会去掉 query string 后聚合
展示 requestCount、averageCostTime、maxCostTime
```

## 六、端到端联动测试

测试步骤：

```text
1. 登录后台
2. 上传文章封面
3. 上传正文图片
4. 保存草稿
5. 发布文章
6. 前台访问文章详情
7. 后台查看操作日志
8. 后台查看运行日志
```

预期结果：

```text
文章发布链路成功
前台能看到封面和正文图片
操作日志记录后台上传、保存、发布行为
运行日志记录后台接口和前台公开接口访问
接口访问排行中出现 /admin/article、/admin/file、/api/article
```

## 七、异常与边界测试

```text
1. 删除或重命名 logs/kyonelife-blog.log 后访问运行日志页面，页面不应崩溃，应返回空统计或空列表
2. 日志中出现无法解析的行时，列表仍展示 raw/message
3. keyword 为空时，搜索接口退化为最近日志查询
4. level 为空时，不过滤日志级别
5. 未登录访问 /admin/log/* 应被 Sa-Token 拦截
```

## 八、通过标准

本阶段通过标准：

```text
MinIO 上传链路可用
操作日志能审计后台关键操作
运行日志页面能分析请求、异常、慢接口和接口排行
三个模块能串成一条完整演示链路
前后端构建通过
```

## 九、行为埋点系统测试

### 9.1 数据表初始化

测试前确认已执行：

```text
blog/sql/event_log.sql
```

检查表结构：

```sql
SHOW CREATE TABLE event_log;
```

预期结果：

```text
event_log 表存在
包含 event_type、visitor_id、article_id、tag_id、collection_id、page_url、referrer、ip、user_agent、device、browser、os、duration、create_time 等字段
包含 event_type、visitor_id + create_time、article_id + create_time、create_time 索引
```

### 9.2 前台 page_view 自动上报

测试接口：

```text
POST /api/event/report
```

测试步骤：

```text
1. 打开前台首页
2. 打开浏览器 DevTools -> Application -> Local Storage
3. 检查是否生成 kyonelife_visitor_id
4. 切换到 /tags、/collections、/archives、/about
5. 查询 event_log 表
```

查询 SQL：

```sql
SELECT event_type, visitor_id, page_url, referrer, ip, browser, os, create_time
FROM event_log
ORDER BY id DESC
LIMIT 20;
```

预期结果：

```text
每次路由切换产生 page_view 事件
同一个浏览器 visitor_id 保持一致
page_url 记录当前前台路径
referrer 记录上一个前台路径或 document.referrer
ip、user_agent、device、browser、os 能由后端补齐
```

### 9.3 文章浏览 article_view 上报

测试步骤：

```text
1. 前台首页点击一篇已发布文章
2. 等待文章详情加载成功
3. 查询 event_log 表
```

查询 SQL：

```sql
SELECT event_type, visitor_id, article_id, page_url, create_time
FROM event_log
WHERE event_type = 'article_view'
ORDER BY id DESC
LIMIT 10;
```

预期结果：

```text
产生 article_view 事件
article_id 等于当前文章 ID
文章不存在或详情加载失败时不应产生 article_view
```

### 9.4 标签和合集点击事件

测试步骤：

```text
1. 进入 /tags
2. 点击任意标签
3. 进入 /collections
4. 点击任意合集
5. 查询 event_log 表
```

查询 SQL：

```sql
SELECT event_type, tag_id, collection_id, page_url, create_time
FROM event_log
WHERE event_type IN ('tag_click', 'collection_click')
ORDER BY id DESC
LIMIT 20;
```

预期结果：

```text
点击标签产生 tag_click，tag_id 有值
点击合集产生 collection_click，collection_id 有值
点击后仍会正常跳转
埋点上报失败不影响前台页面跳转
```

### 9.5 手动调用上报接口

测试请求：

```http
POST http://localhost:8080/api/event/report
Content-Type: application/json

{
  "eventType": "page_view",
  "visitorId": "test_visitor_001",
  "pageUrl": "/manual-test",
  "referrer": "/",
  "duration": 0
}
```

预期结果：

```text
接口返回 code = 200
event_log 表中异步写入一条 page_view
该接口不需要后台登录 Token
```

### 9.6 非法上报参数

测试请求：

```json
{
  "eventType": "",
  "visitorId": ""
}
```

预期结果：

```text
参数校验失败
不会写入 event_log
```

## 十、数据分析看板测试

### 10.1 访问数据概览

测试接口：

```text
GET /admin/analytics/overview
```

后台页面：

```text
系统管理 -> 数据看板
```

测试步骤：

```text
1. 使用同一个浏览器访问前台多个页面
2. 使用另一个浏览器或清空 localStorage 后访问前台
3. 登录后台
4. 打开 数据看板 页面
```

预期结果：

```text
今日 PV = 当天 page_view 事件数量
今日 UV = 当天 page_view 的 visitor_id 去重数量
累计 PV = 所有 page_view 事件数量
文章浏览 = 所有 article_view 事件数量
```

辅助 SQL：

```sql
SELECT COUNT(*) FROM event_log WHERE event_type = 'page_view' AND DATE(create_time) = CURDATE();
SELECT COUNT(DISTINCT visitor_id) FROM event_log WHERE event_type = 'page_view' AND DATE(create_time) = CURDATE();
SELECT COUNT(*) FROM event_log WHERE event_type = 'article_view';
```

### 10.2 最近 7 天 PV/UV 趋势

测试接口：

```text
GET /admin/analytics/trend?days=7
```

测试步骤：

```text
1. 打开数据看板
2. 查看最近 7 天趋势柱状图
3. 刷新页面
4. 对比接口返回数据和页面展示
```

预期结果：

```text
接口固定返回 7 天数据
没有访问数据的日期 PV/UV 为 0
有访问数据的日期展示正确 PV/UV
页面柱状图不因 0 数据报错
```

### 10.3 热门文章 Top 10

测试接口：

```text
GET /admin/analytics/hot-articles?days=7&limit=10
```

测试步骤：

```text
1. 多次访问前台文章 A
2. 少量访问前台文章 B
3. 打开后台数据看板
4. 查看热门文章排行
```

预期结果：

```text
文章 A 排名高于文章 B
列表展示文章标题和浏览次数
只统计 article_view 事件
文章标题从 article 表关联查询
```

辅助 SQL：

```sql
SELECT article_id, COUNT(*) AS cnt
FROM event_log
WHERE event_type = 'article_view'
GROUP BY article_id
ORDER BY cnt DESC;
```

### 10.4 管理端鉴权

测试步骤：

```text
1. 清空后台 Token
2. 直接请求 /admin/analytics/overview
3. 再登录后台后访问数据看板
```

预期结果：

```text
未登录访问后台统计接口会被 Sa-Token 拦截
登录后可以正常访问
/api/event/report 前台上报接口不需要登录
```

## 十一、埋点到看板端到端测试

测试步骤：

```text
1. 清空 event_log 测试数据，或记录当前最大 id
2. 打开前台首页
3. 进入文章详情页
4. 进入标签页并点击标签
5. 进入合集页并点击合集
6. 登录后台打开数据看板
7. 查询 event_log 表核对明细
```

预期结果：

```text
event_log 至少包含 page_view、article_view、tag_click、collection_click
数据看板今日 PV 增加
访问同一浏览器时 UV 不重复增加
换浏览器或清空 visitorId 后 UV 增加
热门文章能根据 article_view 更新
```

## 十二、当前埋点 MVP 的边界

第一版暂不验证以下能力：

```text
Redis 实时计数
MQ 削峰
ClickHouse 明细分析
定时聚合表
批量上报
离线缓存和失败补发
阅读时长精确统计
防刷策略
```

这些能力作为后续优化项，不影响当前 MVP 闭环验收。
