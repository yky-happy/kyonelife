# kyonelife 埋点系统与 Redis 使用说明

> 本文档说明 kyonelife 博客的**前台行为埋点系统**整体架构，以及项目中 **Redis 的全部用法**。
> 最后附带一次真实的端到端链路验证记录。

---

## 一、总览

埋点系统采用 **「生产 → Redis Stream 削峰 → 消费 → 三层存储」** 的架构，核心目标：

- **削峰解耦**：上报接口只把事件写入 Redis Stream 后立即返回，突发流量先在流里缓冲，消费端按自己的节奏批量消费，保护 MySQL。
- **不丢数据**：Redis Stream 持久化 + 消费组 ACK 机制；消费者重启时先恢复未确认消息（至少一次语义）。
- **实时 + 离线两层统计**：Redis 提供秒级实时 PV/UV/热门排行；定时任务把明细聚合成日统计表供看板查询。

### 架构数据流

```
┌─────────────┐   sendBeacon 批量      ┌──────────────────────┐
│  前端 (Vue)  │  ───────────────────►  │ POST /api/event/report(/batch) │
│ 攒批缓冲      │   (页面隐藏/卸载冲刷)   └──────────┬───────────┘
└─────────────┘                                    │ XADD（取出 IP/UA 放入消息）
                                                    ▼
                                   ┌─────────────────────────────┐
                                   │  Redis Stream                │  ◄── 削峰缓冲层
                                   │  kyonelife:stream:events      │
                                   └──────────────┬──────────────┘
                                                  │ XREADGROUP 攒批拉取
                                                  ▼
                              ┌───────────────────────────────────────┐
                              │  EventStreamConsumer (后台线程, 消费组)   │
                              │  解析UA → 批量INSERT → 实时统计 → XACK    │
                              └──────┬───────────────────────┬─────────┘
                                     │                       │
                         批量落库     ▼                       ▼  Redis 实时层
                    ┌──────────────────────┐   ┌──────────────────────────────────┐
                    │ MySQL  event_log      │   │ PV: INCR  realtime:pv:yyyyMMdd    │
                    │ (明细层)              │   │ UV: PFADD realtime:uv:yyyyMMdd    │
                    └──────────┬───────────┘   │ 热门: ZSET realtime:hot-articles  │
                               │               └──────────────────────────────────┘
            @Scheduled 每5分钟  │ UPSERT (分布式锁保证单实例)
            (UV不可增量,全量重算) ▼
                    ┌──────────────────────────────────────────┐
                    │ MySQL 5张日聚合表 (event/article/tag/      │ ──► 后台看板查询
                    │ collection/search_keyword _daily_stat)     │     (Redis 缓存30s)
                    └──────────────────────────────────────────┘
```

---

## 二、前端采集（blog-web）

### 事件类型（6 种）

| 事件类型 | 触发时机 | 关键字段 |
|---------|---------|---------|
| `page_view` | 路由切换 `router.afterEach` | pageUrl, referrer |
| `article_view` | 文章详情页挂载 | articleId |
| `read_duration` | 离开文章页（卸载/页面隐藏，≥1s 才报） | articleId, duration(ms) |
| `tag_click` | 点击标签 | tagId |
| `collection_click` | 点击合集 | collectionId |
| `search` | 首页搜索提交 | keyword |

### 访客标识

`utils/visitor.ts` 生成 `v_{uuid}`，存于 `localStorage` 的 `kyonelife_visitor_id`，每条事件都携带，用于 UV 去重。

### 上报策略（第一档优化）

`utils/tracker.ts` + `api/event.ts`：

1. **客户端攒批**：事件先入内存缓冲区，满 10 条 / 每 5 秒 / 页面隐藏时批量上报，减少请求数。
2. **`navigator.sendBeacon` 上报**：发往 `POST /api/event/report/batch`。
   - 为什么用 sendBeacon：它在页面跳转/关闭时仍能**可靠送达且不阻塞导航**。普通 `fetch` 在页面卸载阶段会被浏览器掐断，导致 `read_duration` 大量丢失。
   - sendBeacon 不可用时回退到 `fetch(..., { keepalive: true })`。
3. **终态事件立即发**：`read_duration` 入队后立即冲刷，避免页面随即关闭丢失。

---

