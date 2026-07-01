-- =========================================================
-- 文章表增量字段
-- 用于已有库升级；新库初始化请直接执行 init.sql。
-- =========================================================

USE kyonelife;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_article_columns_if_missing $$
CREATE PROCEDURE add_article_columns_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'article'
      AND COLUMN_NAME = 'publish_time'
  ) THEN
    ALTER TABLE `article`
      ADD COLUMN `publish_time` datetime DEFAULT NULL COMMENT '定时发布时间，NULL 表示不定时发布' AFTER `status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'article'
      AND COLUMN_NAME = 'like_count'
  ) THEN
    ALTER TABLE `article`
      ADD COLUMN `like_count` bigint NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER `view_count`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'article'
      AND INDEX_NAME = 'idx_publish_time'
  ) THEN
    ALTER TABLE `article` ADD INDEX `idx_publish_time` (`publish_time`);
  END IF;
END $$

CALL add_article_columns_if_missing() $$
DROP PROCEDURE IF EXISTS add_article_columns_if_missing $$

DELIMITER ;
