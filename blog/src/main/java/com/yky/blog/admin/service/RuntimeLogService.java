package com.yky.blog.admin.service;

import com.yky.blog.admin.vo.RuntimeLogApiMetricVO;
import com.yky.blog.admin.vo.RuntimeLogLineVO;
import com.yky.blog.admin.vo.RuntimeLogSlowRequestVO;
import com.yky.blog.admin.vo.RuntimeLogSummaryVO;

import java.util.List;

public interface RuntimeLogService {

    List<RuntimeLogLineVO> recent(int lines, String level);

    List<RuntimeLogLineVO> search(String keyword, String level, int lines);

    RuntimeLogSummaryVO summary(long slowThreshold);

    List<RuntimeLogSlowRequestVO> slowRequests(long threshold, int limit);

    List<RuntimeLogApiMetricVO> topApis(int limit);
}
