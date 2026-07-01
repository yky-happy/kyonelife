package com.yky.blog.common.ai;

import com.yky.blog.admin.dto.ArticleSaveDTO;
import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.service.ArticleService;
import com.yky.blog.admin.service.TagService;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.admin.vo.ArticleVO;
import com.yky.blog.admin.vo.HotArticleVO;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.util.SensitiveWordFilter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内容创作 Agent 的工具集（Tool Map）。
 *
 * <p>把项目<b>已有的</b> service 能力封装成模型可调用的工具：读工具安全、可随意暴露；
 * 写工具只暴露 {@code save_article_draft} 一个、且强制只落草稿（{@code status=0}），
 * 绝不暴露删除/发布类危险工具——这是护栏与"写工具隔离"控制点的落地。</p>
 *
 * <p>各方法上限参数都做了夹紧（clamp），既防模型传入异常值，也间接控制单次工具的开销。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAgentTools {

    /** 写工具回填 holder 在 toolContext 中的 key。 */
    public static final String DRAFT_HOLDER_KEY = "draftHolder";

    private final AnalyticsService analyticsService;
    private final ArticleService articleService;
    private final TagService tagService;
    private final SensitiveWordFilter sensitiveWordFilter;

    // ============================= 读工具 =============================

    @Tool(description = "查询最近 N 天的热门搜索关键词及搜索次数，用于选题参考")
    public List<AnalyticsRankVO> getHotKeywords(
            @ToolParam(description = "统计天数，如 7") int days,
            @ToolParam(description = "返回条数，如 10") int limit) {
        return analyticsService.hotKeywords(clampDays(days), clampLimit(limit));
    }

    @Tool(description = "查询最近 N 天的热门文章（标题+浏览量），可用于选题参考与去重")
    public List<HotArticleVO> getHotArticles(
            @ToolParam(description = "统计天数，如 7") int days,
            @ToolParam(description = "返回条数，如 10") int limit) {
        return analyticsService.hotArticles(clampDays(days), clampLimit(limit));
    }

    @Tool(description = "查询最近 N 天的热门标签点击排行，用于选题方向参考")
    public List<AnalyticsRankVO> getHotTags(
            @ToolParam(description = "统计天数，如 7") int days,
            @ToolParam(description = "返回条数，如 10") int limit) {
        return analyticsService.hotTags(clampDays(days), clampLimit(limit));
    }

    @Tool(description = "查询已有文章标题列表（最新若干篇），用于选题去重，避免和站内已有内容撞题")
    public List<String> getRecentArticles(
            @ToolParam(description = "返回条数，如 20") int limit) {
        return articleService.pageArticle(1, clampLimit(limit), null, null)
                .getRecords().stream()
                .map(ArticleVO::getTitle)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    @Tool(description = "查询已有标签库（标签名列表），写草稿时优先复用其中的标签")
    public List<String> listTags() {
        return existingTags().keySet().stream().toList();
    }

    // ============================= 写工具（隔离 + 只落草稿） =============================

    @Tool(description = "把生成的文章保存为草稿，返回草稿ID。仅保存为草稿(status=0)，绝不发布")
    public Long saveArticleDraft(
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "Markdown 正文") String contentMd,
            @ToolParam(description = "标签名列表（尽量复用已有标签）") List<String> tags,
            ToolContext toolContext) {
        ArticleSaveDTO dto = new ArticleSaveDTO();
        String filteredTitle = sensitiveWordFilter.filter(title);
        String filteredContent = sensitiveWordFilter.filter(contentMd);
        dto.setTitle(filteredTitle);
        dto.setContentMd(filteredContent);
        dto.setContent(filteredContent); // 暂用 md 充当 html，作者后台可再渲染
        dto.setStatus(0);              // 关键：只落草稿，永不直接发布
        dto.setIsOriginal(1);
        List<Long> tagIds = mapTagNamesToIds(tags);
        dto.setTagIds(tagIds);

        Long draftId = articleService.saveArticle(dto);

        // 回填 holder，让 Service 层拿到可靠的 draftId（不依赖模型自己复述）
        DraftHolder holder = holderOf(toolContext);
        if (holder != null) {
            holder.setDraftId(draftId);
            holder.setTitle(filteredTitle);
            holder.setTags(tags == null ? new ArrayList<>() : new ArrayList<>(tags));
        }
        log.info("Agent 保存草稿成功 draftId={} title={}", draftId, filteredTitle);
        return draftId;
    }

    // ============================= 工具方法 =============================

    /** 标签名 → 已有标签ID；库里没有的名字直接忽略（不在工具里自动建标签，避免污染标签库）。 */
    private List<Long> mapTagNamesToIds(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Long> existing = existingTags();
        List<Long> ids = new ArrayList<>();
        for (String name : tags) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Long id = existing.get(name.strip());
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** 已有标签：name → id（保持插入顺序）。 */
    private Map<String, Long> existingTags() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Tag t : tagService.list()) {
            if (StringUtils.hasText(t.getName())) {
                map.putIfAbsent(t.getName(), t.getId());
            }
        }
        return map;
    }

    private DraftHolder holderOf(ToolContext ctx) {
        if (ctx == null || ctx.getContext() == null) {
            return null;
        }
        Object h = ctx.getContext().get(DRAFT_HOLDER_KEY);
        return h instanceof DraftHolder dh ? dh : null;
    }

    private static int clampDays(int days) {
        return Math.min(90, Math.max(1, days));
    }

    private static int clampLimit(int limit) {
        return Math.min(50, Math.max(1, limit));
    }

    /** 写工具回填载体：把可靠的草稿信息带回 Service 层。 */
    @Data
    public static class DraftHolder {
        private Long draftId;
        private String title;
        private List<String> tags = new ArrayList<>();
    }
}
