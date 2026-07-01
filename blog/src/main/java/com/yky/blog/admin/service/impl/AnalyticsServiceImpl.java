package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.service.AnalyticsAggregationService;
import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.ArticleTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.common.mapper.ArticleDailyStatMapper;
import com.yky.blog.common.mapper.CollectionDailyStatMapper;
import com.yky.blog.common.mapper.EventDailyStatMapper;
import com.yky.blog.common.mapper.SearchKeywordStatMapper;
import com.yky.blog.common.mapper.TagDailyStatMapper;
import com.yky.blog.common.redis.RedisCacheService;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String PAGE_VIEW = "page_view";
    private static final Duration ANALYTICS_CACHE_TTL = Duration.ofSeconds(30);

    private final AnalyticsAggregationService analyticsAggregationService;
    private final EventDailyStatMapper eventDailyStatMapper;
    private final ArticleDailyStatMapper articleDailyStatMapper;
    private final TagDailyStatMapper tagDailyStatMapper;
    private final CollectionDailyStatMapper collectionDailyStatMapper;
    private final SearchKeywordStatMapper searchKeywordStatMapper;
    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public AnalyticsOverviewVO overview() {
        JavaType type = objectMapper.getTypeFactory().constructType(AnalyticsOverviewVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:overview"), type, ANALYTICS_CACHE_TTL, this::loadOverview);
    }

    private AnalyticsOverviewVO loadOverview() {
        LocalDate today = LocalDate.now();
        LocalDate epochDate = LocalDate.of(1970, 1, 1);
        analyticsAggregationService.aggregateDate(today);

        AnalyticsOverviewVO vo = new AnalyticsOverviewVO();
        vo.setTodayPv(safeCount(eventDailyStatMapper.sumPv(PAGE_VIEW, today, today)));
        vo.setTodayUv(safeCount(eventDailyStatMapper.sumUv(PAGE_VIEW, today, today)));
        vo.setTotalPv(safeCount(eventDailyStatMapper.sumPv(PAGE_VIEW, epochDate, today)));
        vo.setArticleViewCount(safeCount(articleDailyStatMapper.sumViewCount(epochDate, today)));
        return vo;
    }

    @Override
    public List<AnalyticsTrendVO> trend(int days) {
        int range = days <= 0 ? 7 : Math.min(days, 30);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, AnalyticsTrendVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:trend:" + range), type, ANALYTICS_CACHE_TTL,
                () -> loadTrend(range));
    }

    private List<AnalyticsTrendVO> loadTrend(int range) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);

        Map<String, AnalyticsTrendVO> trendMap = eventDailyStatMapper.listPvUvTrend(startDate, endDate)
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
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, HotArticleVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:hot-articles:" + range + ":" + size),
                type, ANALYTICS_CACHE_TTL, () -> loadHotArticles(range, size));
    }

    private List<HotArticleVO> loadHotArticles(int range, int size) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);
        return articleDailyStatMapper.listHotArticles(startDate, endDate, size);
    }

    @Override
    public List<ArticleTrendVO> articleTrend(int days) {
        int range = days <= 0 ? 7 : Math.min(days, 30);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ArticleTrendVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:article-trend:" + range),
                type, ANALYTICS_CACHE_TTL, () -> loadArticleTrend(range));
    }

    private List<ArticleTrendVO> loadArticleTrend(int range) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);
        Map<String, ArticleTrendVO> trendMap = articleDailyStatMapper.listArticleTrend(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(ArticleTrendVO::getDate, Function.identity()));

        return IntStream.range(0, range)
                .mapToObj(startDate::plusDays)
                .map(date -> {
                    String key = date.toString();
                    ArticleTrendVO existing = trendMap.get(key);
                    if (existing != null) {
                        return existing;
                    }
                    ArticleTrendVO empty = new ArticleTrendVO();
                    empty.setDate(key);
                    empty.setViewCount(0L);
                    empty.setVisitorCount(0L);
                    return empty;
                })
                .toList();
    }

    @Override
    public List<AnalyticsRankVO> hotTags(int days, int limit) {
        int range = days <= 0 ? 7 : Math.min(days, 90);
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, AnalyticsRankVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:hot-tags:" + range + ":" + size),
                type, ANALYTICS_CACHE_TTL, () -> loadHotTags(range, size));
    }

    private List<AnalyticsRankVO> loadHotTags(int range, int size) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);
        return tagDailyStatMapper.listHotTags(startDate, endDate, size);
    }

    @Override
    public List<AnalyticsRankVO> hotCollections(int days, int limit) {
        int range = days <= 0 ? 7 : Math.min(days, 90);
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, AnalyticsRankVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:hot-collections:" + range + ":" + size),
                type, ANALYTICS_CACHE_TTL, () -> loadHotCollections(range, size));
    }

    private List<AnalyticsRankVO> loadHotCollections(int range, int size) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);
        return collectionDailyStatMapper.listHotCollections(startDate, endDate, size);
    }

    @Override
    public List<AnalyticsRankVO> hotKeywords(int days, int limit) {
        int range = days <= 0 ? 7 : Math.min(days, 90);
        int size = limit <= 0 ? 10 : Math.min(limit, 50);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, AnalyticsRankVO.class);
        return redisCacheService.getOrLoad(RedisKeys.cache("analytics:hot-keywords:" + range + ":" + size),
                type, ANALYTICS_CACHE_TTL, () -> loadHotKeywords(range, size));
    }

    private List<AnalyticsRankVO> loadHotKeywords(int range, int size) {
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.aggregateRecentDays(range);
        return searchKeywordStatMapper.listHotKeywords(startDate, endDate, size);
    }

    private Long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
