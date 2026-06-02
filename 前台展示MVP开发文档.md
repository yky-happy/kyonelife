# kyonelife 前台展示 MVP 开发文档

## 一、阶段目标

当前后台文章管理已经进入收尾测试阶段。后台完成“写文章、编辑、发布、下架、删除”后，下一步应进入前台展示 MVP。

本阶段目标是跑通个人博客的最小闭环：

```text
后台写文章 -> 发布文章 -> 前台首页看到文章 -> 点击进入文章详情
```

完成这个闭环后，项目才真正具备博客系统的核心价值。

## 二、开发范围

### 本阶段要做

1. 前台首页文章列表
2. 文章详情页
3. 标签筛选文章
4. 合集列表
5. 合集详情页
6. 归档页
7. 基础响应式适配

### 本阶段暂不做

1. 评论系统
2. 用户登录
3. 图片上传
4. AI 摘要/标签推荐
5. 全文搜索
6. RAG 问答
7. 阅读量 Redis 异步统计
8. 复杂动效和主题切换

这些功能都依赖文章前台展示链路，建议等 MVP 跑通后再迭代。

## 三、推荐开发顺序

### 第 1 步：后端开放前台文章接口

后台接口是 `/admin/article`，前台不应直接复用后台接口。

建议新增前台接口路径：

```text
/web/article/page
/web/article/{id}
/web/tag/list
/web/collection/list
/web/collection/{id}/articles
/web/archive
```

前台接口只返回已发布文章：

```text
status = 1
```

不要返回草稿和已下架文章。

### 第 2 步：新增前台 VO

建议新增前台专用 VO，避免直接复用后台详情对象。

文章列表 VO：

```text
ArticleCardVO
```

建议字段：

| 字段 | 说明 |
| --- | --- |
| id | 文章 ID |
| title | 标题 |
| cover | 封面 |
| summary | 摘要 |
| collectionId | 合集 ID |
| collectionName | 合集名称 |
| tags | 标签列表 |
| isStick | 是否置顶 |
| viewCount | 阅读量 |
| createTime | 发布时间 |

文章详情 VO：

```text
ArticleWebDetailVO
```

建议字段：

| 字段 | 说明 |
| --- | --- |
| id | 文章 ID |
| title | 标题 |
| cover | 封面 |
| summary | 摘要 |
| content | HTML 内容 |
| contentMd | Markdown 原文，前台可暂不返回 |
| collectionName | 合集名称 |
| tags | 标签列表 |
| keywords | SEO 关键词 |
| viewCount | 阅读量 |
| createTime | 发布时间 |

MVP 阶段可以先返回 `contentMd`，前端用 Markdown 渲染；后续再优化成后端保存 HTML、前端直接渲染 HTML。

### 第 3 步：前台项目接入路由和请求封装

当前前台项目：

```text
blog-web
```

建议先补齐：

```text
blog-web/src/router/index.ts
blog-web/src/utils/request.ts
blog-web/src/api/article.ts
blog-web/src/api/tag.ts
blog-web/src/api/collection.ts
```

前台页面路由建议：

| 路径 | 页面 |
| --- | --- |
| `/` | 首页文章列表 |
| `/article/:id` | 文章详情 |
| `/tags` | 标签页 |
| `/tag/:id` | 标签文章列表 |
| `/collections` | 合集页 |
| `/collection/:id` | 合集详情 |
| `/archives` | 归档页 |
| `/about` | 关于我 |

### 第 4 步：首页文章列表

首页先做最小可用：

1. 查询已发布文章
2. 置顶文章排在前面
3. 展示封面、标题、摘要、标签、合集、发布时间
4. 点击文章进入详情页
5. 分页加载

接口建议：

```text
GET /web/article/page?page=1&size=10
```

排序建议：

```text
is_stick DESC, create_time DESC
```

### 第 5 步：文章详情页

详情页先保证阅读体验：

1. 标题
2. 发布时间
3. 标签
4. 合集
5. 封面
6. 正文渲染

正文渲染建议：

- MVP：前端用 Markdown 渲染 `contentMd`
- 后续：后端保存 HTML，前端直接展示 `content`

如果使用 Markdown 渲染，前端可选择：

```text
md-editor-v3 preview-only
```

或者使用更轻量的 Markdown 渲染库。

### 第 6 步：标签筛选文章

标签功能先做简单版本：

1. 标签列表页展示所有标签
2. 点击标签进入文章列表
3. 只展示已发布文章

接口建议：

```text
GET /web/tag/list
GET /web/article/page?tagId=1&page=1&size=10
```

### 第 7 步：合集列表和合集详情

合集用于组织系列文章。

合集列表展示：

1. 合集名称
2. 封面
3. 简介
4. 文章数量

合集详情展示：

1. 合集信息
2. 合集下文章列表

接口建议：

```text
GET /web/collection/list
GET /web/collection/{id}/articles?page=1&size=10
```

### 第 8 步：归档页

归档页按年月展示文章。

可以先由后端返回分组结果：

```text
2026-05
  - 文章 A
  - 文章 B
2026-04
  - 文章 C
```

接口建议：

```text
GET /web/archive
```

MVP 阶段也可以前端拿全部已发布文章后按年月分组，但文章多了以后建议后端分组。

## 四、后端任务清单

- [ ] 新增 `web` 包或前台 controller 包
- [ ] 新增前台文章分页接口
- [ ] 新增前台文章详情接口
- [ ] 新增标签列表接口
- [ ] 新增按标签筛选文章接口
- [ ] 新增合集列表接口
- [ ] 新增合集详情文章接口
- [ ] 新增归档接口
- [ ] 所有前台文章接口只返回 `status = 1`
- [ ] 前台详情接口过滤不存在、草稿、下架文章

## 五、前端任务清单

- [ ] 初始化 `blog-web` 路由
- [ ] 新增请求封装
- [ ] 新增文章 API
- [ ] 新增标签 API
- [ ] 新增合集 API
- [ ] 改造首页文章列表
- [ ] 新增文章详情页
- [ ] 新增标签页和标签文章列表
- [ ] 新增合集页和合集详情页
- [ ] 新增归档页
- [ ] 做基础移动端适配

## 六、验收标准

完成本阶段后，应能完整走通：

1. 后台登录
2. 后台写一篇文章
3. 发布文章
4. 前台首页看到文章
5. 点击进入文章详情
6. 文章标签可点击筛选
7. 文章合集可点击查看
8. 归档页能按年月看到文章
9. 草稿和下架文章不会出现在前台

## 七、注意事项

### 1. 前台接口不要暴露后台数据

前台只展示公开内容，不应返回：

- 草稿文章
- 已下架文章
- 管理字段
- 后台操作状态

### 2. 先保证链路，不要过早做复杂设计

前台展示阶段优先完成核心阅读链路。

评论、搜索、AI、阅读量统计都可以后置。

### 3. 文章详情正文渲染要统一

当前后台保存 `contentMd` 和 `content`。MVP 阶段可以先让二者内容一致，但后续需要明确：

- `contentMd`：编辑用 Markdown 原文
- `content`：前台展示用 HTML

### 4. 前台页面不要依赖后台登录

前台展示应该是公开访问，不依赖管理员 token。

如果当前请求封装默认带 token，也要确保前台请求不要求登录。

## 八、完成后的下一阶段

前台展示 MVP 完成后，再进入：

```text
评论系统 -> 轮播/网站配置 -> 图片上传 -> AI 摘要/标签推荐 -> 搜索/缓存/统计
```

其中评论系统建议作为前台展示后的第一个扩展功能，因为它能让博客从“内容展示”进入“用户互动”。
