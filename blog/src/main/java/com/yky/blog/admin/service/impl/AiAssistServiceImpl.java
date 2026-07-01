package com.yky.blog.admin.service.impl;

import com.yky.blog.admin.dto.AiAssistDTO;
import com.yky.blog.admin.service.AiAssistService;
import com.yky.blog.admin.service.TagService;
import com.yky.blog.admin.vo.AiSummaryVO;
import com.yky.blog.admin.vo.AiTagsVO;
import com.yky.blog.common.ai.AiCallLogger;
import com.yky.blog.common.ai.AiPrompts;
import com.yky.blog.common.entity.AiCallLog;
import com.yky.blog.common.entity.Tag;
import com.yky.blog.common.exception.BizException;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 内容辅助实现。
 *
 * <p>统一调用流程：先查 Redis 缓存（按输入内容 hash 复用，命中即返回、省 token）；
 * 未命中则调用 DeepSeek，成功后回填缓存；任何失败（超时、限流、Key 失效、模型异常）都
 * <b>降级</b>为本地规则结果，保证后台编辑流程不中断。每次调用都异步落 ai_call_log 便于成本观测。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistServiceImpl implements AiAssistService {

    private static final String SCENE_SUMMARY = "summary";
    private static final String SCENE_TAGS = "tags";
    /** 摘要最大长度，留足余量不超过文章摘要字段(500)。 */
    private static final int MAX_SUMMARY_LEN = 200;
    /** 送入模型的正文上限，控制 token 成本（截断不影响摘要/标签质量）。 */
    private static final int MAX_INPUT_LEN = 4000;
    /** AI 结果缓存时长：按内容 hash 命名，内容不变即可长期复用。 */
    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private static final int MAX_TAGS = 5;
    private static final int MAX_TAG_LEN = 12;

    private final ChatClient aiChatClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiCallLogger aiCallLogger;
    private final TagService tagService;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}")
    private String model;

    @Override
    public AiSummaryVO generateSummary(AiAssistDTO dto) {
        String content = requireContent(dto.getContentMd());
        String input = truncate(content, MAX_INPUT_LEN);
        String hash = sha256(SCENE_SUMMARY + "|" + nullToEmpty(dto.getTitle()) + "|" + input);
        String cacheKey = RedisKeys.cache("ai:summary:" + hash);

        String cached = readCache(cacheKey);
        if (cached != null) {
            recordLog(SCENE_SUMMARY, hash, true, true, 0L, null, null);
            return new AiSummaryVO(cached, false, true);
        }

        long start = System.currentTimeMillis();
        try {
            ChatResponse resp = aiChatClient.prompt()
                    .system(AiPrompts.SUMMARY_SYSTEM)
                    .user(AiPrompts.userContent(dto.getTitle(), input))
                    .call()
                    .chatResponse();
            String summary = truncate(cleanText(text(resp)), MAX_SUMMARY_LEN);
            if (!StringUtils.hasText(summary)) {
                throw new IllegalStateException("模型返回空内容");
            }
            writeCache(cacheKey, summary);
            recordLog(SCENE_SUMMARY, hash, false, true, System.currentTimeMillis() - start, resp.getMetadata().getUsage(), null);
            return new AiSummaryVO(summary, false, false);
        } catch (Exception e) {
            log.warn("AI 摘要生成失败，降级为本地截取: {}", e.getMessage());
            recordLog(SCENE_SUMMARY, hash, false, false, System.currentTimeMillis() - start, null, e.getMessage());
            return new AiSummaryVO(fallbackSummary(content), true, false);
        }
    }

    @Override
    public AiTagsVO recommendTags(AiAssistDTO dto) {
        String content = requireContent(dto.getContentMd());
        String input = truncate(content, MAX_INPUT_LEN);
        List<String> existing = existingTagNames();
        String hash = sha256(SCENE_TAGS + "|" + nullToEmpty(dto.getTitle()) + "|" + input + "|" + String.join(",", existing));
        String cacheKey = RedisKeys.cache("ai:tags:" + hash);

        String cached = readCache(cacheKey);
        if (cached != null) {
            recordLog(SCENE_TAGS, hash, true, true, 0L, null, null);
            return new AiTagsVO(splitTags(cached), false, true);
        }

        long start = System.currentTimeMillis();
        try {
            String sys = AiPrompts.TAGS_SYSTEM.formatted(existing.isEmpty() ? "（暂无）" : String.join("、", existing));
            ChatResponse resp = aiChatClient.prompt()
                    .system(sys)
                    .user(AiPrompts.userContent(dto.getTitle(), input))
                    .call()
                    .chatResponse();
            List<String> tags = splitTags(text(resp));
            if (tags.isEmpty()) {
                throw new IllegalStateException("模型未返回有效标签");
            }
            writeCache(cacheKey, String.join(",", tags));
            recordLog(SCENE_TAGS, hash, false, true, System.currentTimeMillis() - start, resp.getMetadata().getUsage(), null);
            return new AiTagsVO(tags, false, false);
        } catch (Exception e) {
            log.warn("AI 标签推荐失败，降级为关键词匹配: {}", e.getMessage());
            recordLog(SCENE_TAGS, hash, false, false, System.currentTimeMillis() - start, null, e.getMessage());
            return new AiTagsVO(fallbackTags(dto.getTitle(), content, existing), true, false);
        }
    }

    // ============================= 输出解析与降级 =============================

    /** 从 ChatResponse 取文本内容。 */
    private String text(ChatResponse resp) {
        return resp == null || resp.getResult() == null ? null : resp.getResult().getOutput().getText();
    }

    /** 清洗摘要：去首尾空白、去包裹引号、去常见前缀。 */
    private String cleanText(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.strip();
        s = s.replaceAll("^[\"'“”『「]+", "").replaceAll("[\"'“”』」]+$", "");
        s = s.replaceFirst("^(摘要|简介|概要)\\s*[:：]\\s*", "");
        return s.strip();
    }

    /** 解析逗号分隔的标签：去重、去空、限长、限量。 */
    private List<String> splitTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new ArrayList<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : raw.split("[,，、\\n]")) {
            String t = part.strip().replaceAll("^[#\\-\\d.、]+", "").strip();
            if (StringUtils.hasText(t) && t.length() <= MAX_TAG_LEN) {
                result.add(t);
            }
            if (result.size() >= MAX_TAGS) {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    /** 降级摘要：剥离 Markdown 标记后截取正文开头。 */
    private String fallbackSummary(String contentMd) {
        String plain = contentMd
                .replaceAll("(?s)```.*?```", " ")          // 代码块
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ") // 图片
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1") // 链接保留文字
                .replaceAll("<[^>]+>", " ")                   // HTML 标签
                .replaceAll("[#>*`_~|-]", " ")                // 其余 md 标记
                .replaceAll("\\s+", " ")
                .strip();
        return truncate(plain, 120);
    }

    /** 降级标签：从已有标签库里挑名字出现在标题/正文中的。 */
    private List<String> fallbackTags(String title, String content, List<String> existing) {
        String haystack = (nullToEmpty(title) + " " + content).toLowerCase();
        return existing.stream()
                .filter(name -> haystack.contains(name.toLowerCase()))
                .limit(MAX_TAGS)
                .collect(Collectors.toList());
    }

    private List<String> existingTagNames() {
        return tagService.list().stream()
                .map(Tag::getName)
                .filter(StringUtils::hasText)
                .limit(100)
                .collect(Collectors.toList());
    }

    // ============================= 缓存与日志 =============================

    private String readCache(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取 AI 缓存失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入 AI 缓存失败 key={}: {}", key, e.getMessage());
        }
    }

    private void recordLog(String scene, String hash, boolean cacheHit, boolean success,
                           long latencyMs, Usage usage, String error) {
        AiCallLog entry = new AiCallLog();
        entry.setScene(scene);
        entry.setModel(model);
        entry.setPromptHash(hash);
        entry.setCacheHit(cacheHit ? 1 : 0);
        entry.setSuccess(success ? 1 : 0);
        entry.setLatencyMs(latencyMs);
        if (usage != null) {
            entry.setPromptTokens(toInt(usage.getPromptTokens()));
            entry.setCompletionTokens(toInt(usage.getCompletionTokens()));
            entry.setTotalTokens(toInt(usage.getTotalTokens()));
        }
        entry.setErrorMessage(truncate(error, 500));
        entry.setCreateTime(LocalDateTime.now());
        aiCallLogger.save(entry);
    }

    // ============================= 工具方法 =============================

    private String requireContent(String contentMd) {
        if (!StringUtils.hasText(contentMd)) {
            throw new BizException("正文为空，无法生成");
        }
        return contentMd;
    }

    private static Integer toInt(Number n) {
        return n == null ? null : n.intValue();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // SHA-256 必然可用；兜底退化为 hashCode，仅影响缓存命中率
            return Integer.toHexString(Arrays.hashCode(s.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
