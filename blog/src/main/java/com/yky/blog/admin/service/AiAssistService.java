package com.yky.blog.admin.service;

import com.yky.blog.admin.dto.AiAssistDTO;
import com.yky.blog.admin.vo.AiSummaryVO;
import com.yky.blog.admin.vo.AiTagsVO;

/**
 * AI 内容辅助：基于文章正文生成摘要、推荐标签。
 * 统一具备结果缓存（按内容 hash 复用、省 token）与失败降级能力。
 */
public interface AiAssistService {

    /** 生成文章摘要。 */
    AiSummaryVO generateSummary(AiAssistDTO dto);

    /** 结合已有标签库推荐标签。 */
    AiTagsVO recommendTags(AiAssistDTO dto);
}
