-- =========================================================
-- event_log 按日 RANGE 分区 + 生命周期管理（埋点明细表瘦身）
-- 目的：解决原始事件表无限增长——分区后删除老数据用 DROP PARTITION（秒级、不锁表），
--       远快于 DELETE；查询按日期可分区裁剪（partition pruning）提速。
--
-- 说明：MySQL RANGE 分区要求分区键包含在所有唯一键/主键中，
--       因此主键由 id 改为 (id, create_time) 复合主键。
-- 适用：MySQL 8.0。请在低峰期执行，大表改造前务必备份。
-- =========================================================

USE kyonelife;

-- ---------------------------------------------------------
-- 1. 改造为分区表（按 create_time 的天分区）
--    若为全新部署，可直接在建表语句里带上 PARTITION BY，省去本步。
-- ---------------------------------------------------------
ALTER TABLE `event_log`
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (`id`, `create_time`);

ALTER TABLE `event_log`
  PARTITION BY RANGE COLUMNS(`create_time`) (
    PARTITION p20260101 VALUES LESS THAN ('2026-01-02'),
    PARTITION p20260102 VALUES LESS THAN ('2026-01-03'),
    -- ……实际由下方存储过程按需补充未来分区……
    PARTITION pmax       VALUES LESS THAN (MAXVALUE)
  );

-- ---------------------------------------------------------
-- 2. 维护存储过程：补未来分区 + 删过期分区（保留最近 N 天）
-- ---------------------------------------------------------
DELIMITER $$

-- 确保未来 ahead_days 天的分区都存在（在 pmax 之前 REORGANIZE 出新分区）
DROP PROCEDURE IF EXISTS `sp_event_log_add_partitions` $$
CREATE PROCEDURE `sp_event_log_add_partitions`(IN ahead_days INT)
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE pname VARCHAR(16);
  DECLARE pval  VARCHAR(16);
  WHILE i < ahead_days DO
    SET pname = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL i DAY), 'p%Y%m%d');
    SET pval  = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL i + 1 DAY), '%Y-%m-%d');
    IF NOT EXISTS (
      SELECT 1 FROM information_schema.PARTITIONS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log' AND PARTITION_NAME = pname
    ) THEN
      SET @sql = CONCAT('ALTER TABLE `event_log` REORGANIZE PARTITION pmax INTO (',
                        'PARTITION ', pname, ' VALUES LESS THAN (''', pval, '''), ',
                        'PARTITION pmax VALUES LESS THAN (MAXVALUE))');
      PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
    SET i = i + 1;
  END WHILE;
END $$

-- 删除早于 keep_days 天的分区（明细 TTL，过期即 DROP PARTITION）
DROP PROCEDURE IF EXISTS `sp_event_log_drop_old_partitions` $$
CREATE PROCEDURE `sp_event_log_drop_old_partitions`(IN keep_days INT)
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE pname VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT PARTITION_NAME FROM information_schema.PARTITIONS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_log'
      AND PARTITION_NAME IS NOT NULL AND PARTITION_NAME <> 'pmax'
      AND PARTITION_NAME < DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL keep_days DAY), 'p%Y%m%d');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO pname;
    IF done THEN LEAVE read_loop; END IF;
    SET @sql = CONCAT('ALTER TABLE `event_log` DROP PARTITION ', pname);
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END LOOP;
  CLOSE cur;
END $$

DELIMITER ;

-- ---------------------------------------------------------
-- 3. 定时维护（保留明细 90 天，提前补 7 天分区）
--    需开启事件调度器：SET GLOBAL event_scheduler = ON;
-- ---------------------------------------------------------
DROP EVENT IF EXISTS `ev_event_log_maintain`;
CREATE EVENT `ev_event_log_maintain`
  ON SCHEDULE EVERY 1 DAY STARTS (TIMESTAMP(CURDATE()) + INTERVAL 1 DAY + INTERVAL 1 HOUR)
  DO BEGIN
    CALL sp_event_log_add_partitions(7);
    CALL sp_event_log_drop_old_partitions(90);
  END;

-- 首次手动初始化未来分区：
-- CALL sp_event_log_add_partitions(7);
