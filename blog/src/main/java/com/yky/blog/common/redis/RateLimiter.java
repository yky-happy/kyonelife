package com.yky.blog.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于 Redis ZSET 的滑动窗口限流器。
 *
 * <p>实现思路：每个限流 key 对应一个有序集合，成员为一次请求、score 为请求毫秒时间戳。
 * 每次请求时：① 移除窗口外的旧成员 → ② 统计窗口内成员数 → ③ 未超限则写入当前请求。
 * 三步通过一段 Lua 脚本在 Redis 端原子执行，避免"先查后写"产生的并发竞态。
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    /**
     * 滑动窗口限流脚本。
     * KEYS[1]=限流key，ARGV: [当前时间ms, 窗口ms, 阈值, 唯一成员]。
     * 返回 1 表示放行，0 表示被限流。
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                return 0
            end
            redis.call('ZADD', key, now, member)
            redis.call('PEXPIRE', key, window)
            return 1
            """,
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取一次通行许可。
     *
     * @param key    限流 key（已包含维度，如 IP）
     * @param limit  窗口内最大请求数
     * @param window 滑动窗口时长
     * @return true=放行，false=已达上限被限流
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        long windowMs = window.toMillis();
        // 唯一成员：时间戳 + 随机数，避免同一毫秒内多次请求 ZADD 相互覆盖导致漏计
        String member = now + "-" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        Long allowed = stringRedisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(limit),
                member);
        return allowed != null && allowed == 1L;
    }
}
