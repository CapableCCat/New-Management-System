-- ============================================================
-- OSC 社团管理系统 · 02 种子数据（字典 + 系统配置）
--
-- 幂等约定
--   - sys_dict：ON DUPLICATE KEY UPDATE 只刷新 label / sort / remark，
--     **不覆盖 enabled**，避免把管理员手动停用的条目重新打开；
--     （VALUES() 写法在 MySQL 8.0.20+ 会有弃用告警，但兼容 5.7，故保留）
--   - sys_config：INSERT IGNORE，不覆盖已经改过的短信模板
--
-- 说明：学院 / 专业 / 兴趣标签三类字典目前只放「其他（other）」兜底，
--       等社长给出种子清单后补录（可直接用 T5 字典管理页维护）。
--
-- 执行：mysql --user=root --password=*** --execute="source F:/.../server/sql/02_seed_dict.sql"
-- ============================================================

SET NAMES utf8mb4;
USE `osc`;

-- ------------------------------------------------------------
-- 1. 字典：部门（PRD §6.4，编码冻结）
-- ------------------------------------------------------------
INSERT INTO `sys_dict` (`type`, `code`, `label`, `sort`, `enabled`, `remark`) VALUES
('department', '0', '社长团', 1, 1, '全社管理资格：department=0'),
('department', '1', '技术部', 2, 1, NULL),
('department', '2', '运营部', 3, 1, NULL),
('department', '3', '宣传部', 4, 1, NULL),
('department', '4', '秘书处', 5, 1, NULL)
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`), `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 2. 字典：职位（编码冻结）
-- ------------------------------------------------------------
INSERT INTO `sys_dict` (`type`, `code`, `label`, `sort`, `enabled`, `remark`) VALUES
('duty', '0', '成员', 1, 1, NULL),
('duty', '1', '副部长', 2, 1, NULL),
('duty', '2', '部长', 3, 1, '部门管理资格：duty=2'),
('duty', '3', '社长', 4, 1, NULL)
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`), `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 3. 字典：账号状态（编码冻结）
-- ------------------------------------------------------------
INSERT INTO `sys_dict` (`type`, `code`, `label`, `sort`, `enabled`, `remark`) VALUES
('status', '0', '正常', 1, 1, NULL),
('status', '1', '冻结', 2, 1, '封禁 / 已离社共用')
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`), `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 4. 字典：反馈来源
-- ------------------------------------------------------------
INSERT INTO `sys_dict` (`type`, `code`, `label`, `sort`, `enabled`, `remark`) VALUES
('feedback_source', '1', '报名成功页', 1, 1, NULL),
('feedback_source', '2', '成员端', 2, 1, NULL)
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`), `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 5. 字典：学院 / 专业 / 兴趣标签（暂只有「其他」兜底，待补种子清单）
-- ------------------------------------------------------------
INSERT INTO `sys_dict` (`type`, `code`, `label`, `sort`, `enabled`, `remark`) VALUES
('college', 'other', '其他', 999, 1, '兜底项；学校学院清单待补'),
('major',   'other', '其他', 999, 1, '兜底项；专业清单待补（选中后前端要求填 major_text）'),
('tag',     'other', '其他', 999, 1, '兜底项；兴趣标签清单待补')
ON DUPLICATE KEY UPDATE `label` = VALUES(`label`), `sort` = VALUES(`sort`), `remark` = VALUES(`remark`);

-- ------------------------------------------------------------
-- 6. 系统配置：系统链接 + 短信模板（变量集见《开发任务点清单》§6 D7）
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('system_url', 'http://127.0.0.1:5173', '系统访问地址；dev 为前端 Vite 端口，上线前改为公网 HTTPS 地址'),
('sms_template_pass',
 '{姓名}同学你好，你的入社申请已通过审核。请用手机号登录 {系统链接}（初始密码：{初始密码}），首次登录需修改密码。',
 '审核通过通知模板；变量：{姓名} {系统链接} {初始密码}'),
('sms_template_reject',
 '{姓名}同学你好，感谢你报名开源鸿蒙社。很抱歉，本次未能通过：{拒绝原因}。欢迎关注我们后续的活动与纳新通知。',
 '审核拒绝通知模板；变量：{姓名} {系统链接} {拒绝原因}');

-- ------------------------------------------------------------
-- 自检：按类型统计字典条目
-- ------------------------------------------------------------
SELECT `type`, COUNT(*) AS `total`, SUM(`enabled`) AS `enabled_count`
FROM `sys_dict` GROUP BY `type` ORDER BY `type`;

SELECT `config_key`, LEFT(`config_value`, 40) AS `value_preview` FROM `sys_config` ORDER BY `id`;
