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
