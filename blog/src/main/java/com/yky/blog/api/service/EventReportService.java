package com.yky.blog.api.service;

import com.yky.blog.api.dto.EventReportDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface EventReportService {

    /** 单条事件上报，写入 Redis Stream 后立即返回。 */
    void report(EventReportDTO dto, HttpServletRequest request);

    /** 批量事件上报（前端攒批 + sendBeacon），逐条写入 Redis Stream。 */
    void reportBatch(List<EventReportDTO> events, HttpServletRequest request);
}
