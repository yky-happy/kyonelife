-- =========================================================
-- 博客系统数据库初始化脚本
-- 数据库：kyonelife
-- =========================================================

CREATE DATABASE IF NOT EXISTS kyonelife DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kyonelife;

-- ---------------------------------------------------------
-- 1. 前台用户表（手机号验证码登录）
-- ---------------------------------------------------------
CREATE TABLE `user` (
  `id`              bigint       NOT NULL AUTO_INCREMENT,
  `phone`           varchar(20)  NOT NULL UNIQUE          COMMENT '手机号',
  `nickname`        varchar(50)  NOT NULL                 COMMENT '自动生成昵称，如 用户_3k9x',
  `status`          tinyint      NOT NULL DEFAULT 1       COMMENT '0=封禁 1=正常',
  `ip`              varchar(50)  DEFAULT NULL             COMMENT '最后登录 IP',
  `ip_location`     varchar(100) DEFAULT NULL             COMMENT '最后登录 IP 归属地',
  `browser`         varchar(100) DEFAULT NULL             COMMENT '最后登录浏览器',
  `os`              varchar(100) DEFAULT NULL             COMMENT '最后登录操作系统',
  `last_login_time` datetime     DEFAULT NULL             COMMENT '最后登录时间',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`)
) COMMENT '前台访客用户表';

-- ---------------------------------------------------------
-- 2. 后台管理员表
-- ---------------------------------------------------------
CREATE TABLE `admin` (
  `id`              bigint       NOT NULL AUTO_INCREMENT,
  `username`        varchar(50)  NOT NULL UNIQUE          COMMENT '登录账号',
  `password`        varchar(100) NOT NULL                 COMMENT 'BCrypt 加密密码',
  `nickname`        varchar(50)  DEFAULT NULL             COMMENT '显示名称',
  `status`          tinyint      NOT NULL DEFAULT 1       COMMENT '0=禁用 1=正常',
  `ip`              varchar(50)  DEFAULT NULL             COMMENT '最后登录 IP',
  `ip_location`     varchar(100) DEFAULT NULL             COMMENT '最后登录 IP 归属地',
  `browser`         varchar(100) DEFAULT NULL             COMMENT '最后登录浏览器',
  `os`              varchar(100) DEFAULT NULL             COMMENT '最后登录操作系统',
  `last_login_time` datetime     DEFAULT NULL             COMMENT '最后登录时间',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '后台管理员表';

-- 初始化超级管理员（密码明文: admin123，上线前请修改）
INSERT INTO `admin` (`username`, `password`, `nickname`)
VALUES ('yky', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员');

-- ---------------------------------------------------------
-- 3. 角色表
-- ---------------------------------------------------------
CREATE TABLE `role` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `code`        varchar(50)  NOT NULL UNIQUE             COMMENT '角色编码，如 SUPER / REGULAR',
  `name`        varchar(50)  NOT NULL                    COMMENT '角色名称，如 超级管理员',
  `remarks`     varchar(200) DEFAULT NULL                COMMENT '角色描述',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '角色表';

INSERT INTO `role` (`code`, `name`, `remarks`) VALUES
('SUPER',   '超级管理员', '拥有全部权限'),
('REGULAR', '普通管理员', '只读权限，不能执行写操作');

-- ---------------------------------------------------------
-- 4. 管理员角色关联表
-- ---------------------------------------------------------
CREATE TABLE `admin_role` (
  `id`       int NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL                             COMMENT '管理员ID',
  `role_id`  int    NOT NULL                             COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`)
) COMMENT '管理员角色关联表';

-- 初始超级管理员绑定 SUPER 角色（admin.id=1, role.id=1）
INSERT INTO `admin_role` (`admin_id`, `role_id`) VALUES (1, 1);

-- ---------------------------------------------------------
-- 5. 权限菜单表（目录 / 菜单 / 按钮三级）
-- ---------------------------------------------------------
CREATE TABLE `menu` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `parent_id`   int          DEFAULT 0                   COMMENT '父菜单ID，0 表示顶级',
  `title`       varchar(50)  NOT NULL                    COMMENT '菜单名称',
  `type`        varchar(20)  NOT NULL                    COMMENT 'CATALOG=目录 MENU=菜单 BUTTON=按钮',
  `path`        varchar(200) DEFAULT NULL                COMMENT '前端路由路径',
  `component`   varchar(200) DEFAULT NULL                COMMENT '前端组件路径',
  `perm`        varchar(100) DEFAULT NULL                COMMENT '权限标识，如 article:add',
  `icon`        varchar(100) DEFAULT NULL                COMMENT '菜单图标',
  `sort`        int          NOT NULL DEFAULT 0          COMMENT '排序，越小越靠前',
  `hidden`      tinyint      NOT NULL DEFAULT 0          COMMENT '0=显示 1=隐藏',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '权限菜单表';

