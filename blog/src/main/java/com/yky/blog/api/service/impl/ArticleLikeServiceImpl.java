package com.yky.blog.api.service.impl;

import com.yky.blog.api.dto.LikeStatusVO;
import com.yky.blog.api.service.ArticleLikeService;
import com.yky.blog.api.service.LikePersistService;
import com.yky.blog.common.mapper.LikeRecordMapper;
import com.yky.blog.common.redis.DistributedLock;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * 文章点赞（不限量、一人一赞、可取消重赞）。
 *
 * <p>设计（方案B）：Redis 为实时权威层，MySQL 异步落库做最终一致兜底。
 * <ul>
 *   <li><b>一人一赞 + 防并发翻倍</b>：toggle 是"判断是否已赞 → SADD/SREM → 计数±1"的多命令
 *       check-then-act，用 <b>一段 Lua 脚本</b>在 Redis 端原子执行，杜绝并发双击导致计数翻倍。
 *       这里 Lua 是真正必要的（多 key 必须一起原子变更），而非为用而用。</li>
 *   <li><b>无需 CAS / 分布式锁</b>：不限量 → 没有超卖，CAS 无用武之地；toggle 的原子性由 Lua 保证。</li>
 *   <li><b>冷启动重建</b>：Redis 无该文章计数时，从 like_record 重建集合与计数（以记录真实条数为准，自愈漂移），
 *       用分布式锁防并发重建击穿。</li>
 *   <li><b>持久化</b>：toggle 后异步写 like_record（唯一索引兜底一人一赞）+ article.like_count。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleLikeServiceImpl implements ArticleLikeService {

    /**
     * 原子 toggle 脚本。KEYS[1]=点赞用户集合, KEYS[2]=计数; ARGV[1]=visitorId。
     * 返回 {state, count}：state=1 点赞成功 / 0 取消成功。
     */
    private static final DefaultRedisScript<List> TOGGLE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                redis.call('SREM', KEYS[1], ARGV[1])
                local c = redis.call('DECR', KEYS[2])
                if c < 0 then redis.call('SET', KEYS[2], '0'); c = 0 end
                return {0, c}
            else
                redis.call('SADD', KEYS[1], ARGV[1])
                local c = redis.call('INCR', KEYS[2])
                return {1, c}
            end
            """,
            List.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final LikeRecordMapper likeRecordMapper;
    private final LikePersistService likePersistService;
    private final DistributedLock distributedLock;

    @Override
    @SuppressWarnings("unchecked")
    public LikeStatusVO toggle(Long articleId, String visitorId) {
        ensureLoaded(articleId);
        List<Long> result = stringRedisTemplate.execute(
                TOGGLE_SCRIPT,
                Arrays.asList(RedisKeys.likeUsers(articleId), RedisKeys.likeCount(articleId)),
                visitorId);
        boolean liked = result != null && result.get(0) == 1L;
        long count = result == null ? 0L : result.get(1);
        // 实时结果异步落库（最终一致），不阻塞响应
        likePersistService.persist(articleId, visitorId, liked);
        return new LikeStatusVO(liked, count);
    }

    @Override
    public LikeStatusVO getStatus(Long articleId, String visitorId) {
        ensureLoaded(articleId);
        boolean liked = visitorId != null && Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeys.likeUsers(articleId), visitorId));
        String raw = stringRedisTemplate.opsForValue().get(RedisKeys.likeCount(articleId));
        long count = raw == null ? 0L : Long.parseLong(raw);
        return new LikeStatusVO(liked, count);
    }

    /**
     * 确保该文章的点赞状态已加载到 Redis；未加载则从数据库重建（防击穿用分布式锁）。
     */
    private void ensureLoaded(Long articleId) {
        String countKey = RedisKeys.likeCount(articleId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(countKey))) {
            return;
        }
        boolean rebuiltBySelf = distributedLock.runIfAcquired(
                RedisKeys.lock("like-load:" + articleId), Duration.ofSeconds(10), () -> {
                    if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(countKey))) {
                        loadFromDb(articleId);
                    }
                });
        if (!rebuiltBySelf) {
            // 其它线程正在重建，短暂等待其完成，避免拿到空状态
            for (int i = 0; i < 20 && !Boolean.TRUE.equals(stringRedisTemplate.hasKey(countKey)); i++) {
                sleep(50);
            }
        }
    }

    private void loadFromDb(Long articleId) {
        List<String> visitors = likeRecordMapper.selectVisitorIds(articleId);
        String usersKey = RedisKeys.likeUsers(articleId);
        String countKey = RedisKeys.likeCount(articleId);
        stringRedisTemplate.delete(usersKey);
        if (!visitors.isEmpty()) {
            stringRedisTemplate.opsForSet().add(usersKey, visitors.toArray(new String[0]));
        }
        // 计数以 like_record 真实条数为准，保证 Redis 计数==集合基数，自愈历史漂移
        stringRedisTemplate.opsForValue().set(countKey, Integer.toString(visitors.size()));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
