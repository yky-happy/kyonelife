package com.yky.blog.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 配置：基于 Spring AI 自动装配的 DeepSeek ChatModel，构建一个共享的 {@link ChatClient}。
 * 模型、API Key、温度等参数在 application.yml 的 spring.ai.deepseek.* 配置。
 */
@Configuration
public class AiConfig {

    /**
     * 全局共享的 ChatClient。Spring AI 依据 spring-ai-starter-model-deepseek 自动装配
     * {@link ChatClient.Builder}，这里 build 出一个不带默认提示词的客户端，提示词在调用处按场景指定。
     */
    @Bean
    public ChatClient aiChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 工具调用管理器，供第二层工具型 Agent 手写循环时<b>手动执行工具</b>使用
     * （配合 {@code internalToolExecutionEnabled=false} 关闭框架自动循环，实现迭代次数等控制）。
     * 通常 Spring AI 已自动装配一个；此处仅在缺失时兜底，避免依赖注入失败。
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolCallingManager toolCallingManager() {
        return ToolCallingManager.builder().build();
    }

    /**
     * Agent 跨请求会话记忆。底层 Repository 落 Redis，窗口只保留最近若干条消息，控制 token 成本。
     */
    @Bean
    public ChatMemory agentChatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${blog.ai.agent.memory-max-messages:8}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(Math.max(2, maxMessages))
                .build();
    }

    /**
     * 给容器里共享的 {@link org.springframework.web.client.RestClient.Builder} 设置较长的读超时。
     * Spring AI 的 DeepSeek 客户端用的就是这个 builder：第一层 summary/tags 输出短无所谓，
     * 但第二层创作 Agent 要生成整篇文章，单次响应较慢，默认读超时会触发 SocketTimeout 导致整体降级。
     * 连接超时仍保持较短以便快速发现网络问题。
     */
    @Bean
    public RestClientCustomizer aiHttpTimeoutCustomizer(
            @Value("${blog.ai.http.connect-timeout-seconds:10}") long connectSeconds,
            @Value("${blog.ai.http.read-timeout-seconds:180}") long readSeconds) {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(connectSeconds))
                        .withReadTimeout(Duration.ofSeconds(readSeconds))));
    }
}
