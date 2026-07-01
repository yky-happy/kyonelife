package com.yky.blog.admin.service;

import java.time.LocalDate;

public interface AnalyticsAggregationService {

    void aggregateDate(LocalDate statDate);

    void aggregateRecentDays(int days);
}
