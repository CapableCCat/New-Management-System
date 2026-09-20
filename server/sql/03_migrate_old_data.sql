-- ============================================================
-- OSC 社团管理系统 · 03 旧数据迁移（框架 / 人工执行）
--
-- ⚠️ 现状：旧库在内网 172.19.15.13，当前不可连，本脚本按**框架**交付：
--    先人工把旧 user 数据导出成 staging 表，再逐步执行下面各段。
--    直接执行本脚本不会误改数据（staging 为空时第 6 段插入 0 行）。
--
-- 映射规则（PRD §6.6 + 《开发任务点清单》§6）
--   1) 以手机号去重；手机号为空的记录**不建账号**，输出「待补录清单」
--   2) 旧 code → student_id
--   3) 旧 role=1（管理员）不沿用；新超管只由 F-012 初始化或人工指派（role=2）
--   4) 旧 status 2/3（待通过/未通过）不是正式成员 → 不迁移
--   5) 旧密码（明文/MD5）不迁移：写入哨兵值，需管理员重置密码后才能登录
--   6) 冲突清单（手机号/学号重复）交给人工判定
-- ============================================================

SET NAMES utf8mb4;
USE `osc`;

-- ------------------------------------------------------------
-- 步骤 1：建 staging 表（列按旧导出实际情况增删）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `old_user_stage` (
    `old_id`      BIGINT       DEFAULT NULL COMMENT '旧主键',
    `name`        VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    `phone`       VARCHAR(32)  DEFAULT NULL COMMENT '手机号（可能带空格/+86）',
    `code`        VARCHAR(32)  DEFAULT NULL COMMENT '旧学号字段',
    `college`     VARCHAR(64)  DEFAULT NULL COMMENT '学院（旧为中文文本）',
    `major`       VARCHAR(64)  DEFAULT NULL COMMENT '专业（旧为中文文本）',
    `department`  VARCHAR(32)  DEFAULT NULL COMMENT '旧部门编码',
    `duty`        VARCHAR(32)  DEFAULT NULL COMMENT '旧职位编码',
    `role`        VARCHAR(32)  DEFAULT NULL COMMENT '旧角色编码',
    `status`      VARCHAR(32)  DEFAULT NULL COMMENT '旧状态编码',
    `gender`      VARCHAR(8)   DEFAULT NULL COMMENT '性别（旧可能为中文/数字）',
    `province`    VARCHAR(32)  DEFAULT NULL COMMENT '生源地-省',
    `city`        VARCHAR(32)  DEFAULT NULL COMMENT '生源地-市',
    `create_time` DATETIME     DEFAULT NULL COMMENT '旧创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '迁移用 staging（迁移完成后可 DROP）';

-- ------------------------------------------------------------
-- 步骤 2：把旧数据灌进 staging（三选一）
--   a) 旧库可连时（在旧库侧执行导出，再导入本库）：
--      INSERT INTO osc.old_user_stage (old_id, name, phone, code, ...)
--      SELECT id, name, phone, code, ... FROM <旧库>.user;
--   b) CSV：LOAD DATA LOCAL INFILE 'D:/old_user.csv' INTO TABLE old_user_stage
--      FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\r\n'
--      IGNORE 1 LINES;   -- 需要服务端 local_infile=ON
--   c) Navicat / Workbench 直接导入 CSV 到 old_user_stage
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 步骤 3：规范化手机号（去空格、去 +86、去横线）
-- ------------------------------------------------------------
UPDATE `old_user_stage`
SET `phone` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(TRIM(`phone`), ' ', ''), '-', ''), '+86', ''), CHAR(9), ''), CHAR(13), '')
WHERE `phone` IS NOT NULL;

-- ------------------------------------------------------------
-- 步骤 4：待补录清单（手机号空 / 非 11 位 → 不建账号，交管理员补录）
-- ------------------------------------------------------------
SELECT '待补录清单' AS `list_type`, `old_id`, `name`, `phone`, `code`, `college`, `major`
FROM `old_user_stage`
WHERE `phone` IS NULL OR `phone` = '' OR CHAR_LENGTH(`phone`) <> 11
ORDER BY `old_id`;

-- ------------------------------------------------------------
-- 步骤 5：冲突清单（人工判定保留哪条）
-- ------------------------------------------------------------
SELECT '手机号重复' AS `conflict_type`, `phone`, COUNT(*) AS `cnt`, GROUP_CONCAT(`old_id`) AS `old_ids`
FROM `old_user_stage`
WHERE CHAR_LENGTH(`phone`) = 11
GROUP BY `phone` HAVING COUNT(*) > 1;

