# kyonelife Agent 内容创作中心设计

## 一、设计目标

`kyonelife` 的 Agent 能力不应该做成孤立 Demo，而应该接入内容平台的生产链路。

核心目标：

```text
Agent 生成结构化内容
-> 系统转换为 Markdown 草稿
-> 保存到文章后台
-> 作者人工编辑确认
-> 发布到前台展示
-> 通过埋点和数据看板分析内容效果
```

这样 Agent 就不是单独的工具页，而是内容平台的创作辅助能力。

## 二、为什么比单独 Agent 项目更好

单独做旅游规划 Agent，容易被理解为：

```text
输入需求 -> 调模型 -> 输出旅行计划
```

更像一个 AI Demo。

接入 `kyonelife` 后，能体现：

```text
Agent 输出标准化
JSON 转 Markdown
内容草稿入库
后台人工审核
前台文章展示
埋点数据反馈
```

面试表达：

```text
我没有把 Agent 做成孤立页面，而是将它接入内容平台的生产链路。Agent 负责生成结构化内容，后端负责将 JSON 标准化为 Markdown，并保存为文章草稿，作者编辑确认后发布到前台。
```

## 三、模块命名

建议统一命名为：

```text
创作助手中心
```

下面可以挂多个 Agent：

```text
旅行灵感助手
技术文章助手
内容改写助手
面试复盘助手
读书笔记助手
周报助手
代码复盘助手
```

不要在项目里表现成很多杂乱 Agent，而是统一成：

```text
Agent 驱动的内容创作中心
```

## 四、推荐优先做的 Agent

### 1. 旅行灵感助手

适合生活内容和旅行攻略。

输入：

```text
目的地
出行天数
出行时间
预算
人数
偏好：美食 / 风景 / 历史 / 轻松 / 特种兵
避免项：不想爬山 / 不想太赶 / 不吃辣
```

输出：

```text
旅行攻略 Markdown 草稿
每日路线
预算估算
住宿区域建议
避坑提醒
```

### 2. 技术文章助手

适合技术博客和秋招展示。

输入：

```text
技术主题
知识点
代码片段
学习笔记
Bug 描述
```

输出：

```text
技术文章 Markdown 草稿
标题
摘要
目录
标签建议
代码说明
```

### 3. 内容改写助手

最容易接入现有后台文章编辑流程。

输入：

```text
已有文章标题
已有 Markdown 正文
期望风格
```

输出：

```text
优化后的标题
摘要
标签
SEO 描述
文章结构建议
```

### 4. 面试复盘助手

适合秋招阶段持续产出内容。

输入：

```text
面试公司
岗位
面试问题
回答情况
不会的知识点
```

输出：

```text
面试复盘 Markdown 草稿
问题分类
知识补强计划
复习清单
```

## 五、统一处理流程

所有 Agent 尽量统一成同一条链路：

```text
表单输入
-> Agent 生成结构化 JSON
-> 后端校验 JSON
-> JSON 转 Markdown
-> 生成标题、摘要、标签
-> 保存为文章草稿
-> 后台编辑确认
-> 发布到前台
```

统一输出结构：

```json
{
  "title": "成都 3 日轻松美食旅行攻略",
  "summary": "一份适合初次到成都、偏轻松节奏的 3 日行程规划。",
  "contentMd": "# 成都 3 日轻松美食旅行攻略\n\n...",
  "tags": ["旅行", "成都", "攻略"],
  "collectionName": "旅行灵感"
}
```

## 六、旅行灵感助手示例

### Agent 返回 JSON 示例

```json
{
  "title": "成都 3 日轻松美食旅行攻略",
  "summary": "一份适合初次到成都、偏轻松节奏的 3 日行程规划。",
  "destination": "成都",
  "days": 3,
  "itinerary": [
    {
      "day": 1,
      "theme": "市区慢逛与小吃",
      "plans": [
        {
          "time": "上午",
          "activity": "人民公园",
          "reason": "适合体验成都慢生活"
        },
        {
          "time": "下午",
          "activity": "宽窄巷子",
          "reason": "适合拍照和小吃"
        }
      ]
    }
  ],
  "budget": {
    "total": 2000,
    "items": [
      { "name": "住宿", "amount": 800 },
      { "name": "餐饮", "amount": 500 }
    ]
  },
  "tips": [
    "避开周末高峰",
    "火锅建议提前排队"
  ]
}
```

### 转成 Markdown 示例

```md
# 成都 3 日轻松美食旅行攻略

> 一份适合初次到成都、偏轻松节奏的 3 日行程规划。

## 基本信息

- 目的地：成都
- 天数：3 天
- 预算：约 2000 元

## Day 1：市区慢逛与小吃

### 上午：人民公园

推荐理由：适合体验成都慢生活。

### 下午：宽窄巷子

推荐理由：适合拍照和小吃。

## 预算估算

| 项目 | 预算 |
|---|---:|
| 住宿 | 800 元 |
| 餐饮 | 500 元 |

## 出行建议

- 避开周末高峰
- 火锅建议提前排队
```

## 七、接口设计

可以先设计为后台接口：

```text
POST /admin/agent/travel/generate
POST /admin/agent/content/rewrite
POST /admin/agent/article/draft
```

也可以统一成：

```text
POST /admin/agent/generate-draft
```

请求示例：

```json
{
  "agentType": "travel",
  "input": {
    "destination": "成都",
    "days": 3,
    "budget": 2000,
    "preference": ["美食", "轻松"]
  }
}
```

