package com.yky.blog.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志：记录每一次大模型调用的场景、模型、耗时、token 消耗与成败，
 * 用于成本统计与可观测（命中缓存的调用也会记录，便于评估缓存省下的 token）。
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 调用场景：summary（摘要生成）/ tags（标签推荐）。 */
    private String scene;
    /** 实际使用的模型，如 deepseek-chat。 */
    private String model;
    /** 输入内容的 SHA-256，用于缓存命中统计与排查。 */
    private String promptHash;
    /** 是否命中缓存：0 否 1 是。命中时不消耗 token。 */
    private Integer cacheHit;
    /** 是否调用成功：0 否（已降级）1 是。 */
    private Integer success;
    /** 耗时毫秒（命中缓存记 0）。 */
    private Long latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    /** Agent 任务内的步序（第几轮模型调用）；第一层非 Agent 调用为空。 */
    private Integer stepNo;
    /** 该步模型请求调用的工具名（逗号分隔）；最终步（直接出答案）为空。 */
    private String toolName;
    /** 失败或降级原因（成功为空）。 */
    private String errorMessage;
    private LocalDateTime createTime;
}
