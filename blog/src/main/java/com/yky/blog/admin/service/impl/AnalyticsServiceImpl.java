package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.yky.blog.common.mapper.EventLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String PAGE_VIEW = "page_view";
    private static final String ARTICLE_VIEW = "article_view";

    private final EventLogMapper eventLogMapper;

    @Override
    public AnalyticsOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime epochStart = LocalDate.of(1970, 1, 1).atStartOfDay();

        AnalyticsOverviewVO vo = new AnalyticsOverviewVO();
        vo.setTodayPv(safeCount(eventLogMapper.countByEventTypeBetween(PAGE_VIEW, todayStart, tomorrowStart)));
        vo.setTodayUv(safeCount(eventLogMapper.countUvByEventTypeBetween(PAGE_VIEW, todayStart, tomorrowStart)));
        vo.setTotalPv(safeCount(eventLogMapper.countByEventTypeBetween(PAGE_VIEW, epochStart, tomorrowStart)));
        vo.setArticleViewCount(safeCount(eventLogMapper.countByEventTypeBetween(ARTICLE_VIEW, epochStart, tomorrowStart)));
        return vo;
    }

    @Override
    public List<AnalyticsTrendVO> trend(int days) {
        int range = days <= 0 ? 7 : Math.min(days, 30);
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = LocalDate.now().plusDays(1).atStartOfDay();
        Map<String, AnalyticsTrendVO> trendMap = eventLogMapper.listPvUvTrend(startTime, endTime)
                .stream()
                .collect(Collectors.toMap(AnalyticsTrendVO::getDate, Function.identity()));

        return IntStream.range(0, range)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    String key = date.toString();
                    AnalyticsTrendVO existing = trendMap.get(key);
                    if (existing != null) {
                        return existing;
                    }
                    AnalyticsTrendVO empty = new AnalyticsTrendVO();
                    empty.setDate(key);
                    empty.setPv(0L);
                    empty.setUv(0L);
                    return empty;
                })
                .toList();
    }

    @Override
    public List<HotArticleVO> hotArticles(int days, int limit) {
        int range = days <= 0 ? 7 : Math.min(days, 90);
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        LocalDateTime startTime = LocalDate.now().minusDays(range - 1L).atStartOfDay();
        LocalDateTime endTime = LocalDate.now().plusDays(1).atStartOfDay();
        return eventLogMapper.listHotArticles(startTime, endTime, size);
    }

    private Long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
