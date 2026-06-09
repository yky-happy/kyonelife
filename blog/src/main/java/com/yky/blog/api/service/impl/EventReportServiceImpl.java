package com.yky.blog.api.service.impl;

import com.yky.blog.api.dto.EventReportDTO;
import com.yky.blog.api.service.EventReportService;
import com.yky.blog.common.entity.EventLog;
import com.yky.blog.common.mapper.EventLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReportServiceImpl implements EventReportService {

    private final EventLogMapper eventLogMapper;

    @Async
    @Override
    public void report(EventReportDTO dto, HttpServletRequest request) {
        try {
            EventLog eventLog = new EventLog();
            eventLog.setEventType(dto.getEventType());
            eventLog.setVisitorId(dto.getVisitorId());
            eventLog.setArticleId(dto.getArticleId());
            eventLog.setTagId(dto.getTagId());
            eventLog.setCollectionId(dto.getCollectionId());
            eventLog.setKeyword(dto.getKeyword());
            eventLog.setPageUrl(dto.getPageUrl());
            eventLog.setReferrer(dto.getReferrer());
            eventLog.setIp(getClientIp(request));
            eventLog.setUserAgent(truncate(request.getHeader("User-Agent"), 1000));
            eventLog.setDevice(resolveDevice(eventLog.getUserAgent()));
            eventLog.setBrowser(resolveBrowser(eventLog.getUserAgent()));
            eventLog.setOs(resolveOs(eventLog.getUserAgent()));
            eventLog.setDuration(dto.getDuration() == null ? 0L : Math.max(dto.getDuration(), 0L));
            eventLog.setCreateTime(LocalDateTime.now());
            eventLogMapper.insert(eventLog);
        } catch (Exception e) {
            log.warn("保存前台埋点失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }

    private String resolveDevice(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "mobile";
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "tablet";
        }
        return "desktop";
    }

    private String resolveBrowser(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "Other";
    }

    private String resolveOs(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