-- 初始化菜单数据
INSERT INTO `menu` (`id`, `parent_id`, `title`, `type`, `path`, `component`, `perm`, `sort`) VALUES
-- 顶级目录
(1,  0, '内容管理', 'CATALOG', '/content',  NULL, NULL, 1),
(2,  0, '系统管理', 'CATALOG', '/system',   NULL, NULL, 2),
-- 内容管理 > 菜单
(10, 1, '文章管理', 'MENU',    '/content/article',    'content/article/index',    NULL,             1),
(11, 1, '合集管理', 'MENU',    '/content/collection', 'content/collection/index', NULL,             2),
(12, 1, '标签管理', 'MENU',    '/content/tag',        'content/tag/index',        NULL,             3),
(13, 1, 'AI 创作助手', 'MENU', '/content/agent',      'content/agent/index',      NULL,             4),
(14, 1, '轮播管理', 'MENU',    '/content/banner',     'content/banner/index',     NULL,             5),
-- 系统管理 > 菜单
(20, 2, '管理员管理', 'MENU',  '/system/admin',       'system/admin/index',       NULL,             1),
(21, 2, '角色管理',   'MENU',  '/system/role',        'system/role/index',        NULL,             2),
(22, 2, '菜单管理',   'MENU',  '/system/menu',        'system/menu/index',        NULL,             3),
(23, 2, '用户管理',   'MENU',  '/system/user',        'system/user/index',        NULL,             4),
(24, 2, '网站配置',   'MENU',  '/system/config',      'system/config/index',      NULL,             5),
(25, 2, '操作日志',   'MENU',  '/system/operation-log','system/operation-log/index',NULL,            6),
(26, 2, '数据分析',   'MENU',  '/system/analytics',   'system/analytics/index',   NULL,             7),
(27, 2, '文件管理',   'MENU',  '/system/file',        'system/file/index',        NULL,             8),
(28, 2, '运行日志',   'MENU',  '/system/log',         'system/log/index',         NULL,             9),
(29, 2, '仪表盘',     'MENU',  '/dashboard',          'dashboard/index',          NULL,             0),
-- 文章管理按钮
(100, 10, '文章列表', 'BUTTON', NULL, NULL, 'article:list',   1),
(101, 10, '新增文章', 'BUTTON', NULL, NULL, 'article:add',    2),
(102, 10, '编辑文章', 'BUTTON', NULL, NULL, 'article:edit',   3),
(103, 10, '删除文章', 'BUTTON', NULL, NULL, 'article:delete', 4),
-- 合集管理按钮
(110, 11, '合集列表',   'BUTTON', NULL, NULL, 'collection:list',   1),
(111, 11, '新增合集',   'BUTTON', NULL, NULL, 'collection:add',    2),
(112, 11, '编辑合集',   'BUTTON', NULL, NULL, 'collection:edit',   3),
(113, 11, '删除合集',   'BUTTON', NULL, NULL, 'collection:delete', 4),
-- 标签管理按钮
(120, 12, '标签列表', 'BUTTON', NULL, NULL, 'tag:list',   1),
(121, 12, '新增标签', 'BUTTON', NULL, NULL, 'tag:add',    2),
(122, 12, '编辑标签', 'BUTTON', NULL, NULL, 'tag:edit',   3),
(123, 12, '删除标签', 'BUTTON', NULL, NULL, 'tag:delete', 4),
-- AI 创作助手按钮
(130, 13, '生成选题', 'BUTTON', NULL, NULL, 'ai-agent:topics', 1),
(131, 13, '生成草稿', 'BUTTON', NULL, NULL, 'ai-agent:draft',  2),
-- 轮播管理按钮
(140, 14, '轮播列表', 'BUTTON', NULL, NULL, 'banner:list',   1),
(141, 14, '新增轮播', 'BUTTON', NULL, NULL, 'banner:add',    2),
(142, 14, '编辑轮播', 'BUTTON', NULL, NULL, 'banner:edit',   3),
(143, 14, '删除轮播', 'BUTTON', NULL, NULL, 'banner:delete', 4),
-- 管理员管理按钮
(200, 20, '管理员列表', 'BUTTON', NULL, NULL, 'admin:list',   1),
(201, 20, '新增管理员', 'BUTTON', NULL, NULL, 'admin:add',    2),
(202, 20, '编辑管理员', 'BUTTON', NULL, NULL, 'admin:edit',   3),
(203, 20, '删除管理员', 'BUTTON', NULL, NULL, 'admin:delete', 4),
-- 角色管理按钮
(210, 21, '角色列表',     'BUTTON', NULL, NULL, 'role:list',        1),
(211, 21, '新增角色',     'BUTTON', NULL, NULL, 'role:add',         2),
(212, 21, '编辑角色',     'BUTTON', NULL, NULL, 'role:edit',        3),
(213, 21, '删除角色',     'BUTTON', NULL, NULL, 'role:delete',      4),
(214, 21, '分配角色权限', 'BUTTON', NULL, NULL, 'role:assign:menu', 5),
-- 用户管理按钮
(230, 23, '用户列表', 'BUTTON', NULL, NULL, 'user:list',   1),
(231, 23, '封禁用户', 'BUTTON', NULL, NULL, 'user:ban',    2),
(232, 23, '删除用户', 'BUTTON', NULL, NULL, 'user:delete', 3),
-- 网站配置按钮
(240, 24, '配置查看', 'BUTTON', NULL, NULL, 'config:list', 1),
(241, 24, '配置编辑', 'BUTTON', NULL, NULL, 'config:edit', 2),
-- 菜单管理按钮
(220, 22, '菜单列表', 'BUTTON', NULL, NULL, 'menu:list',   1),
(221, 22, '新增菜单', 'BUTTON', NULL, NULL, 'menu:add',    2),
(222, 22, '编辑菜单', 'BUTTON', NULL, NULL, 'menu:edit',   3),
(223, 22, '删除菜单', 'BUTTON', NULL, NULL, 'menu:delete', 4),
-- 操作日志按钮
(250, 25, '操作日志列表', 'BUTTON', NULL, NULL, 'operation-log:list', 1),
-- 数据分析按钮
(260, 26, '数据分析查看', 'BUTTON', NULL, NULL, 'analytics:list', 1),
-- 文件管理按钮
(270, 27, '文件列表', 'BUTTON', NULL, NULL, 'file:list', 1),
(271, 27, '文件上传', 'BUTTON', NULL, NULL, 'file:upload', 2),
(272, 27, '文件删除', 'BUTTON', NULL, NULL, 'file:delete', 3),
-- 运行日志按钮
(280, 28, '运行日志查看', 'BUTTON', NULL, NULL, 'runtime-log:list', 1),
-- 仪表盘按钮
(290, 29, '仪表盘查看', 'BUTTON', NULL, NULL, 'dashboard:list', 1);

