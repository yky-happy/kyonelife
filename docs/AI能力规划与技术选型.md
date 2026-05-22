# AI / LLM / Agent 能力规划与技术选型

> 时间：2026-05-22
> 范围：博客项目未来集成 AI 能力的完整技术路线图、选型权衡与"现在该提前留的坑"
> 一句话回答：**核心用 LangChain4j 就够，但视场景需要搭配向量库、异步队列等组件。**

---

## 一、核心问题：LangChain4j 是不是"加一个依赖就够"

**短答**：是核心库，但**不是只加它就够**。它解决"如何调 LLM、如何编排 Prompt、如何让 LLM 用工具"，但不解决"用户数据存哪里、长任务怎么调度、API 成本怎么控制"。

可以这样理解：

```
LangChain4j  ≈  Spring Web
（处理请求/编排）
   +
向量数据库   ≈  MySQL
（数据持久化层）
   +
消息队列     ≈  Redis（异步缓冲）
   +
监控告警     ≈  Logback / Prometheus
```

LangChain4j 在 AI 技术栈里的定位，类似 Spring Web 在传统后端里的定位——它是中枢，但不是全部。

---

## 二、LangChain4j 能直接覆盖的场景

下面这些场景，**只加 `langchain4j-open-ai-spring-boot-starter`（或类似 starter）即可**，不需要额外基础设施。

| 场景 | 实现方式 | 工作量 |
|------|----------|--------|
| 文章发布时自动生成摘要 | 1 次 ChatLanguageModel 调用 | 半天 |
| 文章自动推荐标签（从已有标签集中选） | 1 次调用，Prompt 里塞标签列表让 LLM 选 | 半天 |
| 评论自动审核（识别垃圾/脏话） | 1 次调用，输出 JSON 评分 | 半天 |
| 文章润色/扩写助手 | 流式调用，前端实现打字机效果 | 1 天 |
| 简单 ChatBot | `AiServices` + `ChatMemory` | 1 天 |
| 让 LLM 调用你的接口（基础 Agent） | `@Tool` 注解 | 1~2 天 |

**核心 API 速览**：

```java
// 1. 最简单的同步调用
ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName("gpt-4o-mini")
        .build();
String summary = model.generate("用 100 字总结这篇文章：" + content);

// 2. 接口式（推荐，更 Java）
interface Assistant {
    @SystemMessage("你是博客文章摘要助手，输出不超过 150 字")
    String summarize(String content);
}
Assistant assistant = AiServices.create(Assistant.class, model);
String result = assistant.summarize(article);

// 3. 工具调用（Agent 基础）
class BlogTools {
    @Tool("获取最近一个月发表的文章数量")
    public int countRecentArticles() {
        return articleService.countLastMonth();
    }
}
Assistant agent = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .tools(new BlogTools())
        .build();
```

---

## 三、需要"额外搭配"的场景

### 3.1 RAG（基于文章库的智能问答 / 语义搜索）

> 典型场景：用户在博客首页输入"Spring Boot 启动慢怎么办"，系统从你写过的所有文章里**按语义相关度**找出 Top 5，喂给 LLM 生成针对性回答。

**额外需要的组件**：

| 组件 | 作用 | 推荐方案 |
|------|------|---------|
| **向量数据库** | 存储文章的向量表示，支持近似最近邻搜索 | **pgvector**（PostgreSQL 插件） |
| **Embedding 模型** | 把文本转成向量 | OpenAI `text-embedding-3-small`（便宜）/ 本地 `all-MiniLM-L6-v2`（免费） |
| **文档切分器** | 长文章按段切分再向量化 | LangChain4j 自带 `DocumentSplitter` |

**向量数据库选型对比**：

| 选项 | 优点 | 缺点 | 适合 |
|------|------|------|------|
| **pgvector** | 不用新起服务，PostgreSQL 一并搞定；SQL 同时查关系数据 + 向量 | 性能上限不如专门的向量库（千万级以下完全够用） | **个人博客首选** |
| Milvus | 性能最强，分布式 | 重，运维复杂 | 数据量 1 亿+ 才用 |
| Qdrant | 性能/易用平衡 | 多起一个服务 | 中等规模 |
| Chroma | 极简，嵌入式 | 生产能力一般 | 原型快速验证 |
| Redis Stack | 复用你现有的 Redis | 向量索引相对弱 | 数据量小 |

