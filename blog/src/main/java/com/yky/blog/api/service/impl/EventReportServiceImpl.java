package com.yky.blog.api.service.impl;

import com.yky.blog.api.dto.EventReportDTO;
import com.yky.blog.api.service.EventReportService;
import com.yky.blog.common.redis.RedisKeys;
import com.yky.blog.common.util.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 埋点上报生产者：只负责把事件写入 Redis Stream 后立即返回（削峰解耦），
 * 解析落库、实时统计等重活交给 {@code EventStreamConsumer} 异步消费。
 *
 * <p>注意：客户端 IP、User-Agent 必须在此处（请求线程内）取出放入消息，
 * 因为消费者运行在后台线程、没有 HttpServletRequest 上下文。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventReportServiceImpl implements EventReportService {

    /** 单次批量上报的最大事件数，防滥用。 */
    private static final int MAX_BATCH = 50;
    private static final int MAX_UA_LENGTH = 1000;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void report(EventReportDTO dto, HttpServletRequest request) {
        publish(dto, getClientIp(request), userAgent(request), System.currentTimeMillis());
    }

    @Override
    public void reportBatch(List<EventReportDTO> events, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        String ip = getClientIp(request);
        String ua = userAgent(request);
        long now = System.currentTimeMillis();
        events.stream().limit(MAX_BATCH).forEach(dto -> publish(dto, ip, ua, now));
    }

    private void publish(EventReportDTO dto, String ip, String ua, long occurredAt) {
        // 事件类型与访客标识缺失的脏数据直接丢弃，不污染统计
        if (dto == null || !StringUtils.hasText(dto.getEventType()) || !StringUtils.hasText(dto.getVisitorId())) {
            return;
        }
        try {
            Map<String, String> body = new HashMap<>(16);
            body.put("eventType", dto.getEventType());
            body.put("visitorId", dto.getVisitorId());
            putIfPresent(body, "articleId", dto.getArticleId());
            putIfPresent(body, "tagId", dto.getTagId());
            putIfPresent(body, "collectionId", dto.getCollectionId());
            putIfText(body, "keyword", dto.getKeyword());
            putIfText(body, "pageUrl", dto.getPageUrl());
            putIfText(body, "referrer", dto.getReferrer());
            putIfText(body, "ip", ip);
            putIfText(body, "userAgent", ua);
            putIfPresent(body, "duration", dto.getDuration());
            body.put("occurredAt", Long.toString(occurredAt));
            // 事件唯一ID，供消费端去重（防 Stream 重放导致重复计数）
            body.put("eventId", java.util.UUID.randomUUID().toString());
            stringRedisTemplate.opsForStream().add(RedisKeys.EVENT_STREAM, body);
        } catch (Exception e) {
            // Stream 不可用时丢弃事件，不影响前台浏览（fire-and-forget）
            log.warn("写入埋点事件流失败，丢弃事件 type={}: {}", dto.getEventType(), e.getMessage());
        }
    }

    private void putIfPresent(Map<String, String> body, String key, Object value) {
        if (value != null) {
            body.put(key, value.toString());
        }
    }

    private void putIfText(Map<String, String> body, String key, String value) {
        if (StringUtils.hasText(value)) {
            body.put(key, value);
        }
    }

    private String userAgent(HttpServletRequest request) {
        return UserAgentUtil.truncate(request.getHeader("User-Agent"), MAX_UA_LENGTH);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }
}
