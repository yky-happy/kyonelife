package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class RuntimeLogApiMetricVO {

    private String method;

    private String path;

    private Long requestCount;

    private Long averageCostTime;

    private Long maxCostTime;
}
