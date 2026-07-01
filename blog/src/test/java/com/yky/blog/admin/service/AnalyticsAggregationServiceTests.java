package com.yky.blog.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yky.blog.common.entity.ArticleDailyStat;
import com.yky.blog.common.entity.CollectionDailyStat;
import com.yky.blog.common.entity.EventDailyStat;
import com.yky.blog.common.entity.EventLog;
import com.yky.blog.common.entity.SearchKeywordStat;
import com.yky.blog.common.entity.TagDailyStat;
import com.yky.blog.common.mapper.ArticleDailyStatMapper;
import com.yky.blog.common.mapper.CollectionDailyStatMapper;
import com.yky.blog.common.mapper.EventDailyStatMapper;
import com.yky.blog.common.mapper.EventLogMapper;
import com.yky.blog.common.mapper.SearchKeywordStatMapper;
import com.yky.blog.common.mapper.TagDailyStatMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnalyticsAggregationServiceTests {

    private static final LocalDate STAT_DATE = LocalDate.of(2099, 1, 1);
    private static final LocalDateTime STAT_TIME = STAT_DATE.atTime(10, 0);
    private static final String VISITOR_PREFIX = "aggregation-test-";
    private static final Long ARTICLE_ID = 999999L;
    private static final Long TAG_ID = 999998L;
    private static final Long COLLECTION_ID = 999997L;
    private static final String KEYWORD = "aggregation-keyword";

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @Autowired
    private EventLogMapper eventLogMapper;

    @Autowired
    private EventDailyStatMapper eventDailyStatMapper;

    @Autowired
    private ArticleDailyStatMapper articleDailyStatMapper;

    @Autowired
    private TagDailyStatMapper tagDailyStatMapper;

    @Autowired
    private CollectionDailyStatMapper collectionDailyStatMapper;

    @Autowired
    private SearchKeywordStatMapper searchKeywordStatMapper;

    @AfterEach
    void cleanUp() {
        eventLogMapper.delete(new LambdaQueryWrapper<EventLog>()
                .likeRight(EventLog::getVisitorId, VISITOR_PREFIX));
        eventDailyStatMapper.delete(new LambdaQueryWrapper<EventDailyStat>()
                .eq(EventDailyStat::getStatDate, STAT_DATE));
        articleDailyStatMapper.delete(new LambdaQueryWrapper<ArticleDailyStat>()
                .eq(ArticleDailyStat::getStatDate, STAT_DATE));
        tagDailyStatMapper.delete(new LambdaQueryWrapper<TagDailyStat>()
                .eq(TagDailyStat::getStatDate, STAT_DATE));
        collectionDailyStatMapper.delete(new LambdaQueryWrapper<CollectionDailyStat>()
                .eq(CollectionDailyStat::getStatDate, STAT_DATE));
        searchKeywordStatMapper.delete(new LambdaQueryWrapper<SearchKeywordStat>()
                .eq(SearchKeywordStat::getStatDate, STAT_DATE));
    }

    @Test
    void aggregateDateShouldWriteEventAndArticleDailyStatsIdempotently() {
        insertEvent("page_view", VISITOR_PREFIX + "a", null, 1000L);
        insertEvent("page_view", VISITOR_PREFIX + "a", null, 2000L);
        insertEvent("page_view", VISITOR_PREFIX + "b", null, 3000L);
        insertEvent("article_view", VISITOR_PREFIX + "a", ARTICLE_ID, 4000L);
        insertEvent("article_view", VISITOR_PREFIX + "b", ARTICLE_ID, 5000L);
        insertEvent("tag_click", VISITOR_PREFIX + "a", null, TAG_ID, null, null, 0L);
        insertEvent("tag_click", VISITOR_PREFIX + "b", null, TAG_ID, null, null, 0L);
        insertEvent("collection_click", VISITOR_PREFIX + "a", null, null, COLLECTION_ID, null, 0L);
        insertEvent("search", VISITOR_PREFIX + "a", null, null, null, KEYWORD, 0L);
        insertEvent("search", VISITOR_PREFIX + "b", null, null, null, KEYWORD, 0L);

        analyticsAggregationService.aggregateDate(STAT_DATE);
        analyticsAggregationService.aggregateDate(STAT_DATE);

        EventDailyStat pageViewStat = eventDailyStatMapper.selectOne(new LambdaQueryWrapper<EventDailyStat>()
                .eq(EventDailyStat::getStatDate, STAT_DATE)
                .eq(EventDailyStat::getEventType, "page_view"));
        EventDailyStat articleViewStat = eventDailyStatMapper.selectOne(new LambdaQueryWrapper<EventDailyStat>()
                .eq(EventDailyStat::getStatDate, STAT_DATE)
                .eq(EventDailyStat::getEventType, "article_view"));
        ArticleDailyStat articleStat = articleDailyStatMapper.selectOne(new LambdaQueryWrapper<ArticleDailyStat>()
                .eq(ArticleDailyStat::getStatDate, STAT_DATE)
                .eq(ArticleDailyStat::getArticleId, ARTICLE_ID));
        TagDailyStat tagStat = tagDailyStatMapper.selectOne(new LambdaQueryWrapper<TagDailyStat>()
                .eq(TagDailyStat::getStatDate, STAT_DATE)
                .eq(TagDailyStat::getTagId, TAG_ID));
        CollectionDailyStat collectionStat = collectionDailyStatMapper.selectOne(new LambdaQueryWrapper<CollectionDailyStat>()
                .eq(CollectionDailyStat::getStatDate, STAT_DATE)
                .eq(CollectionDailyStat::getCollectionId, COLLECTION_ID));
        SearchKeywordStat keywordStat = searchKeywordStatMapper.selectOne(new LambdaQueryWrapper<SearchKeywordStat>()
                .eq(SearchKeywordStat::getStatDate, STAT_DATE)
                .eq(SearchKeywordStat::getKeyword, KEYWORD));

        assertThat(pageViewStat).isNotNull();
        assertThat(pageViewStat.getPv()).isEqualTo(3L);
        assertThat(pageViewStat.getUv()).isEqualTo(2L);
        assertThat(pageViewStat.getDurationTotal()).isEqualTo(6000L);

        assertThat(articleViewStat).isNotNull();
        assertThat(articleViewStat.getPv()).isEqualTo(2L);
        assertThat(articleViewStat.getUv()).isEqualTo(2L);

        assertThat(articleStat).isNotNull();
        assertThat(articleStat.getViewCount()).isEqualTo(2L);
        assertThat(articleStat.getVisitorCount()).isEqualTo(2L);
        assertThat(articleStat.getDurationTotal()).isEqualTo(9000L);

        assertThat(tagStat).isNotNull();
        assertThat(tagStat.getClickCount()).isEqualTo(2L);
        assertThat(tagStat.getVisitorCount()).isEqualTo(2L);

        assertThat(collectionStat).isNotNull();
        assertThat(collectionStat.getClickCount()).isEqualTo(1L);
        assertThat(collectionStat.getVisitorCount()).isEqualTo(1L);

        assertThat(keywordStat).isNotNull();
        assertThat(keywordStat.getSearchCount()).isEqualTo(2L);
        assertThat(keywordStat.getVisitorCount()).isEqualTo(2L);
    }

    private void insertEvent(String eventType, String visitorId, Long articleId, Long duration) {
        insertEvent(eventType, visitorId, articleId, null, null, null, duration);
    }

    private void insertEvent(String eventType, String visitorId, Long articleId, Long tagId,
                             Long collectionId, String keyword, Long duration) {
        EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        eventLog.setVisitorId(visitorId);
        eventLog.setArticleId(articleId);
        eventLog.setTagId(tagId);
        eventLog.setCollectionId(collectionId);
        eventLog.setKeyword(keyword);
        eventLog.setPageUrl("/aggregation-test");
        eventLog.setDuration(duration);
        eventLog.setCreateTime(STAT_TIME);
        eventLogMapper.insert(eventLog);
    }
}
