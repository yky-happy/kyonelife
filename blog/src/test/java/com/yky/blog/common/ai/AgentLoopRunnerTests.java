package com.yky.blog.common.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentLoopRunnerTests {

    @Test
    void shouldForceFinalAnswerWhenToolLoopReachesMaxRounds() {
        ChatModel chatModel = mock(ChatModel.class);
        ToolCallingManager toolCallingManager = mock(ToolCallingManager.class);
        AiCallLogger aiCallLogger = mock(AiCallLogger.class);
        AgentLoopRunner runner = new AgentLoopRunner(chatModel, toolCallingManager, aiCallLogger);
        ReflectionTestUtils.setField(runner, "defaultMaxRounds", 1);
        ReflectionTestUtils.setField(runner, "model", "test-model");

        ChatResponse toolCallResponse = response(new AssistantMessage(
                "",
                Map.of(),
                List.of(new AssistantMessage.ToolCall("call-1", "function", "get_hot_keywords", "{}"))));
        ChatResponse finalResponse = response(new AssistantMessage("[{\"title\":\"最终选题\"}]"));

        when(chatModel.call(any(Prompt.class))).thenReturn(toolCallResponse, finalResponse);
        when(toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
                .thenReturn(ToolExecutionResult.builder()
                        .conversationHistory(List.of(new UserMessage("goal"), new AssistantMessage("tool result")))
                        .build());

        AgentLoopRunner.AgentResult result = runner.run("agent-topics", "system", "goal", new TestTools(), null);

        assertThat(result.ok()).isTrue();
        assertThat(result.capped()).isTrue();
        assertThat(result.rounds()).isEqualTo(1);
        assertThat(result.text()).contains("最终选题");
        verify(chatModel, times(2)).call(any(Prompt.class));
        verify(aiCallLogger, times(2)).save(any());
    }

    private ChatResponse response(AssistantMessage message) {
        return new ChatResponse(List.of(new Generation(message)));
    }

    static class TestTools {
        @Tool(description = "test tool")
        String getHotKeywords() {
            return "Redis";
        }
    }
}
