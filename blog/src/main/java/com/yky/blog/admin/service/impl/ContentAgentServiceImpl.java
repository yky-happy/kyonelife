package com.yky.blog.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.admin.dto.AgentDraftDTO;
import com.yky.blog.admin.dto.AgentTopicsDTO;
import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.service.ContentAgentService;
import com.yky.blog.admin.vo.AgentDraftVO;
import com.yky.blog.admin.vo.AgentTopicsVO;
import com.yky.blog.admin.vo.AnalyticsRankVO;
import com.yky.blog.admin.vo.TopicSuggestionVO;
import com.yky.blog.common.ai.AgentConversationMemory;
import com.yky.blog.common.ai.AgentLoopRunner;
import com.yky.blog.common.ai.AiPrompts;
import com.yky.blog.common.ai.ContentAgentTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具型 Agent 实现：把 {@link AgentLoopRunner}（手写循环 + 迭代上限 + step 级日志）
 * 与 {@link ContentAgentTools}（平台数据/写草稿工具）接起来，并负责：
 * <ul>
 *   <li>解析模型最终输出的结构化 JSON（选题列表 / 草稿信息）；</li>
 *   <li>写草稿的 draftId 以工具回填的 holder 为准，不依赖模型复述，确保可靠；</li>
 *   <li>整体失败时<b>降级</b>：选题退回"查热搜词直接拼"，草稿则提示用户手填，后台流程不中断。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAgentServiceImpl implements ContentAgentService {

    private static final String SCENE_TOPICS = "agent-topics";
    private static final String SCENE_DRAFT = "agent-draft";

    private final AgentLoopRunner agentLoopRunner;
    private final ContentAgentTools contentAgentTools;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;
    private final AgentConversationMemory agentConversationMemory;

    @Override
    public AgentTopicsVO suggestTopics(AgentTopicsDTO dto, Object adminId) {
        int days = dto.getDays() == null ? 7 : dto.getDays();
        int count = dto.getCount() == null ? 5 : dto.getCount();
        String userGoal = AiPrompts.topicsGoal(days, count, dto.getDirection());
        String conversationId = agentConversationMemory.conversationId(adminId, SCENE_TOPICS, dto.getSessionId());

        AgentLoopRunner.AgentResult result = agentLoopRunner.run(
                SCENE_TOPICS,
                AiPrompts.TOPICS_SYSTEM,
                userGoal,
                contentAgentTools,
                null,
                agentConversationMemory.recentMessages(conversationId));

        if (result.ok()) {
            List<TopicSuggestionVO> topics = parseTopics(result.text());
            if (!topics.isEmpty()) {
                if (topics.size() > count) {
                    topics = topics.subList(0, count);
                }
                agentConversationMemory.remember(conversationId, userGoal, result.text());
                return new AgentTopicsVO(topics, result.rounds(), result.capped(), false);
            }
            log.warn("Agent[{}] 输出无法解析为选题，降级。原文: {}", SCENE_TOPICS, abbreviate(result.text()));
        }
        // 降级：退回"第一层固定工具调用"——直接查热搜词拼成选题，后台不中断
        return new AgentTopicsVO(fallbackTopics(days, count), result.rounds(), result.capped(), true);
    }

    @Override
    public AgentDraftVO generateDraft(AgentDraftDTO dto, Object adminId) {
        ContentAgentTools.DraftHolder holder = new ContentAgentTools.DraftHolder();
        Map<String, Object> toolContext = Map.of(ContentAgentTools.DRAFT_HOLDER_KEY, holder);
        String userGoal = AiPrompts.draftGoal(dto.getTopic(), dto.getPoints(), dto.getStyle());
        String conversationId = agentConversationMemory.conversationId(adminId, SCENE_DRAFT, dto.getSessionId());

        AgentLoopRunner.AgentResult result = agentLoopRunner.run(
                SCENE_DRAFT,
                AiPrompts.DRAFT_SYSTEM,
                userGoal,
                contentAgentTools,
                toolContext,
                agentConversationMemory.recentMessages(conversationId));

        // 以工具回填的 draftId 为准（可靠），不依赖模型复述
        if (result.ok() && holder.getDraftId() != null) {
            DraftMeta meta = parseDraftMeta(result.text());
            String title = StringUtils.hasText(holder.getTitle()) ? holder.getTitle() : meta.title;
            List<String> tags = !holder.getTags().isEmpty() ? holder.getTags() : meta.tags;
            agentConversationMemory.remember(conversationId, userGoal, result.text());
            return new AgentDraftVO(holder.getDraftId(), title, meta.summary, tags,
                    result.rounds(), result.capped(), false);
        }
        // 降级：未成功落草稿，提示用户手填，不抛错中断后台
        log.warn("Agent[{}] 未能生成草稿（draftId={}, ok={}），降级", SCENE_DRAFT, holder.getDraftId(), result.ok());
        return new AgentDraftVO(null, dto.getTopic(), null, new ArrayList<>(),
                result.rounds(), result.capped(), true);
    }

    // ============================= 结构化输出解析 =============================

    private List<TopicSuggestionVO> parseTopics(String text) {
        String json = extractJson(text, '[', ']');
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            List<TopicSuggestionVO> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("解析选题 JSON 失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private DraftMeta parseDraftMeta(String text) {
        DraftMeta meta = new DraftMeta();
        String json = extractJson(text, '{', '}');
        if (json == null) {
            return meta;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("title")) {
                meta.title = node.get("title").asText();
            }
            if (node.hasNonNull("summary")) {
                meta.summary = node.get("summary").asText();
            }
            if (node.has("tags") && node.get("tags").isArray()) {
                for (JsonNode t : node.get("tags")) {
                    if (StringUtils.hasText(t.asText())) {
                        meta.tags.add(t.asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析草稿 JSON 失败: {}", e.getMessage());
        }
        return meta;
    }

    /** 从模型文本里抠出 JSON：去掉 ``` 代码块围栏，再截取首个 open 到末个 close 之间的片段。 */
    private String extractJson(String text, char open, char close) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String s = text.strip();
        if (s.startsWith("```")) {
            s = s.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("(?s)\\s*```$", "").strip();
        }
        int start = s.indexOf(open);
        int end = s.lastIndexOf(close);
        if (start < 0 || end <= start) {
            return null;
        }
        return s.substring(start, end + 1);
    }

    // ============================= 降级 =============================

    /** 降级选题：直接查近 N 天热搜词，拼成"固定工具调用"式选题（对应文档第二节第 1 步）。 */
    private List<TopicSuggestionVO> fallbackTopics(int days, int count) {
        List<TopicSuggestionVO> topics = new ArrayList<>();
        try {
            List<AnalyticsRankVO> hot = analyticsService.hotKeywords(days, count);
            for (AnalyticsRankVO kw : hot) {
                if (!StringUtils.hasText(kw.getName())) {
                    continue;
                }
                TopicSuggestionVO t = new TopicSuggestionVO();
                t.setTitle("关于「" + kw.getName() + "」的实践与思考");
                t.setReason("基于近 " + days + " 天热门搜索词「" + kw.getName() + "」（搜索量 " + kw.getCount() + "）");
                t.setRefKeywords(List.of(kw.getName()));
                t.setRefArticles(new ArrayList<>());
                topics.add(t);
                if (topics.size() >= count) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("降级选题也失败: {}", e.getMessage());
        }
        return topics;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    /** 草稿元信息载体。 */
    private static class DraftMeta {
        private String title;
        private String summary;
        private final List<String> tags = new ArrayList<>();
    }
}
