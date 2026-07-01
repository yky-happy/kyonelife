package com.yky.blog.common.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RedisKeys {

    public static final String ARTICLE_VIEW_DELTA = "kyonelife:article:view-delta";
    public static final String REALTIME_HOT_ARTICLES = "kyonelife:realtime:hot-articles";

    /** 埋点事件流（Redis Stream）key 及消费组名。 */
    public static final String EVENT_STREAM = "kyonelife:stream:events";
    public static final String EVENT_STREAM_GROUP = "event-consumers";

    /** 埋点事件去重 key（消费成功后写入，防 Stream 重放重复处理）。 */
    public static String eventDedup(String eventId) {
        return "kyonelife:event:dedup:" + eventId;
    }

    /**
     * 文章相关缓存统一前缀（列表/归档/详情都在其下），便于一次性失效。
     * 注意：要与 {@link #cache(String)} 生成的 article:* 前缀保持一致。
     */
    public static final String ARTICLE_CACHE_PREFIX = "kyonelife:cache:article:";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private RedisKeys() {
    }

    public static String cache(String name) {
        return "kyonelife:cache:" + name;
    }

    /** 分布式锁 key。 */
    public static String lock(String name) {
        return "kyonelife:lock:" + name;
    }

    /** 文章点赞访客集合 key（Set，存已点赞的 visitorId，用于一人一赞判断与 isLiked）。 */
    public static String likeUsers(Object articleId) {
        return "kyonelife:like:users:" + articleId;
    }

    /** 文章点赞数 key（String，Redis 实时权威计数）。 */
    public static String likeCount(Object articleId) {
        return "kyonelife:like:count:" + articleId;
    }

    /**
     * 前台文章详情缓存 key（在 {@link #ARTICLE_CACHE_PREFIX} 命名空间下）。
     */
    public static String articleDetail(Object articleId) {
        return ARTICLE_CACHE_PREFIX + "detail:" + articleId;
    }

    public static String realtimePv(LocalDate date) {
        return "kyonelife:realtime:pv:" + DATE_FORMATTER.format(date);
    }

    public static String realtimeUv(LocalDate date) {
        return "kyonelife:realtime:uv:" + DATE_FORMATTER.format(date);
    }

    /**
     * 管理员权限标识缓存 key（按 adminId 维度）。
     */
    public static String adminPerms(Object adminId) {
        return "kyonelife:rbac:perms:" + adminId;
    }

    /**
     * 管理员角色编码缓存 key（按 adminId 维度）。
     */
    public static String adminRoles(Object adminId) {
        return "kyonelife:rbac:roles:" + adminId;
    }

    /**
     * 接口限流 key（按接口标识 + 维度标识，如 IP）。
     */
    public static String rateLimit(String name, String identity) {
        return "kyonelife:ratelimit:" + name + ":" + identity;
    }

    /**
     * Agent 跨请求会话记忆 key（按管理员、场景、会话隔离）。
     */
    public static String agentMemory(String conversationId) {
        return "kyonelife:agent:memory:" + conversationId;
    }
}
