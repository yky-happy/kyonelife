package com.yky.blog.admin.vo;

import lombok.Data;

@Data
public class AnalyticsOverviewVO {

    private Long todayPv;

    private Long todayUv;

    private Long totalPv;

    private Long articleViewCount;
}
