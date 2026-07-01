-- AI 内容辅助：调用日志表（成本与可观测）。
-- 用于已有数据库的增量建表，init.sql 也已包含本表。
-- 第一层（summary/tags）每次调用记一行；第二层工具型 Agent 一次任务里
-- 的「每一步模型调用」各记一行（step_no 区分），实现 step 级可观测。
CREATE TABLE IF NOT EXISTS ai_call_log (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    scene             VARCHAR(32) NOT NULL                COMMENT '调用场景：summary/tags/agent-topics/agent-draft',
    model             VARCHAR(64)          DEFAULT NULL   COMMENT '模型，如 deepseek-chat',
    prompt_hash       CHAR(64)             DEFAULT NULL   COMMENT '输入内容 SHA-256，用于缓存命中统计',
    cache_hit         TINYINT              DEFAULT 0      COMMENT '是否命中缓存：0 否 1 是',
    success           TINYINT              DEFAULT 0      COMMENT '是否成功：0 否(已降级) 1 是',
    latency_ms        BIGINT               DEFAULT NULL   COMMENT '耗时毫秒（命中缓存为 0）',
    prompt_tokens     INT                  DEFAULT NULL   COMMENT '输入 token',
    completion_tokens INT                  DEFAULT NULL   COMMENT '输出 token',
    total_tokens      INT                  DEFAULT NULL   COMMENT '总 token',
    step_no           INT                  DEFAULT NULL   COMMENT 'Agent 任务内的步序（第几轮模型调用）；非 Agent 为空',
    tool_name         VARCHAR(128)         DEFAULT NULL   COMMENT '该步模型请求调用的工具名（逗号分隔）；最终步为空',
    error_message     VARCHAR(500)         DEFAULT NULL   COMMENT '失败或降级原因',
    create_time       DATETIME             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_scene_time (scene, create_time),
    KEY idx_prompt_hash (prompt_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI 调用日志';

-- 已有库增量升级：补充 Agent step 级可观测字段。
-- MySQL 8.0.29+ 支持 IF NOT EXISTS；低版本请先检查字段是否存在后再执行。
ALTER TABLE ai_call_log
    ADD COLUMN IF NOT EXISTS step_no   INT          DEFAULT NULL COMMENT 'Agent 任务内的步序（第几轮模型调用）；非 Agent 为空' AFTER total_tokens,
    ADD COLUMN IF NOT EXISTS tool_name VARCHAR(128) DEFAULT NULL COMMENT '该步模型请求调用的工具名（逗号分隔）；最终步为空' AFTER step_no;
