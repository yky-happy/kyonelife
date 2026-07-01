package com.yky.blog.api.service.impl;

import com.yky.blog.api.service.RealtimeStatsService;
import com.yky.blog.common.entity.EventLog;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeStatsServiceImpl implements RealtimeStatsService {

    private static final String PAGE_VIEW = "page_view";
    private static final String ARTICLE_VIEW = "article_view";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void record(EventLog event) {
        try {
            LocalDate today = LocalDate.now();
            if (PAGE_VIEW.equals(event.getEventType())) {
                String pvKey = RedisKeys.realtimePv(today);
                String uvKey = RedisKeys.realtimeUv(today);
                stringRedisTemplate.opsForValue().increment(pvKey);
                if (StringUtils.hasText(event.getVisitorId())) {
                    stringRedisTemplate.opsForHyperLogLog().add(uvKey, event.getVisitorId());
                }
                stringRedisTemplate.expire(pvKey, Duration.ofDays(3));
                stringRedisTemplate.expire(uvKey, Duration.ofDays(3));
            }
            if (ARTICLE_VIEW.equals(event.getEventType()) && event.getArticleId() != null) {
                stringRedisTemplate.opsForZSet().incrementScore(RedisKeys.REALTIME_HOT_ARTICLES, event.getArticleId().toString(), 1D);
            }
        } catch (Exception e) {
            log.warn("写入 Redis 实时埋点失败: {}", e.getMessage());
        }
    }
}
