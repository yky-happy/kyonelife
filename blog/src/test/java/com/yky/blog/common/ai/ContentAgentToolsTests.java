package com.yky.blog.common.ai;

import com.yky.blog.admin.dto.ArticleSaveDTO;
import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.service.ArticleService;
import com.yky.blog.admin.service.TagService;
import com.yky.blog.common.util.SensitiveWordFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentAgentToolsTests {

    @Test
    void saveArticleDraftShouldFilterSensitiveWordsAndForceDraftStatus() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        ArticleService articleService = mock(ArticleService.class);
        TagService tagService = mock(TagService.class);
        ContentAgentTools tools = new ContentAgentTools(
                analyticsService, articleService, tagService, new SensitiveWordFilter());
        ContentAgentTools.DraftHolder holder = new ContentAgentTools.DraftHolder();
        ToolContext context = new ToolContext(Map.of(ContentAgentTools.DRAFT_HOLDER_KEY, holder));

        when(articleService.saveArticle(any(ArticleSaveDTO.class))).thenReturn(42L);

        Long draftId = tools.saveArticleDraft(
                "Redis 加微信 实战",
                "正文包含 赌博 和 Redis 内容",
                List.of(),
                context);

        ArgumentCaptor<ArticleSaveDTO> captor = ArgumentCaptor.forClass(ArticleSaveDTO.class);
        verify(articleService).saveArticle(captor.capture());
        ArticleSaveDTO dto = captor.getValue();
        assertThat(draftId).isEqualTo(42L);
        assertThat(dto.getStatus()).isZero();
        assertThat(dto.getTitle()).isEqualTo("Redis *** 实战");
        assertThat(dto.getContentMd()).contains("**").doesNotContain("赌博");
        assertThat(holder.getDraftId()).isEqualTo(42L);
        assertThat(holder.getTitle()).isEqualTo("Redis *** 实战");
    }
}
