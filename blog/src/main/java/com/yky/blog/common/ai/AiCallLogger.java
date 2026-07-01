package com.yky.blog.common.ai;

import com.yky.blog.common.entity.AiCallLog;
import com.yky.blog.common.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * AI 调用日志异步写入器。
 * 日志写库不在业务调用线程内进行，避免拖慢 AI 接口响应；写失败仅告警，不影响主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiCallLogger {

    private final AiCallLogMapper aiCallLogMapper;

    @Async
    public void save(AiCallLog logEntry) {
        try {
            aiCallLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("AI 调用日志写入失败 scene={}: {}", logEntry.getScene(), e.getMessage());
        }
    }
}
