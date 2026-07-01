package com.yky.blog.admin.service;

import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.ArticleTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;

import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewVO overview();

    List<AnalyticsTrendVO> trend(int days);

    List<HotArticleVO> hotArticles(int days, int limit);

    List<ArticleTrendVO> articleTrend(int days);

    List<AnalyticsRankVO> hotTags(int days, int limit);

    List<AnalyticsRankVO> hotCollections(int days, int limit);

    List<AnalyticsRankVO> hotKeywords(int days, int limit);
}
