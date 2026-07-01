-- 前台读者：邮箱登录所需字段（email 唯一、头像；phone 改为可空）
ALTER TABLE user
    MODIFY COLUMN phone VARCHAR(20) NULL,
    ADD COLUMN email VARCHAR(120) NULL UNIQUE COMMENT '邮箱' AFTER phone,
    ADD COLUMN avatar VARCHAR(255) NULL COMMENT '头像' AFTER nickname;

-- 文章评论
CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT       NOT NULL COMMENT '文章ID',
    user_id     BIGINT       NOT NULL COMMENT '评论用户ID',
    content     VARCHAR(1000) NOT NULL COMMENT '内容',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_article (article_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '文章评论';