## 三、后端生产者（写入事件流）

`EventReportServiceImpl`：

- 只负责把事件 **XADD 进 Redis Stream** 后立即返回（fire-and-forget，不阻塞前台）。
- **客户端 IP、User-Agent 在请求线程内取出**放进消息——因为消费者运行在后台线程，没有 `HttpServletRequest` 上下文。
- 缺失 eventType / visitorId 的脏数据直接丢弃；批量上报上限 50 条防滥用。
- 事件时间戳 `occurredAt` 在生产端打入，保证落库 `create_time` 反映事件真实发生时间。

接口（`EventReportController`，前缀 `/api/event`，无需登录）：

| 接口 | 说明 | 限流 |
|------|------|------|
| `POST /report` | 单条上报 | 30 次 / 10s / IP |
| `POST /report/batch` | 批量上报（前端 sendBeacon 用） | 60 次 / 10s / IP |

---

## 四、Redis Stream 削峰消费者（第二档核心）

`EventStreamConsumer`（`SmartLifecycle`，独立后台线程）：

```
启动 → 创建消费组(MKSTREAM) → drainPending(恢复上次未确认消息)
     → 循环: XREADGROUP 攒批(100条) → 解析 → 批量INSERT → 实时统计 → XACK
```

**关键设计：**

| 能力 | 实现 |
|------|------|
| 削峰填谷 | 突发流量堆在 Stream，消费端按自己节奏消费 |
| 不丢数据 | Stream 持久化 + 消费组 ACK；落库失败**不 ACK**，消息保留为 pending |
| 崩溃恢复 | 启动先 `drainPending`（读 id=0 的 pending）重处理上次未 ACK 的消息（至少一次） |
| 降低写放大 | `EventLogMapper.insertBatch` **单条多值 INSERT** 批量落库，替代每事件一条 INSERT |
| 内存回收 | `@Scheduled` 每 10 分钟 `XTRIM` 近似裁剪流长度（上限 100 万） |
| 避免阻塞 | 用**非阻塞读 + 空轮询 sleep(500ms)**，而非 `BLOCK`，防止占用 Lettuce 共享连接 |

> 消费者名固定 `consumer-main`，单实例足够；多实例部署需为每实例配置不同消费者名（同组并行消费、各自维护 pending）。

---

## 五、三层存储

### 1. 明细层 —— MySQL `event_log`
原始事件全量留存，含解析后的 device/browser/os、IP、时长等。是聚合与回溯的数据源。
按日 RANGE 分区 + TTL 见 `blog/sql/event_log_partition.sql`（过期分区 `DROP PARTITION` 秒级清理）。

### 2. 实时层 —— Redis
消费者处理每条事件时实时更新（见下方 Redis 用法表）。提供秒级 PV/UV/热门数据。

### 3. 离线聚合层 —— MySQL 5 张日统计表
`AnalyticsAggregationServiceImpl` 用 `@Scheduled` 每 5 分钟从 `event_log` UPSERT 聚合：

| 明细 → 聚合表 | 内容 |
|--------------|------|
| `event_daily_stat` | 每日 PV/UV/总时长 |
| `article_daily_stat` | 每篇文章每日浏览/访客/时长 |
| `tag_daily_stat` | 标签每日点击 |
| `collection_daily_stat` | 合集每日点击 |
| `search_keyword_stat` | 搜索词每日次数 |

> **为何是"全量重算"而非增量聚合**：UV 是 `COUNT(DISTINCT visitor_id)`，**跨批次不可累加**，强行增量会算错 UV。因此保留按日全量重算 + UPSERT 的幂等方式保证 UV 正确；用**分布式锁**解决多实例重复执行的问题。

---

## 六、Redis 全部用法清单

> 连接：`application.yml` 配 `localhost:6379`（容器 `blogRedis` 端口映射），统一用 `StringRedisTemplate`。
> 所有 key 以 `kyonelife:` 前缀命名（见 `common/redis/RedisKeys.java`）。

