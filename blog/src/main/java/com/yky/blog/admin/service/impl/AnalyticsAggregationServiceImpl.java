package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.service.AnalyticsAggregationService;
import com.yky.blog.common.mapper.ArticleDailyStatMapper;
import com.yky.blog.common.mapper.CollectionDailyStatMapper;
import com.yky.blog.common.mapper.EventDailyStatMapper;
import com.yky.blog.common.mapper.SearchKeywordStatMapper;
import com.yky.blog.common.mapper.TagDailyStatMapper;
import com.yky.blog.common.redis.DistributedLock;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsAggregationServiceImpl implements AnalyticsAggregationService {

    private final EventDailyStatMapper eventDailyStatMapper;
    private final ArticleDailyStatMapper articleDailyStatMapper;
    private final TagDailyStatMapper tagDailyStatMapper;
    private final CollectionDailyStatMapper collectionDailyStatMapper;
    private final SearchKeywordStatMapper searchKeywordStatMapper;
    private final DistributedLock distributedLock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDate(LocalDate statDate) {
        eventDailyStatMapper.upsertByDate(statDate);
        eventDailyStatMapper.deleteDateWhenNoEvents(statDate);
        articleDailyStatMapper.upsertByDate(statDate);
        articleDailyStatMapper.deleteDateWhenNoArticleViews(statDate);
        tagDailyStatMapper.upsertByDate(statDate);
        tagDailyStatMapper.deleteDateWhenNoTagClicks(statDate);
        collectionDailyStatMapper.upsertByDate(statDate);
        collectionDailyStatMapper.deleteDateWhenNoCollectionClicks(statDate);
        searchKeywordStatMapper.upsertByDate(statDate);
        searchKeywordStatMapper.deleteDateWhenNoSearches(statDate);
    }

    @Override
    public void aggregateRecentDays(int days) {
        int range = days <= 0 ? 1 : Math.min(days, 30);
        LocalDate today = LocalDate.now();
        IntStream.range(0, range)
                .mapToObj(today::minusDays)
                .forEach(this::aggregateDate);
    }

    /**
     * 每 5 分钟聚合今日数据。
     *
     * <p>聚合采用"按日全量重算 + UPSERT"的幂等方式：因为 UV 是 COUNT(DISTINCT visitor_id)，
     * <b>不可跨批次累加</b>，无法做真正的增量聚合，故每次对当天重算保证 UV 正确。
     * 多实例部署时用分布式锁保证只有一个实例执行，避免重复聚合（重复 UPSERT 虽幂等但浪费资源）。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void aggregateCurrentDay() {
        boolean executed = distributedLock.runIfAcquired(
                RedisKeys.lock("analytics-aggregate"), Duration.ofMinutes(4), () -> {
                    try {
                        aggregateDate(LocalDate.now());
                    } catch (Exception e) {
                        log.warn("聚合今日埋点数据失败: {}", e.getMessage());
                    }
                });
        if (!executed) {
            log.debug("未抢到聚合锁，跳过本轮（其它实例正在执行）");
        }
    }
}