**预估工作量**：1~2 天（含数据库迁移 + Embedding pipeline + 检索 + 整合到搜索接口）

### 3.2 复杂多步骤 Agent

> 典型场景：用户说"帮我把上个月所有关于 Spring 的文章总结成一篇月度回顾"
> Agent 需要：① 查找文章 → ② 读取每篇内容 → ③ 综合 → ④ 输出

LangChain4j 的 `@Tool` 能做单轮工具调用，但**多轮自主决策**需要额外考虑：

| 关注点 | 怎么处理 |
|--------|---------|
| **超时控制** | 单次工具调用 + 单次 LLM 调用都设超时（10~30s）|
| **死循环防护** | 限制最大步数（如 10 步），超出强制停止 |
| **失败重试** | Resilience4j 或 Spring Retry |
| **链路追踪** | 每一步推理写日志：用了什么工具、传入参数、返回值、LLM 思考过程 |
| **成本控制** | 限制每次任务最大 token 消耗 |

### 3.3 异步 / 排队（生产必备）

LLM 调用响应时间 **3~30 秒**，远远超过 HTTP 请求的合理等待时间。生产上必须异步化。

**轻量方案（个人博客够用）**：
```
HTTP 请求 → 写入 Redis 队列（key = "ai:task:queue"） → 立即返回 taskId
              ↓
         后台 worker（Spring @Async）轮询 → 调 LLM → 把结果存回 Redis
              ↓
         前端轮询 GET /ai/task/{taskId} 或走 SSE 推送
```

**重型方案（流量大才上）**：RabbitMQ / RocketMQ + 独立 worker 进程

### 3.4 流式输出（提升体验）

> 典型场景：AI 写作助手，让用户看到一个字一个字蹦出来（打字机效果）

技术栈：
- 后端：LangChain4j 的 `StreamingChatLanguageModel`
- 前端：**SSE (Server-Sent Events)**，而不是 WebSocket（SSE 单向更简单）

axios 默认不好处理 SSE，可以用原生 `EventSource` 或 `@microsoft/fetch-event-source` 库。

### 3.5 成本控制 + 监控（防火墙级别重要）

LLM 按 token 计费，**真有人滥用一晚上能烧掉几百块**。必备措施：

| 项目 | 做法 |
|------|------|
| **用户级速率限制** | Sa-Token + Redis 计数：每用户每天 ≤ 100 次 AI 调用 |
| **Token 计数** | 调用前预估 token，超出预算拒绝；调用后记录实际消耗 |
| **成本统计** | 按日/周/月聚合每个用户的开销，写到数据库 |
| **告警** | 单小时调用超过阈值 → 钉钉/邮件通知 |
| **降级** | 余额预警时自动切到免费/本地模型 |
| **缓存** | 相同 prompt 缓存 24 小时，命中直接返回 |

---

## 四、技术选型：LangChain4j vs Spring AI

Spring 官方 2024 年推出了 **Spring AI**，定位与 LangChain4j 重叠：

| 维度 | LangChain4j | Spring AI |
|------|-------------|-----------|
| 起源 | 第三方移植自 LangChain (Python) | Spring 官方 |
| Spring Boot 集成度 | 有 starter，风格相对"独立" | 极度 Spring 风格（`@Bean` 配置友好） |
| API 风格 | 偏 Python LangChain | 偏 Spring 传统 |
| 文档质量 | 中等，但社区文章多 | 优秀且持续更新 |
| 模型覆盖 | 广（含很多小众模型） | OpenAI / Anthropic / Ollama / Azure 等主流 |
| 工具调用 | `@Tool` 注解，简洁 | Function Calling API，较 verbose |
| RAG 支持 | 成熟，VectorStore 抽象完善 | 成熟，且与 Spring Data 整合好 |
| 流式支持 | ✅ | ✅ |
| 社区活跃度 | 高 | 高，且 Spring 官方加持 |
| 学习资源 | Python LangChain 的概念可直接迁移 | 需要单独学习 Spring AI 概念 |

### 给本项目的建议

**主选：LangChain4j**

理由：
1. 你将来若学 Python LangChain（行业事实标准），概念一一对应
2. 生态更广，遇到问题搜中文/英文都好找
3. Anthropic Claude、OpenAI、本地 Ollama 都支持，灵活性高
4. 跟现有 Spring Boot 项目兼容良好（有官方 starter）