| 用途 | Key | 数据结构 | 说明 |
|------|-----|---------|------|
| **埋点事件流** | `kyonelife:stream:events` | **Stream** | 削峰缓冲，消费组 `event-consumers` 消费 |
| **实时 PV** | `kyonelife:realtime:pv:yyyyMMdd` | String(INCR) | 当日页面浏览量，TTL 3 天 |
| **实时 UV** | `kyonelife:realtime:uv:yyyyMMdd` | **HyperLogLog** | 当日去重访客数，固定 12KB 内存近似去重，TTL 3 天 |
| **热门文章** | `kyonelife:realtime:hot-articles` | **ZSet** | 文章浏览数排行，`ZINCRBY` 累加，`ZREVRANGE` 取榜 |
| **文章阅读量增量** | `kyonelife:article:view-delta` | **Hash** | 文章详情浏览量先攒在 Redis，每 2 分钟刷回 MySQL，削减热点行写 |
| **接口限流** | `kyonelife:ratelimit:{name}:{ip}` | **ZSet** | 滑动窗口限流，成员=请求、score=时间戳 |
| **分布式锁** | `kyonelife:lock:{name}` | String(SETNX) | 保证定时任务集群内单实例执行 |
| **页面/列表缓存** | `kyonelife:cache:article:*` | String(JSON) | 文章列表/归档/详情缓存 |
| **RBAC 权限缓存** | `kyonelife:rbac:perms:{adminId}` / `roles:{adminId}` | String(JSON) | 管理员权限/角色缓存，TTL 30 分钟 |
| **点赞用户集合** | `kyonelife:like:users:{articleId}` | **Set** | 已点赞访客集合，`SISMEMBER` 判一人一赞 / isLiked |
| **点赞计数** | `kyonelife:like:count:{articleId}` | String | 文章点赞数，Redis 实时权威计数 |

下面分述几处非平凡用法。

### 6.1 实时统计（HyperLogLog + ZSet）

- **UV 用 HyperLogLog**：`PFADD` 写入 visitorId、`PFCOUNT` 读取去重数。相比 Set 存全部访客 ID，HLL 用**固定 ~12KB** 内存即可对海量访客做基数估算（误差 ~0.81%），是 UV 统计的标准方案。
- **热门文章用 ZSet**：`ZINCRBY` 给文章打分（每次浏览 +1），`ZREVRANGE ... WITHSCORES` 直接拿到 Top-N 排行，O(logN) 写、范围查询天然有序。

### 6.2 文章阅读量（Hash 增量 + 定时刷库）

文章详情页每次访问都要 +1 阅读量。若每次直接 `UPDATE article SET view_count=view_count+1`，会在热点行上产生大量行锁竞争。优化：

- 浏览时 `HINCRBY kyonelife:article:view-delta {articleId} 1` 把增量攒在 Redis；
- 展示时返回 `数据库基础值 + Redis 增量`，保证实时；
- `@Scheduled` 每 2 分钟把增量批量刷回 MySQL 并清零（带分布式锁，并失效对应文章详情缓存防止阅读量回退）。

### 6.3 接口限流（ZSet 滑动窗口 + Lua）

`RateLimiter` + `@RateLimit` 注解 + AOP 切面。滑动窗口用 ZSet：成员=单次请求，score=毫秒时间戳。每次请求在 **一段 Lua 脚本**里原子完成：
`ZREMRANGEBYSCORE 移除窗口外` → `ZCARD 统计窗口内` → 未超限则 `ZADD` 记录 + `PEXPIRE` 续期。
整段塞进 Lua 在 Redis 端单线程原子执行，避免"先查后写"的并发竞态。按 **IP** 限流（visitorId 可被前端伪造，IP 才是真实成本维度）。

### 6.4 分布式锁（SETNX + Lua 安全释放）

`DistributedLock`：`SET key token NX EX` 抢锁，释放时用 Lua **比对 token 再删除**，避免误删他人持有的锁。用于让 `@Scheduled` 的聚合、刷库任务在多实例下只执行一个。生产可换 ShedLock / Redisson，这里零额外依赖自实现。

### 6.5 缓存与一致性

`RedisCacheService` 统一封装 Cache-Aside 读写，并处理**缓存三大问题**：

| 问题 | 处理 |
|------|------|
| 缓存穿透 | loader 返回 null 时缓存**空值哨兵**（短 TTL 60s），挡住不存在数据反复打库 |
| 缓存击穿 | 未命中时用 **Redis 互斥锁(SETNX)** 重建，热点 key 只放一个线程回源，其余等待复用 |
| 缓存雪崩 | 写入时 TTL **叠加 0~10% 随机抖动**，打散过期时刻 |