-- ---------------------------------------------------------
-- 6. 角色菜单关联表
-- ---------------------------------------------------------
CREATE TABLE `role_menu` (
  `id`      int NOT NULL AUTO_INCREMENT,
  `role_id` int NOT NULL                                 COMMENT '角色ID',
  `menu_id` int NOT NULL                                 COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) COMMENT '角色菜单关联表';

-- SUPER 角色拥有全部菜单（含写操作按钮）
INSERT INTO `role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `menu`;

-- REGULAR 角色只拥有列表类只读按钮
INSERT INTO `role_menu` (`role_id`, `menu_id`)
SELECT 2, id FROM `menu`
WHERE `type` IN ('CATALOG', 'MENU')
   OR `perm` LIKE '%:list';

-- ---------------------------------------------------------
-- 7. 合集表
-- ---------------------------------------------------------
CREATE TABLE `collection` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `name`        varchar(100) NOT NULL                    COMMENT '合集名称',
  `cover`       varchar(500) DEFAULT NULL                COMMENT '合集封面图',
  `description` varchar(500) DEFAULT NULL                COMMENT '合集简介',
  `sort`        int          NOT NULL DEFAULT 0          COMMENT '排序，越大越靠前',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '合集表';

-- ---------------------------------------------------------
-- 8. 标签表
-- ---------------------------------------------------------
CREATE TABLE `tag` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `name`        varchar(50)  NOT NULL UNIQUE             COMMENT '标签名称',
  `color`       varchar(20)  DEFAULT '#409EFF'           COMMENT '标签颜色（十六进制）',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '标签表';

-- ---------------------------------------------------------
-- 9. 文章表
-- ---------------------------------------------------------
CREATE TABLE `article` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `title`         varchar(200) NOT NULL                  COMMENT '文章标题',
  `cover`         varchar(500) DEFAULT NULL              COMMENT '封面图地址（单封面）',
  `summary`       varchar(500) DEFAULT NULL              COMMENT '文章摘要',
  `content`       mediumtext   DEFAULT NULL              COMMENT 'HTML 渲染内容',
  `content_md`    mediumtext   DEFAULT NULL              COMMENT 'Markdown 原文',
  `keywords`      varchar(200) DEFAULT NULL              COMMENT 'SEO 关键词',
  `ai_describe`   mediumtext   DEFAULT NULL              COMMENT 'AI 生成的简短描述',
  `collection_id` int          DEFAULT NULL              COMMENT '所属合集ID，NULL 表示不加入任何合集',
  `status`        tinyint      NOT NULL DEFAULT 0        COMMENT '0=草稿 1=已发布 2=已下架',
  `publish_time`  datetime     DEFAULT NULL              COMMENT '定时发布时间，NULL 表示不定时发布',
  `is_stick`      tinyint      NOT NULL DEFAULT 0        COMMENT '是否置顶：0=否 1=是',
  `is_carousel`   tinyint      NOT NULL DEFAULT 0        COMMENT '是否加入轮播：0=否 1=是（最多5篇，应用层控制）',
  `carousel_sort` int          NOT NULL DEFAULT 0        COMMENT '轮播排序（is_carousel=1 时有效）',
  `is_original`   tinyint      NOT NULL DEFAULT 1        COMMENT '0=转载 1=原创',
  `original_url`  varchar(500) DEFAULT NULL              COMMENT '转载原文链接',
  `view_count`    bigint       NOT NULL DEFAULT 0        COMMENT '阅读量',
  `like_count`    bigint       NOT NULL DEFAULT 0        COMMENT '点赞数',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FULLTEXT KEY `idx_ft_title` (`title`),
  KEY `idx_status_stick` (`status`, `is_stick`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_collection` (`collection_id`),
  KEY `idx_carousel` (`is_carousel`),
  PRIMARY KEY (`id`)
) COMMENT '文章表';

