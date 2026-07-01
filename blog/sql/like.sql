-- =========================================================
-- 文章点赞（一人一赞 toggle，不限量）
-- 身份用前台 visitorId 标识（与埋点访客体系一致）；将来有用户登录后可换成 user_id。
-- like_record 的唯一索引是"一人一赞"的最终一致兜底；Redis 为实时层。
-- =========================================================

USE kyonelife;

-- 文章表增加点赞数（去规范化计数，供列表卡片展示；权威实时值在 Redis）
DELIMITER $$

DROP PROCEDURE IF EXISTS add_like_count_if_missing $$
CREATE PROCEDURE add_like_count_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'article'
      AND COLUMN_NAME = 'like_count'
  ) THEN
    ALTER TABLE `article`
      ADD COLUMN `like_count` bigint NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `view_count`;
  END IF;
END $$

CALL add_like_count_if_missing() $$
DROP PROCEDURE IF EXISTS add_like_count_if_missing $$

DELIMITER ;

-- 点赞记录表：唯一索引保证一人一赞，并发重复插入被数据库拦下
CREATE TABLE IF NOT EXISTS `like_record` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `article_id`  bigint       NOT NULL                 COMMENT '文章ID',
  `visitor_id`  varchar(100) NOT NULL                 COMMENT '访客标识（前台 visitorId）',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_visitor` (`article_id`, `visitor_id`),
  KEY `idx_article` (`article_id`)
) COMMENT '文章点赞记录表';