SELECT '学号重复' AS `conflict_type`, `code`, COUNT(*) AS `cnt`, GROUP_CONCAT(`old_id`) AS `old_ids`
FROM `old_user_stage`
WHERE `code` IS NOT NULL AND TRIM(`code`) <> ''
GROUP BY `code` HAVING COUNT(*) > 1;

-- ------------------------------------------------------------
-- 步骤 6：正式迁移
--   ⚠️ 旧部门/职位编码与新字典口径不同（旧后端 0运营/1技术/2宣传/3其它；
--      新字典 0社长团/1技术部/2运营部/3宣传部/4秘书处），下表映射为**建议值**，
--      执行前请与社长确认；无法判定的置 NULL，由成员档案页人工补齐。
--   旧职位映射：旧0→新0成员、旧2→新1副部长、旧3→新2部长、旧4→新3社长，其余 NULL→0
-- ------------------------------------------------------------
INSERT INTO `user`
(`phone`, `password`, `name`, `student_id`, `college`, `major`, `department`, `duty`, `role`, `status`,
 `gender`, `province`, `city`, `activated_at`, `created_at`)
SELECT
    s.`phone`,
    'MIGRATED_RESET_REQUIRED'                                   AS `password`,
    COALESCE(NULLIF(TRIM(s.`name`), ''), '待补录')               AS `name`,
    NULLIF(TRIM(s.`code`), '')                                  AS `student_id`,
    NULLIF(TRIM(s.`college`), '')                               AS `college`,
    NULLIF(TRIM(s.`major`), '')                                 AS `major`,
    CASE s.`department`
        WHEN '0' THEN 2   -- 旧 运营 → 新 运营部
        WHEN '1' THEN 1   -- 旧 技术 → 新 技术部
        WHEN '2' THEN 3   -- 旧 宣传 → 新 宣传部
        ELSE NULL         -- 旧 其它 / 未知 → 留空待人工分配
    END                                                          AS `department`,
    CASE s.`duty`
        WHEN '0' THEN 0
        WHEN '2' THEN 1
        WHEN '3' THEN 2
        WHEN '4' THEN 3
        ELSE 0
    END                                                          AS `duty`,
    0                                                            AS `role`,
    CASE s.`status` WHEN '1' THEN 1 ELSE 0 END                   AS `status`,
    CASE
        WHEN s.`gender` IN ('1') OR s.`gender` = '男' THEN 1
        WHEN s.`gender` IN ('2') OR s.`gender` = '女' THEN 2
        ELSE 0
    END                                                          AS `gender`,
    NULLIF(TRIM(s.`province`), '')                              AS `province`,
    NULLIF(TRIM(s.`city`), '')                                  AS `city`,
    NULL                                                         AS `activated_at`,
    COALESCE(s.`create_time`, NOW())                             AS `created_at`
FROM `old_user_stage` s
WHERE CHAR_LENGTH(s.`phone`) = 11
  AND s.`status` IN ('0', '1')                                            -- 仅正式成员（正常/封号）
  AND NOT EXISTS (SELECT 1 FROM `user` u WHERE u.`phone` = s.`phone`)     -- 手机号去重
  AND NOT EXISTS (                                                 -- 学号冲突的行跳过，交人工处理
        SELECT 1 FROM `old_user_stage` s2
        WHERE s2.`code` = s.`code` AND s2.`code` IS NOT NULL AND TRIM(s2.`code`) <> ''
        GROUP BY s2.`code` HAVING COUNT(*) > 1
      );

-- ------------------------------------------------------------
-- 步骤 7：迁移核对
-- ------------------------------------------------------------
SELECT 'staging 总数'            AS `item`, COUNT(*) AS `cnt` FROM `old_user_stage`
UNION ALL SELECT '待补录（无手机号）', COUNT(*) FROM `old_user_stage` WHERE `phone` IS NULL OR CHAR_LENGTH(`phone`) <> 11
UNION ALL SELECT '已迁移到 user',      COUNT(*) FROM `user`
UNION ALL SELECT '需重置密码的迁移账号', COUNT(*) FROM `user` WHERE `password` = 'MIGRATED_RESET_REQUIRED';

-- ------------------------------------------------------------
-- 步骤 8：确认无误后清理 staging
-- DROP TABLE `old_user_stage`;
-- ============================================================
