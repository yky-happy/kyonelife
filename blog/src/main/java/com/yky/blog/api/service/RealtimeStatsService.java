package com.yky.blog.api.service;

import com.yky.blog.common.entity.EventLog;

public interface RealtimeStatsService {

    void record(EventLog event);
}