**何时考虑 Spring AI**：
- 主要用 OpenAI/Azure OpenAI 一家
- 团队对 Python 生态不熟，纯 Java 背景
- 想要"最干净的 Spring 风格"代码

---

## 五、给本项目的分步演进路线图

不要一次性把所有 AI 能力堆上。按这个顺序逐步演进，每一步都能独立上线且有价值。

### 阶段 A1：单点功能（建议在阶段 4 文章 CRUD 完成后启动）

| 功能 | 依赖 | 工作量 |
|------|------|--------|
| 文章发布时自动生成摘要 | `langchain4j-open-ai-spring-boot-starter` | 半天 |
| 文章发布时推荐标签 | 同上（复用 client） | 半天 |
| 评论自动审核 | 同上 | 2 小时 |

**这一阶段不需要任何额外基础设施**，加个 API Key 就开干。

### 阶段 A2：流式写作助手

| 功能 | 依赖 | 工作量 |
|------|------|--------|
| Markdown 编辑器旁的"AI 助手"侧边栏 | 流式 SSE + Vue 组件 | 1~2 天 |
| 支持"润色 / 扩写 / 翻译 / 写大纲" | Prompt 模板管理 | 0.5 天 |

### 阶段 A3：RAG 语义搜索

| 功能 | 依赖 | 工作量 |
|------|------|--------|
| 文章入库时计算 embedding 存到 pgvector | `langchain4j-embeddings-*` + pgvector | 1 天 |
| 前台搜索框语义搜索（替换 LIKE 模糊匹配） | LangChain4j Retriever | 0.5 天 |
| "智能问答"页面：用户问问题，从所有文章里找答案 | RAG 完整 pipeline | 1 天 |

⚠️ **阶段 A3 需要把 MySQL 换成 PostgreSQL**（或加一个单独的 PG 实例只放向量）。

### 阶段 A4：Agent

| 功能 | 依赖 | 工作量 |
|------|------|--------|
| 后台"AI 助手"：你问它"上个月写了几篇 Spring 文章"它能查数据库回答 | `@Tool` + AiServices | 1 天 |
| 自动化任务：每周一发布一篇"上周 AI 行业摘要" | Agent + 网络搜索工具 + `@Scheduled` | 2~3 天 |

### 阶段 A5：生产化打磨

| 任务 | 工作量 |
|------|--------|
| 速率限制 + 成本监控 | 1 天 |
| 调用日志 + 链路追踪 | 1 天 |
| 异步队列改造 | 1 天 |
| 错误降级 + 备用模型切换 | 0.5 天 |

---

## 六、现在阶段就该提前留的"坑位"

如果你**确定**未来要加 AI 能力，**现在阶段 3 / 4 写代码时可以提前留好这些位置**，未来加功能时不用大改：

### 6.1 数据库设计层面

**文章表加字段**：
```sql
ALTER TABLE article ADD COLUMN summary TEXT COMMENT 'AI 生成的摘要';
ALTER TABLE article ADD COLUMN summary_status TINYINT DEFAULT 0 COMMENT '0=未生成 1=生成中 2=已生成 3=失败';
ALTER TABLE article ADD COLUMN ai_tags VARCHAR(255) COMMENT 'AI 推荐的标签 ID（逗号分隔）';
```

**评论表加字段**：
```sql
ALTER TABLE comment ADD COLUMN ai_review_score TINYINT COMMENT 'AI 审核分（0-100，越高越疑似垃圾）';
ALTER TABLE comment ADD COLUMN ai_review_reason VARCHAR(255) COMMENT 'AI 审核理由';
```

**新增 AI 任务表**（异步队列用）：
```sql
CREATE TABLE ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(50) NOT NULL COMMENT 'SUMMARIZE/REVIEW/TAG_RECOMMEND/...',
    biz_id BIGINT COMMENT '关联业务 ID（如文章 ID）',
    input TEXT,
    output TEXT,
    status TINYINT DEFAULT 0 COMMENT '0=待执行 1=执行中 2=成功 3=失败',
    error_msg VARCHAR(500),
    tokens_used INT COMMENT '消耗的 token 数',
    cost DECIMAL(10,6) COMMENT '成本（美元）',
    create_time DATETIME,
    update_time DATETIME
);
```