返回示例：

```json
{
  "title": "成都 3 日轻松美食旅行攻略",
  "summary": "一份适合初次到成都、偏轻松节奏的 3 日行程规划。",
  "contentMd": "# 成都 3 日轻松美食旅行攻略\n\n...",
  "tags": ["旅行", "成都", "攻略"],
  "collectionId": 12
}
```

保存草稿接口可以复用文章接口：

```text
POST /admin/article
```

只需要将：

```text
article.status = 草稿
article.content_md = 生成 Markdown
```

## 八、是否需要 RAG

第一版不需要直接上 RAG。

判断标准：

```text
生成通用内容：不需要 RAG
基于已有资料生成内容：需要 RAG
基于实时信息生成内容：需要搜索工具
基于平台数据分析：需要数据库查询工具
```

### 不需要 RAG 的 Agent

```text
旅行灵感助手
技术文章大纲助手
周报助手
面试复盘助手
普通内容改写助手
```

这些可以先用：

```text
表单输入 + Prompt + 结构化输出
```

### 适合 RAG 的 Agent

```text
论文阅读助手
个人博客问答助手
已有文章知识库问答
基于历史文章生成新文章
项目文档问答助手
```

如果用户需求是：

```text
基于我已有的文章，帮我生成一篇 Redis 总结
```

这时适合 RAG，因为 Agent 需要从历史文章中检索相关内容。

## 九、Agent 能力分层

### 第一层：Prompt Agent

最容易落地。

```text
输入表单
-> 调模型
-> 生成结构化 JSON
-> 转 Markdown
-> 保存草稿
```

适合：

```text
旅行灵感助手
技术文章助手
面试复盘助手
周报助手
```

### 第二层：工具型 Agent

让 Agent 调用平台已有能力。

可用工具：

```text
查询文章列表
查询文章详情
查询标签列表
查询合集列表
查询热门文章
查询搜索关键词
查询埋点统计
保存文章草稿
```

适合：

```text
内容选题助手
运营分析助手
文章优化助手
```

示例：

```text
根据最近 7 天热门文章和搜索关键词，帮我生成 5 个下一篇文章选题。
```

### 第三层：RAG Agent

最后再做。

```text
已有文章向量化
用户问题向量检索
召回相关文章片段
拼接 Prompt
生成回答或草稿
```

适合：

```text
博客知识库问答
基于历史文章生成新文章
论文阅读助手
```

## 十、Tool Map 设计

如果 Agent 需要调用很多工具，就需要工具映射。

简单理解：

```text
Agent 决定调用哪个工具
-> 根据 tool_name 从 TOOL_MAP 找到函数
-> 执行函数
-> 将工具结果交给 Agent 总结
```

示例：

```python
TOOL_MAP = {
    "get_hot_articles": get_hot_articles,
    "get_search_keywords": get_search_keywords,
    "get_tag_rank": get_tag_rank,
    "get_collection_rank": get_collection_rank,
    "get_article_detail": get_article_detail,
    "get_article_list": get_article_list,
    "save_article_draft": save_article_draft,
}
```

Agent 工具调用结果示例：

```json
{
  "tool": "get_hot_articles",
  "args": {
    "days": 7,
    "limit": 10
  }
}
```

程序执行：

```python
tool_func = TOOL_MAP[result["tool"]]
tool_result = tool_func(**result["args"])
```

## 十一、是否一开始就需要 Tool Map

不需要。

建议分阶段：

```text
第一阶段：不用 Tool Map
旅行灵感助手只根据表单输入生成 Markdown 草稿

第二阶段：固定工具调用
后端先查热门文章、搜索关键词，再把结果塞给模型

第三阶段：真正 Tool Map
Agent 自己根据用户目标决定调用哪些工具
```

这样实现更稳，面试时也更容易讲清楚演进路线。

## 十二、开发优先级

推荐顺序：

```text
1. 旅行灵感助手 Prompt 版
2. JSON 转 Markdown
3. 保存为文章草稿
4. 后台预览和编辑确认
5. 技术文章助手
6. 内容改写助手
7. 固定工具调用型内容选题助手
8. Tool Map
9. RAG 文章库问答
```

第一阶段只要完成：

```text
旅行灵感助手 -> Markdown 草稿 -> 保存文章
```

就已经能体现 Agent 和内容平台的结合。

## 十三、面试表达

可以这样说：

```text
我没有把 Agent 做成一个孤立的 AI Demo，而是把它设计成内容平台的创作助手中心。不同 Agent 负责生成不同类型的内容，比如旅行攻略、技术文章、面试复盘等。Agent 输出结构化 JSON 后，系统会将其转换成 Markdown 草稿，并接入后台文章发布流程，由作者编辑确认后发布到前台。

在实现上，我将 Agent 能力按复杂度分层。第一阶段是 Prompt Agent，用于生成结构化内容；第二阶段是工具型 Agent，可以调用平台里的热门文章、搜索关键词和标签排行等数据接口；第三阶段再扩展 RAG，让 Agent 基于已有文章库进行问答和内容生成。
```

## 十四、最终定位

Agent 模块最终定位为：

```text
面向内容平台的 AI 创作助手中心
```

它服务于：

```text
内容生成
内容改写
内容选题
内容草稿保存
前台展示
数据反馈
```

最终形成闭环：

```text
AI Agent 生成内容
-> Markdown 草稿
-> 人工确认
-> 前台发布
-> 埋点分析
-> 数据反馈给后续选题
```
