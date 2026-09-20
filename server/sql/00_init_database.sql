-- ============================================================
-- OSC 社团管理系统 · 00 建库与业务账号
-- 执行者：MySQL root（业务账号 osc_app 无权 DDL）
-- 执行方式（PowerShell 5.1，PS 不支持 < 重定向）：
--   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --password=***
--       --default-character-set=utf8mb4 --execute="source F:/项目路径/server/sql/00_init_database.sql"
--
-- ⚠️ 安全约定
--   1) 本文件是**模板**，密码占位符不得提交真实值；
--      本地/线上执行时请用内联 --execute 的方式传真实密码（见 README 说明），
--      或临时改本文件后**不要提交**。
--   2) 重复执行会重置 osc_app 的密码（ALTER USER），线上慎用。
--   3) 账号只授予 DML，DDL 由 root 执行；host 限定 localhost（最小权限）。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 建库（字符集 utf8mb4，兼容 MySQL 5.7 与 8.0 的排序规则）
CREATE DATABASE IF NOT EXISTS `osc`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 2. 业务账号（把 CHANGE_ME_StrongPwd 换成强密码）
SET @osc_app_password = 'CHANGE_ME_StrongPwd';

SET @sql = CONCAT('CREATE USER IF NOT EXISTS ''osc_app''@''localhost'' IDENTIFIED BY ''', @osc_app_password, '''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 重置密码（换成强密码后执行；仅需改密码时也可单独执行本段）
SET @sql = CONCAT('ALTER USER ''osc_app''@''localhost'' IDENTIFIED BY ''', @osc_app_password, '''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 只授权 osc 库的 DML（不含 DDL / GRANT / FILE 等）
GRANT SELECT, INSERT, UPDATE, DELETE ON `osc`.* TO 'osc_app'@'localhost';

FLUSH PRIVILEGES;

-- 5. 若应用与数据库不在同一台机器，按需追加更精确的 host（不要用 '%'）：
-- CREATE USER IF NOT EXISTS 'osc_app'@'10.0.0.5' IDENTIFIED BY '<强密码>';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `osc`.* TO 'osc_app'@'10.0.0.5';

-- 6. 自检
SELECT user, host FROM mysql.user WHERE user = 'osc_app';
SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'osc';
