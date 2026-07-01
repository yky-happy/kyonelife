package com.yky.blog.api.service;

import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.mapper.LikeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞的异步持久化：Redis 完成实时 toggle 后，异步把结果落到 MySQL（最终一致兜底）。
 * 单独成 Bean 以让 {@code @Async} 代理生效（同类自调用不会异步）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikePersistService {

    /** 落库失败的最大重试次数。 */
    private static final int MAX_ATTEMPTS = 3;

    private final LikeRecordMapper likeRecordMapper;
    private final ArticleMapper articleMapper;

    /**
     * 自身代理引用：用于在 {@link #persist} 内调用带事务的 {@link #persistOnce}，
     * 绕过同类自调用导致 {@code @Transactional} 失效的问题。{@code @Lazy} 打破构造期循环依赖。
     */
    @Lazy
    @Autowired
    private LikePersistService self;

    /**
     * @param liked true=点赞落库，false=取消落库
     * 失败按指数退避重试，每次重试都是独立事务（insert/delete 与计数增减同进同退），
     * 因此中途失败回滚后重试不会出现「记录已插入但计数没加」的永久错位。
     * 重试用尽仍失败则打 ERROR，供告警与后续对账任务兜底。
     */
    @Async
    public void persist(Long articleId, String visitorId, boolean liked) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                self.persistOnce(articleId, visitorId, liked);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("点赞落库最终失败（已重试 {} 次）articleId={} visitorId={} liked={}: {}",
                            MAX_ATTEMPTS, articleId, visitorId, liked, e.getMessage(), e);
                    return;
                }
                log.warn("点赞落库失败，准备第 {}/{} 次重试 articleId={} liked={}: {}",
                        attempt + 1, MAX_ATTEMPTS, articleId, liked, e.getMessage());
                sleepBeforeRetry(attempt);
            }
        }
    }

    /**
     * 单次落库（独立事务）。仅在 like_record 实际发生变更（影响行数>0）时才同步增减
     * article.like_count，配合唯一索引保证并发重复点赞不会把计数刷大。
     * 任一步异常都会回滚整笔，保证记录与计数一致。
     */
    @Transactional(rollbackFor = Exception.class)
    public void persistOnce(Long articleId, String visitorId, boolean liked) {
        if (liked) {
            if (likeRecordMapper.insertIgnore(articleId, visitorId) > 0) {
                articleMapper.incrementLikeCount(articleId, 1L);
            }
        } else {
            if (likeRecordMapper.deleteOne(articleId, visitorId) > 0) {
                articleMapper.incrementLikeCount(articleId, -1L);
            }
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(attempt * 200L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