**一致性（Cache-Aside 删除策略）**：文章/标签/合集写操作后失效 `kyonelife:cache:article:*`，并把删除动作放在**事务提交后**执行，避免"先删缓存→事务未提交→并发读把旧值写回"的竞态。按前缀失效用 **SCAN** 渐进遍历（不阻塞 Redis，区别于 KEYS）。

### 6.6 RBAC 权限缓存

`PermissionService` 把管理员的权限标识/角色编码缓存在 Redis（TTL 30 分钟），避免每次鉴权都查 `admin→role→menu` 关联链。登录/登出时清除缓存，保证角色变更即时生效。

### 6.7 文章点赞（Lua 原子 toggle）

点赞需求：**不限量、一人一赞、取消后可重赞**（toggle）。身份用前台 `visitorId` 标识（与埋点访客体系一致，将来接入用户登录后换成 userId）。

**为什么用 Lua，而不用 CAS / 分布式锁：**
- 不限量 → 没有"超卖"，**乐观锁 CAS 无用武之地**（CAS 是带条件递减一个库存列，这里没有库存）。
- toggle 是"判断是否已赞 → `SADD/SREM` → 计数 `±1`"的**多命令 check-then-act**，跨两个 key。若不原子，并发双击/重试会让**计数与集合漂移、甚至翻倍**。把它塞进**一段 Lua** 在 Redis 端单线程原子执行即可解决，**不需要分布式锁**。
- "一人一赞"由 `Set` 的天然去重 + 数据库 `like_record` 的**唯一索引**兜底。

**toggle 的 Lua 脚本**（`KEYS[1]`=点赞用户集合, `KEYS[2]`=计数, `ARGV[1]`=visitorId）：

```lua
if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
    redis.call('SREM', KEYS[1], ARGV[1])
    local c = redis.call('DECR', KEYS[2])
    if c < 0 then redis.call('SET', KEYS[2], '0'); c = 0 end
    return {0, c}          -- 取消点赞
else
    redis.call('SADD', KEYS[1], ARGV[1])
    local c = redis.call('INCR', KEYS[2])
    return {1, c}          -- 点赞成功
end
```

**分层架构（与阅读量同思路，复用"Redis 实时 + 异步落库"模式）：**

```
POST /api/article/{id}/like  (限流 20/10s)
        │ Lua 原子 toggle（Set 成员 + 计数 一起原子变更）
        ▼
   Redis 实时权威层： like:users:{id}(Set) + like:count:{id}
        │ 异步落库 @Async（最终一致兜底）
        ▼
   MySQL： like_record(唯一索引一人一赞) + article.like_count
           （仅在影响行数>0 时才 ±1，防并发重复点赞刷大计数）
```

- **冷启动重建**：Redis 无该文章计数时，从 `like_record` 重建集合与计数（以记录真实条数为准，自愈历史漂移），用**分布式锁防重建击穿**。
- **读路径**：`GET /api/article/{id}/like-status?visitorId=` 直接读 Redis（`SISMEMBER` + 计数），不进文章详情缓存以保持点赞数实时。

> 关键不变量：**Redis 计数 ≡ 集合基数(SCARD)**。Lua 的原子性保证它在任意并发下都成立（见第七节并发验证）。

---

## 七、端到端验证记录

> 环境：MySQL 容器 `blogmysql`、Redis 容器 `blogRedis`、应用本地 `localhost:8080`。
> 容器内执行 redis-cli 需带密码：`docker exec blogRedis redis-cli -a <密码> ...`。

发送 9 条测试事件（1 条单发 + 4 条批量 + 4 条文章浏览）后核对四层：

**① Redis Stream**
```
XLEN kyonelife:stream:events            → 9          (9条全部入流)
XINFO GROUPS                            → pending 0, lag 0   (全部消费并ACK)
```

**② Redis 实时层**
```
GET   kyonelife:realtime:pv:20260624    → 2          (2条 page_view)
PFCOUNT kyonelife:realtime:uv:20260624  → 2          (2个去重访客)
ZREVRANGE kyonelife:realtime:hot-articles 0 -1 WITHSCORES
                                        → 文章1=4分, 文章2=1分   (与发送量吻合)
```

