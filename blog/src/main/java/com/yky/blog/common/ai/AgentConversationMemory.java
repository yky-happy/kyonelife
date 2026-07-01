package com.yky.blog.common.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Cross-request Agent memory facade.
 * Conversation buckets are isolated by admin, scene, and frontend session id.
 */
@Component
@RequiredArgsConstructor
public class AgentConversationMemory {

    private final ChatMemory chatMemory;

    @Value("${blog.ai.agent.memory-message-max-chars:4000}")
    private int maxMessageChars;

    public String conversationId(Object adminId, String scene, String sessionId) {
        String adminPart = adminId == null ? "anonymous" : String.valueOf(adminId);
        String scenePart = normalize(scene, "default");
        String sessionPart = normalize(sessionId, "default");
        return adminPart + ":" + scenePart + ":" + sessionPart;
    }

    public List<Message> recentMessages(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        return chatMemory.get(conversationId);
    }

    public void remember(String conversationId, String userText, String assistantText) {
        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(userText) || !StringUtils.hasText(assistantText)) {
            return;
        }
        chatMemory.add(conversationId, List.of(
                new UserMessage(trim(userText)),
                new AssistantMessage(trim(assistantText))));
    }

    private String normalize(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.replaceAll("[^a-zA-Z0-9:_-]", "_");
    }

    private String trim(String value) {
        int limit = Math.max(200, maxMessageChars);
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
