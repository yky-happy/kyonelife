package com.yky.blog.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 文章相关缓存（列表/归档/详情）失效器，供管理端写操作复用。
 *
 * <p>采用 Cache-Aside 的"更新数据库 + 删除缓存"策略；并且把删除动作放在
 * <b>事务提交之后</b>执行，避免"先删缓存→事务尚未提交→并发读把旧值又写回缓存"的竞态。
 * 若当前没有事务，则立即删除。
 */
@Component
@RequiredArgsConstructor
public class ArticleCacheEvictor {

    private final RedisCacheService redisCacheService;

    /**
     * 失效全部文章缓存命名空间（列表、归档、详情）。
     * 文章/标签/合集变更都可能影响列表卡片展示，统一按前缀清空，保证不读到脏数据。
     */
    public void evictAll() {
        runAfterCommit(() -> redisCacheService.evictByPrefix(RedisKeys.ARTICLE_CACHE_PREFIX));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
