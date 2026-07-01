-- =========================================================
-- 后台新增模块权限补丁
-- 用于已有库升级；新库初始化请直接执行 init.sql。
-- =========================================================

USE kyonelife;

INSERT IGNORE INTO `menu` (`id`, `parent_id`, `title`, `type`, `path`, `component`, `perm`, `sort`) VALUES
(13, 1, 'AI 创作助手', 'MENU', '/content/agent', 'content/agent/index', NULL, 4),
(130, 13, '生成选题', 'BUTTON', NULL, NULL, 'ai-agent:topics', 1),
(131, 13, '生成草稿', 'BUTTON', NULL, NULL, 'ai-agent:draft', 2),
(26, 2, '数据分析', 'MENU', '/system/analytics', 'system/analytics/index', NULL, 7),
(27, 2, '文件管理', 'MENU', '/system/file', 'system/file/index', NULL, 8),
(28, 2, '运行日志', 'MENU', '/system/log', 'system/log/index', NULL, 9),
(29, 2, '仪表盘', 'MENU', '/dashboard', 'dashboard/index', NULL, 0),
(240, 24, '配置查看', 'BUTTON', NULL, NULL, 'config:list', 1),
(241, 24, '配置编辑', 'BUTTON', NULL, NULL, 'config:edit', 2),
(260, 26, '数据分析查看', 'BUTTON', NULL, NULL, 'analytics:list', 1),
(270, 27, '文件列表', 'BUTTON', NULL, NULL, 'file:list', 1),
(271, 27, '文件上传', 'BUTTON', NULL, NULL, 'file:upload', 2),
(272, 27, '文件删除', 'BUTTON', NULL, NULL, 'file:delete', 3),
(280, 28, '运行日志查看', 'BUTTON', NULL, NULL, 'runtime-log:list', 1),
(290, 29, '仪表盘查看', 'BUTTON', NULL, NULL, 'dashboard:list', 1);

-- SUPER 角色拥有新增权限。
INSERT IGNORE INTO `role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `menu`
WHERE id IN (13, 26, 27, 28, 29, 130, 131, 240, 241, 260, 270, 271, 272, 280, 290);

-- REGULAR 角色只授予目录/菜单和只读按钮。
INSERT IGNORE INTO `role_menu` (`role_id`, `menu_id`)
SELECT 2, id FROM `menu`
WHERE id IN (13, 26, 27, 28, 29, 240, 260, 270, 280, 290);
