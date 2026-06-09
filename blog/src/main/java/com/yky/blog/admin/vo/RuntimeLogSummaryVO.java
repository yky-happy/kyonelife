package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class RuntimeLogSummaryVO {

    private Long todayRequestCount;

    private Long warnCount;

    private Long errorCount;

    private Long slowRequestCount;

    private String lastStartTime;

    private String lastShutdownTime;

    private String logFilePath;
}
