package com.yky.blog.api.service;

import com.yky.blog.api.dto.EventReportDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface EventReportService {

    void report(EventReportDTO dto, HttpServletRequest request);
}
