package com.yky.blog.common.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yky.blog.common.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-backed ChatMemoryRepository for Agent cross-request memory.
 * Only plain system/user/assistant messages are persisted; tool messages are per-run execution detail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final TypeReference<List<StoredMessage>> MESSAGE_LIST = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${blog.ai.agent.memory-ttl-hours:24}")
    private long ttlHours;

    @Override
    public List<String> findConversationIds() {
        List<String> ids = new ArrayList<>();
        String prefix = RedisKeys.agentMemory("");
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                ids.add(cursor.next().substring(prefix.length()));
            }
        } catch (Exception e) {
            log.warn("扫描 Agent 会话记忆失败: {}", e.getMessage());
        }
        return ids;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            return List.of();
        }
        String raw = stringRedisTemplate.opsForValue().get(RedisKeys.agentMemory(conversationId));
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            List<StoredMessage> stored = objectMapper.readValue(raw, MESSAGE_LIST);
            List<Message> messages = new ArrayList<>();
            for (StoredMessage item : stored) {
                Message message = toMessage(item);
                if (message != null) {
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("读取 Agent 会话记忆失败 conversationId={}: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (!StringUtils.hasText(conversationId)) {
            return;
        }
        try {
            List<StoredMessage> stored = messages.stream()
                    .filter(this::persistable)
                    .map(m -> new StoredMessage(m.getMessageType().getValue(), m.getText()))
                    .toList();
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.agentMemory(conversationId),
                    objectMapper.writeValueAsString(stored),
                    Duration.ofHours(Math.max(1, ttlHours)));
        } catch (Exception e) {
            log.warn("保存 Agent 会话记忆失败 conversationId={}: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            stringRedisTemplate.delete(RedisKeys.agentMemory(conversationId));
        }
    }

    private boolean persistable(Message message) {
        if (message == null || !StringUtils.hasText(message.getText())) {
            return false;
        }
        MessageType type = message.getMessageType();
        return type == MessageType.USER || type == MessageType.ASSISTANT || type == MessageType.SYSTEM;
    }

    private Message toMessage(StoredMessage item) {
        if (item == null || !StringUtils.hasText(item.text())) {
            return null;
        }
        MessageType type = MessageType.fromValue(item.type());
        if (type == MessageType.USER) {
            return new UserMessage(item.text());
        }
        if (type == MessageType.ASSISTANT) {
            return new AssistantMessage(item.text());
        }
        if (type == MessageType.SYSTEM) {
            return new SystemMessage(item.text());
        }
        return null;
    }

    private record StoredMessage(String type, String text) {
    }
}
