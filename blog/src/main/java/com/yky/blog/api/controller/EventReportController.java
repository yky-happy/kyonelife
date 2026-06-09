package com.yky.blog.api.controller;

import com.yky.blog.api.dto.EventReportDTO;
import com.yky.blog.api.service.EventReportService;
import com.yky.blog.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前台行为埋点")
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventReportController {

    private final EventReportService eventReportService;

    @Operation(summary = "上报前台行为事件")
    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody EventReportDTO dto, HttpServletRequest request) {
        eventReportService.report(dto, request);
        return Result.success();
    }
}
