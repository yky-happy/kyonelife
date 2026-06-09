USE kyonelife;

INSERT IGNORE INTO `menu` (`id`, `parent_id`, `title`, `type`, `path`, `component`, `perm`, `sort`) VALUES
(25, 2, '操作日志', 'MENU', '/system/operation-log', 'system/operation-log/index', NULL, 6),
(250, 25, '操作日志列表', 'BUTTON', NULL, NULL, 'operation-log:list', 1);

INSERT IGNORE INTO `role_menu` (`role_id`, `menu_id`) VALUES
(1, 25),
(1, 250),
(2, 25),
(2, 250);

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`               bigint        NOT NULL AUTO_INCREMENT,
  `admin_id`         bigint        DEFAULT NULL             COMMENT '管理员ID',
  `admin_name`       varchar(50)   DEFAULT NULL             COMMENT '管理员名称',
  `module`           varchar(50)   NOT NULL                 COMMENT '操作模块',
  `operation`        varchar(50)   NOT NULL                 COMMENT '操作类型',
  `request_method`   varchar(10)   DEFAULT NULL             COMMENT '请求方法',
  `request_path`     varchar(255)  DEFAULT NULL             COMMENT '请求路径',
  `request_params`   text          DEFAULT NULL             COMMENT '请求参数，敏感字段脱敏',
  `response_code`    int           DEFAULT NULL             COMMENT '响应编码',
  `response_message` varchar(200)  DEFAULT NULL             COMMENT '响应消息',
  `ip`               varchar(50)   DEFAULT NULL             COMMENT '请求IP',
  `user_agent`       varchar(2000) DEFAULT NULL             COMMENT 'User-Agent',
  `cost_time`        bigint        DEFAULT NULL             COMMENT '接口耗时，毫秒',
  `success`          tinyint       NOT NULL DEFAULT 1       COMMENT '0=失败 1=成功',
  `error_message`    text          DEFAULT NULL             COMMENT '异常信息',
  `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_module` (`module`),
  KEY `idx_success` (`success`),
  KEY `idx_create_time` (`create_time`)
) COMMENT '后台操作日志表';
