package com.yky.blog.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.admin.dto.AgentTopicsDTO;
import com.yky.blog.admin.service.AnalyticsService;
import com.yky.blog.admin.vo.AgentTopicsVO;
import com.yky.blog.common.ai.AgentConversationMemory;
import com.yky.blog.common.ai.AgentLoopRunner;
import com.yky.blog.common.ai.ContentAgentTools;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentAgentServiceImplTests {

    @Test
    void shouldPassConversationMemoryToAgentAndRememberSuccessfulResult() {
        AgentLoopRunner runner = mock(AgentLoopRunner.class);
        ContentAgentTools tools = mock(ContentAgentTools.class);
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        AgentConversationMemory memory = mock(AgentConversationMemory.class);
        ContentAgentServiceImpl service = new ContentAgentServiceImpl(
                runner, tools, analyticsService, new ObjectMapper(), memory);

        AgentTopicsDTO dto = new AgentTopicsDTO();
        dto.setDays(7);
        dto.setCount(1);
        dto.setDirection("Redis");
        dto.setSessionId("session-a");
        List<Message> recent = List.of(new UserMessage("上一轮：换个角度"));
        String response = """
                [{"title":"Redis 热点 Key 治理","reason":"引用热搜词 Redis","refKeywords":["Redis"],"refArticles":[]}]
                """;

        when(memory.conversationId(100L, "agent-topics", "session-a")).thenReturn("100:agent-topics:session-a");
        when(memory.recentMessages("100:agent-topics:session-a")).thenReturn(recent);
        when(runner.run(eq("agent-topics"), any(), any(), eq(tools), isNull(), anyList()))
                .thenReturn(new AgentLoopRunner.AgentResult(response, 2, false, true, null));

        AgentTopicsVO vo = service.suggestTopics(dto, 100L);

        assertThat(vo.getTopics()).hasSize(1);
        assertThat(vo.getTopics().get(0).getTitle()).isEqualTo("Redis 热点 Key 治理");
        assertThat(vo.getDegraded()).isFalse();

        ArgumentCaptor<List<Message>> memoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(runner).run(eq("agent-topics"), any(), any(), eq(tools), isNull(), memoryCaptor.capture());
        assertThat(memoryCaptor.getValue()).isSameAs(recent);
        verify(memory).remember(eq("100:agent-topics:session-a"), any(), eq(response));
    }
}