### 6.2 数据库选型决策

**如果未来打算做 RAG**：**现在就用 PostgreSQL 替代 MySQL**。

| 维度 | MySQL | PostgreSQL |
|------|-------|-----------|
| 业务表存储 | ✅ | ✅ |
| pgvector 向量检索 | ❌ | ✅（同库内即可，省一个服务）|
| JSON 类型支持 | 一般 | 优秀（jsonb 索引）|
| 全文搜索 | 一般 | 内建强大（tsvector）|
| 你已有数据 | 14 张表已建好 | 需迁移 |

**当前阶段建议**：
- 如果你**100% 确定要做 RAG** → 现在就切 PG（早切早省事）
- 如果只是"以后可能加"，留 MySQL 也行，未来要么加 pgvector 独立实例，要么用 Qdrant 等专门向量库

### 6.3 包结构预留

```
blog/src/main/java/com/yky/blog/
├── admin/        ← 现在已有
├── api/          ← 现在已有（前台接口）
├── common/       ← 现在已有
└── ai/           ← ⭐ 提前建好，未来用
    ├── client/   ← LangChain4j 配置 + ChatModel Bean
    ├── service/  ← SummarizeService / TagRecommendService / ReviewService
    ├── prompt/   ← Prompt 模板集中管理
    └── task/     ← 异步任务调度
```

### 6.4 配置文件预留

`application.yml` 加：
```yaml
ai:
  enabled: false                          # 总开关，默认关
  provider: openai                        # openai | anthropic | ollama
  openai:
    api-key: ${OPENAI_API_KEY:}           # 从环境变量读，不硬编码
    base-url: https://api.openai.com/v1
    model: gpt-4o-mini
    timeout: 30
  rate-limit:
    per-user-per-day: 100
  cost-control:
    monthly-budget-usd: 10
```

`ai.enabled: false` 让你**现在不加 LangChain4j 依赖**也能正常启动，**未来加上依赖后用 `@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")` 控制 Bean 加载**。

---

## 七、立即可做的微调（成本低、未来收益高）

如果你认同这条路线，**今天就可以做的事**（30 分钟搞定）：

1. ✅ 在 `application.yml` 加上 `ai.enabled: false` 这一段配置占位
2. ✅ 在 `com.yky.blog` 下新建空的 `ai/` 包（建个 `package-info.java` 占位即可）
3. ✅ 在 SQL 设计文档里给 `article` / `comment` 表加上 AI 相关字段的预留
4. ✅ 在 `2026.05.20.md` 之类的规划文档里加一行"阶段 6：AI 能力集成"

**不建议现在就做的事**：
- ❌ 现在加 LangChain4j 依赖（用不到反而拖慢启动）
- ❌ 现在做数据库迁移到 PG（除非你 100% 确定要做 RAG）
- ❌ 现在写任何 AI 相关代码（先把博客主体做完）

---

## 八、参考资料

| 内容 | 链接 |
|------|------|
| LangChain4j 官方文档 | https://docs.langchain4j.dev/ |
| LangChain4j GitHub | https://github.com/langchain4j/langchain4j |
| Spring AI 官方文档 | https://docs.spring.io/spring-ai/reference/ |
| pgvector | https://github.com/pgvector/pgvector |
| Ollama（本地运行 LLM） | https://ollama.com/ |

---

## 九、结论 TL;DR

1. **是不是加个 LangChain4j 就够了？**
   - 简单场景（摘要、推荐、审核）：**是**
   - RAG / 语义搜索：还要加**向量库**（推荐 pgvector）
   - Agent / 长任务：还要加**异步队列**和**链路追踪**
   - 生产环境：还要加**速率限制 + 成本监控**

2. **现在该做什么？**
   - **不要加任何 AI 依赖，先把博客主体（阶段 3、4）做完**
   - 但可以提前在**数据库 schema、包结构、配置文件**里留好坑位
   - 决策一件事：未来如果要做 RAG，是否现在就把 MySQL 切成 PostgreSQL

3. **未来怎么走？**
   - 按 A1 → A2 → A3 → A4 → A5 五阶段渐进，每阶段独立上线有价值
   - 主选 LangChain4j，备选 Spring AI
