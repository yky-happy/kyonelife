package com.yky.blog.api.stream;

import com.yky.blog.api.service.RealtimeStatsService;
import com.yky.blog.common.entity.EventLog;
import com.yky.blog.common.mapper.EventLogMapper;
import com.yky.blog.common.redis.RedisKeys;
import com.yky.blog.common.util.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 埋点事件流消费者（Redis Stream 削峰的消费端）。
 *
 * <p>独立后台线程通过消费组 {@code XREADGROUP} <b>攒批拉取</b>事件，
 * 解析 → <b>批量落库</b> event_log → 更新 Redis 实时统计 → <b>XACK</b>。
 * <ul>
 *   <li><b>削峰填谷</b>：突发流量先堆在 Stream 里，消费端按自己节奏批量消费，保护 MySQL。</li>
 *   <li><b>不丢数据</b>：Stream 持久化 + 消费组 ACK 机制，未确认消息保留为 pending；
 *       启动时先 drain 本消费者的 pending，恢复上次崩溃未处理的消息（至少一次语义）。</li>
 *   <li><b>降低写放大</b>：单条多值 INSERT 批量落库，替代原来"每事件一条 INSERT"。</li>
 * </ul>
 *
 * <p>注：消费者名固定为 {@code consumer-main}，单实例足够；多实例部署需为每个实例配置
 * 不同消费者名（同组内并行消费、各自维护 pending）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStreamConsumer implements SmartLifecycle {

    private static final int BATCH_SIZE = 100;
    /** 空轮询时的休眠间隔（毫秒）。用非阻塞读 + sleep，避免阻塞读占用 Lettuce 共享连接。 */
    private static final long IDLE_SLEEP_MILLIS = 500L;
    private static final String CONSUMER_NAME = "consumer-main";
    /** 事件流近似最大长度，定时裁剪回收已消费消息内存。 */
    private static final long MAX_STREAM_LEN = 1_000_000L;

    private final StringRedisTemplate redisTemplate;
    private final EventLogMapper eventLogMapper;
    private final RealtimeStatsService realtimeStatsService;
    private final com.yky.blog.common.util.IpLocationService ipLocationService;

    private volatile boolean running = false;
    private ExecutorService executor;

    @Override
    public void start() {
        ensureGroup();
        running = true;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "event-stream-consumer");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::consume);
        log.info("埋点事件流消费者已启动 stream={} group={}", RedisKeys.EVENT_STREAM, RedisKeys.EVENT_STREAM_GROUP);
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private StreamOperations<String, String, String> streamOps() {
        return redisTemplate.opsForStream();
    }

    /** 创建消费组（含 MKSTREAM），已存在(BUSYGROUP)时忽略。 */
    private void ensureGroup() {
        try {
            streamOps().createGroup(RedisKeys.EVENT_STREAM, ReadOffset.from("0"), RedisKeys.EVENT_STREAM_GROUP);
        } catch (Exception e) {
            log.debug("创建埋点消费组结果（可能已存在）: {}", e.getMessage());
        }
    }

    private void consume() {
        // 启动先恢复本消费者上次未 ACK 的 pending 消息（崩溃恢复）
        drainPending();
        while (running) {
            try {
                List<MapRecord<String, String, String>> records = streamOps().read(
                        Consumer.from(RedisKeys.EVENT_STREAM_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_SIZE),
                        StreamOffset.create(RedisKeys.EVENT_STREAM, ReadOffset.lastConsumed()));
                if (records != null && !records.isEmpty()) {
                    process(records);
                } else {
                    sleep(IDLE_SLEEP_MILLIS);
                }
            } catch (Exception e) {
                if (running) {
                    log.warn("消费埋点事件流异常，稍后重试: {}", e.getMessage());
                    ensureGroup();
                    sleep(1000);
                }
            }
        }
    }

    /** 读取并处理本消费者已投递但未 ACK 的消息（id 从 0 开始即 pending）。 */
    private void drainPending() {
        try {
            while (running) {
                List<MapRecord<String, String, String>> pending = streamOps().read(
                        Consumer.from(RedisKeys.EVENT_STREAM_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_SIZE),
                        StreamOffset.create(RedisKeys.EVENT_STREAM, ReadOffset.from("0")));
                if (pending == null || pending.isEmpty()) {
                    break;
                }
                process(pending);
            }
        } catch (Exception e) {
            log.warn("恢复未确认埋点消息失败: {}", e.getMessage());
        }
    }

    private void process(List<MapRecord<String, String, String>> records) {
        List<EventLog> logs = new ArrayList<>(records.size());
        List<String> eventIds = new ArrayList<>(records.size());
        for (MapRecord<String, String, String> record : records) {
            try {
                Map<String, String> map = record.getValue();
                String eventId = map.get("eventId");
                // 去重：已成功处理过的事件（Stream 重放）直接跳过，实现近似精确一次
                if (eventId != null && isProcessed(eventId)) {
                    continue;
                }
                logs.add(toEventLog(map));
                eventIds.add(eventId);
            } catch (Exception e) {
                log.warn("解析埋点事件失败，跳过: {}", e.getMessage());
            }
        }
        if (!logs.isEmpty()) {
            // 批量落库失败则不 ACK，消息保留为 pending，待重启后 drainPending 重试（至少一次）
            if (!batchInsertWithRetry(logs)) {
                return;
            }
            // 落库成功后再标记已处理：保证失败可重试、不会因去重而丢数据
            markProcessed(eventIds);
            for (EventLog event : logs) {
                realtimeStatsService.record(event);
            }
        }
        ack(records);
    }

    /** 事件是否已成功处理过（去重）。 */
    private boolean isProcessed(String eventId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.eventDedup(eventId)));
        } catch (Exception e) {
            return false;
        }
    }

    /** 标记一批事件已处理，TTL 覆盖崩溃重放窗口即可。 */
    private void markProcessed(List<String> eventIds) {
        for (String eventId : eventIds) {
            if (eventId == null) {
                continue;
            }
            try {
                redisTemplate.opsForValue().set(RedisKeys.eventDedup(eventId), "1", java.time.Duration.ofMinutes(10));
            } catch (Exception ignored) {
                // 标记失败最多导致一次重复处理，可接受
            }
        }
    }

    private boolean batchInsertWithRetry(List<EventLog> logs) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                eventLogMapper.insertBatch(logs);
                return true;
            } catch (Exception e) {
                log.warn("批量写入埋点明细失败（第{}次，共{}条）: {}", attempt, logs.size(), e.getMessage());
                sleep(200);
            }
        }
        return false;
    }

    private void ack(List<MapRecord<String, String, String>> records) {
        try {
            String[] ids = records.stream()
                    .map(record -> record.getId().getValue())
                    .toArray(String[]::new);
            streamOps().acknowledge(RedisKeys.EVENT_STREAM, RedisKeys.EVENT_STREAM_GROUP, ids);
        } catch (Exception e) {
            log.warn("ACK 埋点消息失败: {}", e.getMessage());
        }
    }

    /** 每 10 分钟近似裁剪事件流长度，回收已消费消息占用的内存。 */
    @Scheduled(fixedDelay = 600_000L)
    public void trimStream() {
        try {
            streamOps().trim(RedisKeys.EVENT_STREAM, MAX_STREAM_LEN, true);
        } catch (Exception e) {
            log.warn("裁剪埋点事件流失败: {}", e.getMessage());
        }
    }

    private EventLog toEventLog(Map<String, String> map) {
        EventLog event = new EventLog();
        event.setEventType(map.get("eventType"));
        event.setVisitorId(map.get("visitorId"));
        event.setArticleId(parseLong(map.get("articleId")));
        event.setTagId(parseLong(map.get("tagId")));
        event.setCollectionId(parseLong(map.get("collectionId")));
        event.setKeyword(map.get("keyword"));
        event.setPageUrl(map.get("pageUrl"));
        event.setReferrer(map.get("referrer"));
        event.setIp(map.get("ip"));
        event.setIpLocation(ipLocationService.resolve(map.get("ip")));
        String ua = map.get("userAgent");
        event.setUserAgent(ua);
        event.setDevice(UserAgentUtil.device(ua));
        event.setBrowser(UserAgentUtil.browser(ua));
        event.setOs(UserAgentUtil.os(ua));
        Long duration = parseLong(map.get("duration"));
        event.setDuration(duration == null ? 0L : Math.max(duration, 0L));
        Long occurredAt = parseLong(map.get("occurredAt"));
        event.setCreateTime(occurredAt == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(occurredAt), ZoneId.systemDefault()));
        return event;
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