**③ MySQL 明细层（批量插入）**
```
SELECT * FROM event_log  → 9 行，UA已解析(device/browser/os)，read_duration.duration=8500
```

**④ MySQL 离线聚合层（定时任务产出）**
```
event_daily_stat:    article_view pv=5 uv=4 | page_view pv=32 uv=3 | read_duration duration_total=8500 | search pv=1
article_daily_stat:  文章1 view=4 visitor=4 | 文章2 view=1 visitor=1
search_keyword_stat: redis 1
```

**⑤ 限流**
```
连续高频请求 → {"code":429,"message":"请求过于频繁，请稍后再试"}
限流key: kyonelife:ratelimit:event-report:0:0:0:0:0:0:0:1   (::1 本地回环)
```

结论：生产 → Stream → 消费 → 批量落库 → 实时统计 → 定时聚合 全链路打通，五处校验数据完全一致。

### 文章点赞（Lua 原子 toggle）并发验证

```
基础 toggle：v_like_1 赞→取消、v_like_2 赞
  → Redis 集合只剩 v_like_2、计数=1；MySQL like_record 同步只剩 v_like_2     ✓ 双端一致

落库一致性（文章2）：
  → Redis 计数=2 | article.like_count=2 | like_record=2 条                  ✓ 三端一致

并发A：15 个不同访客并发点赞同一文章
  → 计数=15，集合基数(SCARD)=15                                            ✓ 无丢更新、零漂移

并发B：同一访客并发点击 10 次（模拟双击/重试）
  → 计数=1，集合基数=1（绝不会被刷成 10）                                   ✓ 无翻倍
```

关键不变量 **Redis 计数 ≡ 集合基数** 在并发下恒成立——由 Lua 的原子性保证；若非原子，并发 B 会出现计数与集合漂移。

---

## 八、设计取舍与演进方向

**已落地（适配本项目体量）：**
- Redis Stream 削峰解耦 + 批量落库 + sendBeacon 可靠上报 + 分布式锁 + 表分区。

**诚实的取舍：**
- **未做增量聚合**：UV 不可跨批次累加，全量重算 + 分布式锁是更正确的选择。
- **至少一次语义**：崩溃重放可能产生少量重复事件（统计轻微偏高）；精确一次需给事件加唯一 ID 去重，对博客埋点不值得。

**进一步演进（可在面试中阐述，本项目属过度设计）：**
- 引入 **Kafka/RocketMQ** 替代 Redis Stream，支撑更高吞吐与多消费者水平扩展。
- 原始事件入 **ClickHouse** 等 OLAP（列存、物化视图自动 rollup），替代 MySQL 明细 + 定时聚合。
- 可观测性接入 **Prometheus + Grafana**，替代自研运行日志解析。

---

## 九、关键代码位置

| 模块 | 文件 |
|------|------|
| 上报接口 | `api/controller/EventReportController.java` |
| 生产者（XADD） | `api/service/impl/EventReportServiceImpl.java` |
| 流消费者 | `api/stream/EventStreamConsumer.java` |
| 实时统计 | `api/service/impl/RealtimeStatsServiceImpl.java` |
| 阅读量增量 | `api/service/impl/ArticleViewCountServiceImpl.java` |
| 离线聚合 | `admin/service/impl/AnalyticsAggregationServiceImpl.java` |
| 批量插入 | `common/mapper/EventLogMapper.java#insertBatch` |
| Redis Key 定义 | `common/redis/RedisKeys.java` |
| 缓存封装（三大问题） | `common/redis/RedisCacheService.java` |
| 限流器 | `common/redis/RateLimiter.java` + `common/annotation/RateLimit.java` |
| 分布式锁 | `common/redis/DistributedLock.java` |
| 点赞（Lua 原子 toggle） | `api/service/impl/ArticleLikeServiceImpl.java` |
| 点赞异步落库 | `api/service/LikePersistService.java` |
| 点赞建表脚本 | `blog/sql/like.sql` |
| 表分区脚本 | `blog/sql/event_log_partition.sql` |
| 前端埋点 | `blog-web/src/utils/tracker.ts` + `src/api/event.ts` |
| 前端点赞 | `blog-web/src/views/article/detail.vue` + `src/api/article.ts` |
