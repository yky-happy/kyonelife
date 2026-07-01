package com.yky.blog.common.ai;

import com.yky.blog.common.entity.AiCallLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工具型 Agent 的执行引擎（第二层公共底座）。
 *
 * <p>刻意<b>关闭 Spring AI 的自动工具循环</b>（{@code internalToolExecutionEnabled=false}），
 * 改为这里手写 loop，从而拿到三样自动循环给不了的工程控制：</p>
 * <ol>
 *   <li><b>最大迭代次数</b>：超过 {@code maxRounds} 仍想调工具时强制收口，绝不无限烧 token；</li>
 *   <li><b>step 级可观测</b>：每一轮模型调用都单独落一条 {@code ai_call_log}（step_no、tool_name、token、耗时）；</li>
 *   <li><b>到顶兜底</b>：达到上限后再做一次"禁用工具"的收尾调用，逼模型基于已有信息给最终答案，
 *       而不是把一个半成品的工具调用请求返回给上层。</li>
 * </ol>
 *
 * <p>工具内部抛出的异常由 {@link ToolCallingManager} 默认的异常处理器转成可读文本回灌给模型
 * （让它换策略），而不是整个请求失败——满足"工具异常处理"控制点。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLoopRunner {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final AiCallLogger aiCallLogger;

    @Value("${spring.ai.deepseek.chat.options.model:deepseek-chat}")
    private String model;

    /** 单次任务里"工具→模型"循环的最大轮数，超出即强制收口。可在 application.yml 调。 */
    @Value("${blog.ai.agent.max-rounds:5}")
    private int defaultMaxRounds;

    /** 一次 Agent 运行的结果。 */
    public record AgentResult(String text, int rounds, boolean capped, boolean success, String error) {
        public boolean ok() {
            return success;
        }
    }

    /**
     * 跑一个工具型 Agent 任务。
     *
     * @param scene        日志场景名（如 agent-topics / agent-draft）
     * @param systemPrompt 系统提示词（角色与输出约束）
     * @param userGoal     用户目标
     * @param toolsBean    带 {@code @Tool} 注解方法的对象（如 ContentAgentTools）
     * @param toolContext  传给工具的上下文（如承接写工具回填的草稿 holder），可为空
     */
    public AgentResult run(String scene, String systemPrompt, String userGoal,
                           Object toolsBean, Map<String, Object> toolContext) {
        return run(scene, systemPrompt, userGoal, toolsBean, toolContext, List.of());
    }

    public AgentResult run(String scene, String systemPrompt, String userGoal,
                           Object toolsBean, Map<String, Object> toolContext,
                           List<Message> memoryMessages) {
        int maxRounds = Math.max(1, defaultMaxRounds);
        ToolCallback[] callbacks = ToolCallbacks.from(toolsBean);

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(false)        // 关键：自动循环关掉，改由本类控制
                .toolContext(toolContext == null ? Map.of() : toolContext)
                .build();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        if (memoryMessages != null && !memoryMessages.isEmpty()) {
            messages.addAll(memoryMessages);
        }
        messages.add(new UserMessage(userGoal));
        Prompt prompt = new Prompt(messages, options);

        try {
            int round = 0;
            while (round < maxRounds) {
                round++;
                ChatResponse response = callAndLog(scene, round, prompt);

                if (!response.hasToolCalls()) {
                    // 模型不再要工具 → 这是最终答案
                    return new AgentResult(text(response), round, false, true, null);
                }
                // 还想调工具：执行工具（异常被 manager 转成文本回灌），把结果接回对话继续下一轮
                ToolExecutionResult exec = toolCallingManager.executeToolCalls(prompt, response);
                prompt = new Prompt(exec.conversationHistory(), options);
            }

            // 到达上限仍未收口：做一次"禁用工具"的收尾调用，强制出最终结果
            log.warn("Agent[{}] 达到最大迭代轮数 {}，强制收口", scene, maxRounds);
            ChatResponse finalResp = forceFinalAnswer(scene, maxRounds, prompt);
            return new AgentResult(text(finalResp), maxRounds, true, true, null);
        } catch (Exception e) {
            log.warn("Agent[{}] 执行失败：{}", scene, e.getMessage());
            recordStep(scene, 0, null, 0L, null, false, e.getMessage());
            return new AgentResult(null, 0, false, false, e.getMessage());
        }
    }

    /** 调一次模型并落 step 级日志，返回响应。 */
    private ChatResponse callAndLog(String scene, int round, Prompt prompt) {
        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        long latency = System.currentTimeMillis() - start;
        recordStep(scene, round, toolNamesOf(response), latency, usageOf(response), true, null);
        return response;
    }

    /** 达上限的收尾：清掉工具、追加"别再调工具"的提示，逼模型直接给答案。 */
    private ChatResponse forceFinalAnswer(String scene, int maxRounds, Prompt prompt) {
        ToolCallingChatOptions noTools = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
        List<Message> history = new ArrayList<>(prompt.getInstructions());
        history.add(new UserMessage(AiPrompts.CAP_REACHED_NUDGE));
        return callAndLog(scene, maxRounds + 1, new Prompt(history, noTools));
    }

    /** 取该轮模型请求调用的工具名（逗号分隔）；最终步无工具调用返回 null。 */
    private String toolNamesOf(ChatResponse response) {
        if (response == null || !response.hasToolCalls()) {
            return null;
        }
        String names = response.getResults().stream()
                .map(g -> g.getOutput())
                .filter(out -> out != null && out.getToolCalls() != null)
                .flatMap(out -> out.getToolCalls().stream())
                .map(AssistantMessage.ToolCall::name)
                .distinct()
                .collect(Collectors.joining(","));
        return StringUtils.hasText(names) ? names : null;
    }

    private Usage usageOf(ChatResponse response) {
        return response == null || response.getMetadata() == null ? null : response.getMetadata().getUsage();
    }

    private String text(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private void recordStep(String scene, int stepNo, String toolName, long latencyMs,
                            Usage usage, boolean success, String error) {
        AiCallLog entry = new AiCallLog();
        entry.setScene(scene);
        entry.setModel(model);
        entry.setCacheHit(0);
        entry.setSuccess(success ? 1 : 0);
        entry.setLatencyMs(latencyMs);
        entry.setStepNo(stepNo);
        entry.setToolName(toolName);
        if (usage != null) {
            entry.setPromptTokens(toInt(usage.getPromptTokens()));
            entry.setCompletionTokens(toInt(usage.getCompletionTokens()));
            entry.setTotalTokens(toInt(usage.getTotalTokens()));
        }
        if (error != null) {
            entry.setErrorMessage(error.length() > 500 ? error.substring(0, 500) : error);
        }
        entry.setCreateTime(LocalDateTime.now());
        aiCallLogger.save(entry);
    }

    private static Integer toInt(Number n) {
        return n == null ? null : n.intValue();
    }
}
