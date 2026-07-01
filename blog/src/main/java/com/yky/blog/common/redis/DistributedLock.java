package com.yky.blog.common.redis;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Redis 的简易分布式锁（带看门狗自动续期）。
 *
 * <p>用 {@code SET key token NX EX} 抢锁，释放时用 Lua "比对 token 再删除"，避免误删他人的锁。
 * 持锁期间由后台看门狗每 ttl/3 续期一次（仅当 token 仍匹配时），
 * 防止任务执行时间超过锁 TTL 导致锁提前过期、另一实例并发执行。
 * 生产可替换为 Redisson，这里为零额外依赖自实现。
 */
@Slf4j
@Component
public class DistributedLock {

    /** 释放锁：仅当持有者 token 匹配时才删除。 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    /** 续期：仅当 token 匹配时才重置过期时间。 */
    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
            Long.class);

    /**
     * 续期上限：任务最长持锁时间。超过则停止续期、任由锁自然过期，
     * 防止任务异常卡死时无限续期把锁永久占住、阻塞其它实例。
     */
    private static final Duration MAX_HOLD = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final ScheduledExecutorService watchdog = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "lock-watchdog");
        t.setDaemon(true);
        return t;
    });

    public DistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 抢到锁则执行任务（期间自动续期）并在结束后释放；抢不到则跳过。
     *
     * @return true=本实例执行了任务，false=未抢到锁（其它实例在执行）
     */
    public boolean runIfAcquired(String lockKey, Duration ttl, Runnable task) {
        String token = UUID.randomUUID().toString();
        if (!tryLock(lockKey, token, ttl)) {
            return false;
        }
        long ttlMs = ttl.toMillis();
        long renewMs = Math.max(ttlMs / 3, 1000);
        long maxRenewals = Math.max(1, MAX_HOLD.toMillis() / renewMs);
        // 用 AtomicReference 持有 Future，便于续期次数到顶时在回调里自取消
        AtomicReference<ScheduledFuture<?>> renewRef = new AtomicReference<>();
        long[] renewed = {0};
        ScheduledFuture<?> renewTask = watchdog.scheduleAtFixedRate(() -> {
            if (++renewed[0] > maxRenewals) {
                log.warn("分布式锁续期已达上限({} 次)，停止续期任由其过期 lockKey={}", maxRenewals, lockKey);
                ScheduledFuture<?> self = renewRef.get();
                if (self != null) {
                    self.cancel(false);
                }
                return;
            }
            renew(lockKey, token, ttlMs);
        }, renewMs, renewMs, TimeUnit.MILLISECONDS);
        renewRef.set(renewTask);
        try {
            task.run();
            return true;
        } finally {
            renewTask.cancel(false);
            unlock(lockKey, token);
        }
    }

    /** 应用关闭时优雅停掉看门狗线程池。 */
    @PreDestroy
    public void shutdown() {
        watchdog.shutdownNow();
    }

    private boolean tryLock(String lockKey, String token, Duration ttl) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, ttl));
        } catch (Exception e) {
            log.warn("获取分布式锁失败 lockKey={}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    private void renew(String lockKey, String token, long ttlMs) {
        try {
            stringRedisTemplate.execute(RENEW_SCRIPT, Collections.singletonList(lockKey), token, String.valueOf(ttlMs));
        } catch (Exception e) {
            log.warn("续期分布式锁失败 lockKey={}: {}", lockKey, e.getMessage());
        }
    }

    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (Exception e) {
            log.warn("释放分布式锁失败 lockKey={}: {}", lockKey, e.getMessage());
        }
    }
}
