-- 前台行为埋点明细表
USE kyonelife;

CREATE TABLE IF NOT EXISTS `event_log` (
  `id`            bigint        NOT NULL AUTO_INCREMENT,
  `event_type`    varchar(50)   NOT NULL                 COMMENT '事件类型：page_view/article_view/tag_click/collection_click/search',
  `visitor_id`    varchar(100)  NOT NULL                 COMMENT '游客唯一标识',
  `article_id`    bigint        DEFAULT NULL             COMMENT '文章ID',
  `tag_id`        bigint        DEFAULT NULL             COMMENT '标签ID',
  `collection_id` bigint        DEFAULT NULL             COMMENT '合集ID',
  `keyword`       varchar(200)  DEFAULT NULL             COMMENT '搜索关键词',
  `page_url`      varchar(500)  DEFAULT NULL             COMMENT '当前页面',
  `referrer`      varchar(500)  DEFAULT NULL             COMMENT '来源页面',
  `ip`            varchar(50)   DEFAULT NULL             COMMENT '访问IP',
  `user_agent`    varchar(1000) DEFAULT NULL             COMMENT 'User-Agent',
  `device`        varchar(50)   DEFAULT NULL             COMMENT '设备类型',
  `browser`       varchar(100)  DEFAULT NULL             COMMENT '浏览器',
  `os`            varchar(100)  DEFAULT NULL             COMMENT '操作系统',
  `duration`      bigint        DEFAULT 0                COMMENT '停留时长，毫秒',
  `create_time`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_visitor_time` (`visitor_id`, `create_time`),
  KEY `idx_article_time` (`article_id`, `create_time`),
  KEY `idx_create_time` (`create_time`)
) COMMENT '前台行为埋点明细表';

CREATE TABLE IF NOT EXISTS `event_daily_stat` (
  `id`             bigint       NOT NULL AUTO_INCREMENT,
  `stat_date`      date         NOT NULL                 COMMENT '统计日期',
  `event_type`     varchar(50)  NOT NULL                 COMMENT '事件类型',
  `pv`             bigint       NOT NULL DEFAULT 0       COMMENT '事件次数',
  `uv`             bigint       NOT NULL DEFAULT 0       COMMENT '去重访客数',
  `duration_total` bigint       NOT NULL DEFAULT 0       COMMENT '累计停留时长，毫秒',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_event` (`stat_date`, `event_type`),
  KEY `idx_event_date` (`event_type`, `stat_date`)
) COMMENT '前台行为每日聚合表';

CREATE TABLE IF NOT EXISTS `article_daily_stat` (
  `id`             bigint   NOT NULL AUTO_INCREMENT,
  `stat_date`      date     NOT NULL                 COMMENT '统计日期',
  `article_id`     bigint   NOT NULL                 COMMENT '文章ID',
  `view_count`     bigint   NOT NULL DEFAULT 0       COMMENT '文章浏览次数',
  `visitor_count`  bigint   NOT NULL DEFAULT 0       COMMENT '文章去重访客数',
  `duration_total` bigint   NOT NULL DEFAULT 0       COMMENT '累计阅读时长，毫秒',
  `create_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_article` (`stat_date`, `article_id`),
  KEY `idx_article_date` (`article_id`, `stat_date`),
  KEY `idx_date_view` (`stat_date`, `view_count`)
) COMMENT '文章每日聚合表';

CREATE TABLE IF NOT EXISTS `tag_daily_stat` (
  `id`            bigint   NOT NULL AUTO_INCREMENT,
  `stat_date`     date     NOT NULL                 COMMENT '统计日期',
  `tag_id`        bigint   NOT NULL                 COMMENT '标签ID',
  `click_count`   bigint   NOT NULL DEFAULT 0       COMMENT '标签点击次数',
  `visitor_count` bigint   NOT NULL DEFAULT 0       COMMENT '去重访客数',
  `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_tag` (`stat_date`, `tag_id`),
  KEY `idx_tag_date` (`tag_id`, `stat_date`),
  KEY `idx_date_click` (`stat_date`, `click_count`)
) COMMENT '标签每日点击聚合表';

CREATE TABLE IF NOT EXISTS `collection_daily_stat` (
  `id`            bigint   NOT NULL AUTO_INCREMENT,
  `stat_date`     date     NOT NULL                 COMMENT '统计日期',
  `collection_id` bigint   NOT NULL                 COMMENT '合集ID',
  `click_count`   bigint   NOT NULL DEFAULT 0       COMMENT '合集点击次数',
  `visitor_count` bigint   NOT NULL DEFAULT 0       COMMENT '去重访客数',
  `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_collection` (`stat_date`, `collection_id`),
  KEY `idx_collection_date` (`collection_id`, `stat_date`),
  KEY `idx_date_click` (`stat_date`, `click_count`)
) COMMENT '合集每日点击聚合表';

CREATE TABLE IF NOT EXISTS `search_keyword_stat` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `stat_date`     date         NOT NULL                 COMMENT '统计日期',
  `keyword`       varchar(200) NOT NULL                 COMMENT '搜索关键词',
  `search_count`  bigint       NOT NULL DEFAULT 0       COMMENT '搜索次数',
  `visitor_count` bigint       NOT NULL DEFAULT 0       COMMENT '去重访客数',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_keyword` (`stat_date`, `keyword`),
  KEY `idx_keyword_date` (`keyword`, `stat_date`),
  KEY `idx_date_search` (`stat_date`, `search_count`)
) COMMENT '搜索关键词每日聚合表';