-- ---------------------------------------------------------
-- 10. 文章图片表（文章内多图，独立存储）
-- ---------------------------------------------------------
CREATE TABLE `article_image` (
  `id`          bigint       NOT NULL AUTO_INCREMENT,
  `article_id`  bigint       NOT NULL                   COMMENT '所属文章ID',
  `url`         varchar(500) NOT NULL                   COMMENT '图片访问地址',
  `sort`        int          NOT NULL DEFAULT 0         COMMENT '排序，越小越靠前',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`)
) COMMENT '文章图片表（支持一篇文章多张图片）';

-- ---------------------------------------------------------
-- 11. 文章标签关联表（多对多）
-- ---------------------------------------------------------
CREATE TABLE `article_tag` (
  `id`         int    NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL                          COMMENT '文章ID',
  `tag_id`     int    NOT NULL                          COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) COMMENT '文章标签关联表';

-- ---------------------------------------------------------
-- 12. 轮播图表（文章轮播数为 0 时展示）
-- ---------------------------------------------------------
CREATE TABLE `banner` (
  `id`          int          NOT NULL AUTO_INCREMENT,
  `image_url`   varchar(500) NOT NULL                   COMMENT '图片地址',
  `link_url`    varchar(500) DEFAULT NULL               COMMENT '点击跳转链接',
  `title`       varchar(100) DEFAULT NULL               COMMENT '图片标题（可叠加展示在图片上）',
  `sort`        int          NOT NULL DEFAULT 0         COMMENT '排序，越大越靠前',
  `status`      tinyint      NOT NULL DEFAULT 1         COMMENT '0=禁用 1=启用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '轮播图表';

-- ---------------------------------------------------------
-- 14. 网站配置表（单行）
-- ---------------------------------------------------------
CREATE TABLE `web_config` (
  `id`            int          NOT NULL AUTO_INCREMENT,
  `site_name`     varchar(100) DEFAULT '我的博客'        COMMENT '网站名称',
  `logo`          varchar(500) DEFAULT NULL             COMMENT 'Logo 地址',
  `summary`       varchar(500) DEFAULT NULL             COMMENT '网站简介',
  `author`        varchar(50)  DEFAULT NULL             COMMENT '作者名',
  `author_avatar` varchar(500) DEFAULT NULL             COMMENT '作者头像',
  `signature`     varchar(200) DEFAULT NULL             COMMENT '个性签名',
  `github`        varchar(200) DEFAULT NULL             COMMENT 'GitHub 主页',
  `email`         varchar(100) DEFAULT NULL             COMMENT '联系邮箱',
  `about_me`      mediumtext   DEFAULT NULL             COMMENT '关于我（Markdown）',
  `icp_number`    varchar(50)  DEFAULT NULL             COMMENT 'ICP 备案号',
  `bulletin`      varchar(1000) DEFAULT NULL            COMMENT '公告内容',
  `open_comment`  tinyint      NOT NULL DEFAULT 1       COMMENT '是否开启评论：0=否 1=是',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT '网站配置表（仅一行）';

INSERT INTO `web_config` (`site_name`) VALUES ('kyonelife');

-- ---------------------------------------------------------
-- 15. 后台操作日志表
-- ---------------------------------------------------------
CREATE TABLE `operation_log` (
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

-- ---------------------------------------------------------
-- 16. 前台行为埋点明细表
-- ---------------------------------------------------------
CREATE TABLE `event_log` (
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

-- ---------------------------------------------------------
-- 17. 前台行为每日聚合表
-- ---------------------------------------------------------
CREATE TABLE `event_daily_stat` (
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

-- ---------------------------------------------------------
-- 18. 文章每日聚合表
-- ---------------------------------------------------------
CREATE TABLE `article_daily_stat` (
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

-- ---------------------------------------------------------
-- 19. 标签每日点击聚合表
-- ---------------------------------------------------------
CREATE TABLE `tag_daily_stat` (
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

-- ---------------------------------------------------------
-- 20. 合集每日点击聚合表
-- ---------------------------------------------------------
CREATE TABLE `collection_daily_stat` (
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

-- ---------------------------------------------------------
-- 21. 搜索关键词每日聚合表
-- ---------------------------------------------------------
CREATE TABLE `search_keyword_stat` (
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

-- AI 调用日志表（成本与可观测）
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id`                BIGINT      NOT NULL AUTO_INCREMENT,
  `scene`             VARCHAR(32) NOT NULL                COMMENT '调用场景：summary/tags/agent-topics/agent-draft',
  `model`             VARCHAR(64)          DEFAULT NULL   COMMENT '模型，如 deepseek-chat',
  `prompt_hash`       CHAR(64)             DEFAULT NULL   COMMENT '输入内容 SHA-256，用于缓存命中统计',
  `cache_hit`         TINYINT              DEFAULT 0      COMMENT '是否命中缓存：0 否 1 是',
  `success`           TINYINT              DEFAULT 0      COMMENT '是否成功：0 否(已降级) 1 是',
  `latency_ms`        BIGINT               DEFAULT NULL   COMMENT '耗时毫秒（命中缓存为 0）',
  `prompt_tokens`     INT                  DEFAULT NULL   COMMENT '输入 token',
  `completion_tokens` INT                  DEFAULT NULL   COMMENT '输出 token',
  `total_tokens`      INT                  DEFAULT NULL   COMMENT '总 token',
  `step_no`           INT                  DEFAULT NULL   COMMENT 'Agent 任务内的步序（第几轮模型调用）；非 Agent 为空',
  `tool_name`         VARCHAR(128)         DEFAULT NULL   COMMENT '该步模型请求调用的工具名（逗号分隔）；最终步为空',
  `error_message`     VARCHAR(500)         DEFAULT NULL   COMMENT '失败或降级原因',
  `create_time`       DATETIME             DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene_time` (`scene`, `create_time`),
  KEY `idx_prompt_hash` (`prompt_hash`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI 调用日志';
