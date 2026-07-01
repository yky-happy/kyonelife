package com.yky.blog.api.service.impl;

import com.yky.blog.api.service.ArticleViewCountService;
import com.yky.blog.common.mapper.ArticleMapper;
import com.yky.blog.common.redis.DistributedLock;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewCountServiceImpl implements ArticleViewCountService {

    /**
     * 原子扣减本轮已刷库的增量。
     * 若读取快照后又有新浏览量写入同一个 hash field，只扣掉快照值，保留新增值给下轮刷库。
     */
    private static final DefaultRedisScript<Long> CONSUME_DELTA_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current then
                return 0
            end
            local current_num = tonumber(current)
            local flush_num = tonumber(ARGV[2])
            if current_num <= flush_num then
                redis.call('HDEL', KEYS[1], ARGV[1])
                return current_num
            else
                redis.call('HINCRBY', KEYS[1], ARGV[1], -flush_num)
                return flush_num
            end
            """,
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleMapper articleMapper;
    private final RedisCacheService redisCacheService;
    private final DistributedLock distributedLock;

    @Override
    public long increaseAndGetDelta(Long articleId) {
        try {
            Long delta = stringRedisTemplate.opsForHash().increment(RedisKeys.ARTICLE_VIEW_DELTA, articleId.toString(), 1L);
            stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.REALTIME_HOT_ARTICLES, articleId.toString(), 1D);
            return delta == null ? 0L : delta;
        } catch (Exception e) {
            log.warn("写入 Redis 阅读量失败，降级直接写 MySQL articleId={}: {}", articleId, e.getMessage());
            articleMapper.incrementViewCount(articleId, 1L);
            return 1L;
        }
    }

    @Override
    public long getDelta(Long articleId) {
        try {
            Object value = stringRedisTemplate.opsForHash().get(RedisKeys.ARTICLE_VIEW_DELTA, articleId.toString());
            return value == null ? 0L : Long.parseLong(value.toString());
        } catch (Exception e) {
            log.warn("读取 Redis 阅读量增量失败 articleId={}: {}", articleId, e.getMessage());
            return 0L;
        }
    }

    @Override
    @Scheduled(cron = "0 */2 * * * ?")
    public void flushToDb() {
        // 多实例只让一个实例刷库，避免重复累加阅读量
        distributedLock.runIfAcquired(RedisKeys.lock("view-count-flush"), Duration.ofMinutes(1), this::doFlush);
    }

    private void doFlush() {
        Map<Object, Object> deltas;
        try {
            deltas = stringRedisTemplate.opsForHash().entries(RedisKeys.ARTICLE_VIEW_DELTA);
        } catch (Exception e) {
            log.warn("读取 Redis 阅读量增量失败，跳过本轮刷库: {}", e.getMessage());
            return;
        }
        if (deltas.isEmpty()) {
            return;
        }

        deltas.forEach((articleId, delta) -> {
            long snapshot = Long.parseLong(delta.toString());
            if (snapshot <= 0) {
                return;
            }
            String id = articleId.toString();
            // 先原子认领（扣减 Redis）再写库：即便认领后、写库前进程崩溃，
            // 最多丢失这一份增量，而不会下一轮重复读取同值再次累加（计数只少不多，符合阅读量场景）。
            long claimed = consumeDelta(id, snapshot);
            if (claimed <= 0) {
                return;
            }
            try {
                articleMapper.incrementViewCount(Long.valueOf(id), claimed);
                // 增量已并入数据库基础值且 Redis 已扣减，失效该文章详情缓存，
                // 避免详情缓存里旧的基础值 + 归零后的增量造成展示阅读量回退
                redisCacheService.evict(RedisKeys.articleDetail(articleId));
            } catch (Exception e) {
                // 写库失败：把已认领的增量回补到 Redis，留待下一轮重试，避免丢数据
                log.warn("阅读量写库失败，回补增量待下轮重试 articleId={} count={}: {}", id, claimed, e.getMessage());
                addBackDelta(id, claimed);
            }
        });
        log.info("已刷写文章阅读量到 MySQL，文章数={}", deltas.size());
    }

    /** 原子认领并扣减本轮快照增量，返回实际认领到的数量（保留期间新写入的增量给下轮）。 */
    private long consumeDelta(String articleId, long count) {
        Long consumed = stringRedisTemplate.execute(
                CONSUME_DELTA_SCRIPT,
                List.of(RedisKeys.ARTICLE_VIEW_DELTA),
                articleId,
                Long.toString(count));
        if (consumed == null || consumed <= 0) {
            log.warn("阅读量增量扣减 Redis 失败或已不存在 articleId={} count={}", articleId, count);
            return 0L;
        }
        return consumed;
    }

    /** 写库失败时把已认领的增量回补到 Redis hash，等待下一轮刷库重试。 */
    private void addBackDelta(String articleId, long count) {
        try {
            stringRedisTemplate.opsForHash().increment(RedisKeys.ARTICLE_VIEW_DELTA, articleId, count);
        } catch (Exception e) {
            log.error("阅读量回补 Redis 失败，该增量丢失 articleId={} count={}: {}", articleId, count, e.getMessage());
        }
    }
}
