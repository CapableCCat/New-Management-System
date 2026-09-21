-- ============================================================
-- OSC 社团管理系统 · 04 结构变更（幂等）
--
-- 用途：对**已建好的库**补齐后续任务点新增的字段；新建库直接跑 01_schema.sql 即可，
--       不需要执行本脚本（01 里已经包含这些列）。
--
-- 执行：mysql --user=root --password=*** --execute="source F:/.../server/sql/04_alter.sql"
-- ============================================================

SET NAMES utf8mb4;
USE `osc`;

-- ------------------------------------------------------------
-- T6：报名记录新增「兴趣标签-其他」手填值
--   背景：兴趣标签方案里「其他（自由补充）」选中后需要有个地方存自由文本
--   幂等：仅当列不存在时才 ADD COLUMN
-- ------------------------------------------------------------
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recruit_apply'
      AND COLUMN_NAME = 'tag_text'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `recruit_apply` ADD COLUMN `tag_text` VARCHAR(64) DEFAULT NULL COMMENT ''兴趣标签「其他」手填值'' AFTER `tags`',
    'SELECT ''recruit_apply.tag_text 已存在，跳过'' AS note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 自检：确认字段就位
-- ------------------------------------------------------------
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recruit_apply' AND COLUMN_NAME = 'tag_text';
