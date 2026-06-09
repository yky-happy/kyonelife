package com.yky.blog.admin.service;

import com.yky.blog.admin.vo.AnalyticsOverviewVO;
import com.yky.blog.admin.vo.AnalyticsTrendVO;
import com.yky.blog.admin.vo.HotArticleVO;

import java.util.List;

public interface AnalyticsService {

    AnalyticsOverviewVO overview();

    List<AnalyticsTrendVO> trend(int days);

    List<HotArticleVO> hotArticles(int days, int limit);
}
