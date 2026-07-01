package com.yky.blog.api.controller;

import com.yky.blog.api.dto.EventReportDTO;
import com.yky.blog.api.service.EventReportService;
import com.yky.blog.common.annotation.RateLimit;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "前台行为埋点")
@Validated
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventReportController {

    private final EventReportService eventReportService;

    @Operation(summary = "上报前台行为事件")
    @RateLimit(name = "event-report", window = 10, limit = 30)
    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody EventReportDTO dto, HttpServletRequest request) {
        eventReportService.report(dto, request);
        return Result.success();
    }

    @Operation(summary = "批量上报前台行为事件（前端攒批 + sendBeacon）")
    @RateLimit(name = "event-report-batch", window = 10, limit = 60)
    @PostMapping("/report/batch")
    public Result<Void> reportBatch(@Valid @Size(max = 50, message = "单次最多上报50条事件")
                                    @RequestBody List<@Valid EventReportDTO> events,
                                    HttpServletRequest request) {
        eventReportService.reportBatch(events, request);
        return Result.success();
    }
}
