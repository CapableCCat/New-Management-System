-- ============================================================
-- OSC 社团管理系统 · 01 表结构
--
-- 约定
--   1) 只 CREATE TABLE IF NOT EXISTS，**不含 DROP**：重复执行安全，
--      也避免线上误执行导致数据丢失；需要重建请手动 DROP。
--   2) 引擎 InnoDB，字符集 utf8mb4 / utf8mb4_general_ci（兼容 MySQL 5.7 与 8.0）。
--   3) 不建物理外键：关系由应用层校验 + 索引保证，便于批量导入与旧数据迁移。
--   4) 每张表统一带审计四字段 + is_deleted（见《开发任务点清单》§6 D11）。
--   5) 唯一索引**不带 is_deleted**：手机号/学号一旦占用即永久占用（语义更强）；
--      user 用 status=1（冻结）代替删除，sys_dict 用 enabled=0（停用）代替删除。
--
-- 执行：mysql --user=root --password=*** --execute="source F:/.../server/sql/01_schema.sql"
-- ============================================================

SET NAMES utf8mb4;
USE `osc`;

-- ------------------------------------------------------------
-- 1. user 正式成员（仅审核通过者，含本届与往届）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`          VARCHAR(11)     NOT NULL                COMMENT '手机号（业务主键，全局唯一）',
    `password`       VARCHAR(100)    NOT NULL                COMMENT '密码（BCrypt 密文）',
    `name`           VARCHAR(32)     NOT NULL                COMMENT '姓名（仅管理员可改）',
    `student_id`     VARCHAR(32)     DEFAULT NULL            COMMENT '学号（可空，全局唯一）',
    `college`        VARCHAR(32)     DEFAULT NULL            COMMENT '学院（sys_dict: college.code）',
    `major`          VARCHAR(32)     DEFAULT NULL            COMMENT '专业（sys_dict: major.code）',
    `major_text`     VARCHAR(64)     DEFAULT NULL            COMMENT '专业选「其他」时的手填值',
    `department`     TINYINT         DEFAULT NULL            COMMENT '部门 0社长团 1技术部 2运营部 3宣传部 4秘书处（可空，入社后分配）',
    `duty`           TINYINT         NOT NULL DEFAULT 0      COMMENT '职位 0成员 1副部长 2部长 3社长',
    `role`           TINYINT         NOT NULL DEFAULT 0      COMMENT '角色 0普通成员 2超管',
    `status`         TINYINT         NOT NULL DEFAULT 0      COMMENT '状态 0正常 1冻结（封禁/离社共用）',
    `gender`         TINYINT         NOT NULL DEFAULT 0      COMMENT '性别 0未填 1男 2女',
    `province`       VARCHAR(32)     DEFAULT NULL            COMMENT '生源地-省',
    `city`           VARCHAR(32)     DEFAULT NULL            COMMENT '生源地-市',
    `avatar_url`     VARCHAR(255)    DEFAULT NULL            COMMENT '头像地址（MinIO，T11 接入）',
    `bio`            TEXT                                    COMMENT '个人简介（富文本，渲染前清洗）',
    `feishu_open_id` VARCHAR(64)     DEFAULT NULL            COMMENT '飞书 open_id（可空唯一；免登 Stretch）',
    `activated_at`   DATETIME        DEFAULT NULL            COMMENT '首登改密完成时间（NULL=未激活→强制改密）',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（即加入时间）',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '创建人 user.id',
    `updated_by`     BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted`     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    UNIQUE KEY `uk_user_student_id` (`student_id`),
    UNIQUE KEY `uk_user_feishu_open_id` (`feishu_open_id`),
    KEY `idx_user_department_status` (`department`, `status`),
    KEY `idx_user_duty` (`duty`),
    KEY `idx_user_college` (`college`),
    KEY `idx_user_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '正式社团成员（仅审核通过者）';

-- ------------------------------------------------------------
-- 2. recruit_apply 纳新报名记录（全量：待审/通过/拒绝）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `recruit_apply` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`               VARCHAR(32)     NOT NULL                COMMENT '姓名',
    `phone`              VARCHAR(11)     NOT NULL                COMMENT '手机号（报名唯一标识）',
    `college`            VARCHAR(32)     NOT NULL                COMMENT '学院（sys_dict: college.code）',
    `major`              VARCHAR(32)     NOT NULL                COMMENT '专业（sys_dict: major.code）',
    `major_text`         VARCHAR(64)     DEFAULT NULL            COMMENT '专业选「其他」时的手填值',
    `intent_departments` JSON            NOT NULL                COMMENT '意向部门（多选，code 数组，如 [1,3]）',
    `tags`               JSON            DEFAULT NULL            COMMENT '兴趣标签（多选，code 数组）',
    `tag_text`           VARCHAR(64)     DEFAULT NULL            COMMENT '兴趣标签选「其他」时的手填值',
    `gender`             TINYINT         NOT NULL DEFAULT 0      COMMENT '性别 0未填 1男 2女',
    `province`           VARCHAR(32)     DEFAULT NULL            COMMENT '生源地-省',
    `city`               VARCHAR(32)     DEFAULT NULL            COMMENT '生源地-市',
    `status`             TINYINT         NOT NULL DEFAULT 0      COMMENT '状态 0待审 1通过 2拒绝',
    `reject_reason`      VARCHAR(255)    DEFAULT NULL            COMMENT '拒绝原因（对外措辞）',
    `reviewer_id`        BIGINT UNSIGNED DEFAULT NULL            COMMENT '审核人 user.id',
    `reviewed_at`        DATETIME        DEFAULT NULL            COMMENT '审核时间',
    `user_id`            BIGINT UNSIGNED DEFAULT NULL            COMMENT '审核通过后回填的 user.id（溯源用）',
    `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `updated_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`         BIGINT UNSIGNED DEFAULT NULL            COMMENT '创建人 user.id',
    `updated_by`         BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_apply_phone` (`phone`),
    KEY `idx_apply_status` (`status`),
    KEY `idx_apply_college` (`college`),
    KEY `idx_apply_created_at` (`created_at`),
    KEY `idx_apply_reviewer` (`reviewer_id`),
    KEY `idx_apply_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '纳新报名记录（全量，审核状态只在本表维护）';

-- ------------------------------------------------------------
-- 3. sys_dict 字典（部门/职位/状态/学院/专业/兴趣标签/反馈来源）
--    约定：只启停（enabled）不删除，保证 UNIQUE(type, code) 稳定；核心编码不可改
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`       VARCHAR(32)     NOT NULL                COMMENT '类型 department/duty/status/college/major/tag/feedback_source',
    `code`       VARCHAR(32)     NOT NULL                COMMENT '编码（核心条目不可修改）',
    `label`      VARCHAR(64)     NOT NULL                COMMENT '文案（可修改）',
    `sort`       INT             NOT NULL DEFAULT 0      COMMENT '排序（升序）',
    `enabled`    TINYINT         NOT NULL DEFAULT 1      COMMENT '启用 0停用 1启用',
    `remark`     VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '创建人 user.id',
    `updated_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted` TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type_code` (`type`, `code`),
    KEY `idx_dict_type_enabled_sort` (`type`, `enabled`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典（全站引用，编码冻结）';

-- ------------------------------------------------------------
-- 4. sys_config 系统配置（短信模板 / 系统链接等）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`   VARCHAR(64)     NOT NULL                COMMENT '配置键',
    `config_value` TEXT            NOT NULL                COMMENT '配置值（短信模板支持 {变量} 占位）',
    `remark`       VARCHAR(255)    DEFAULT NULL            COMMENT '备注',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '创建人 user.id',
    `updated_by`   BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted`   TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统配置';

-- ------------------------------------------------------------
-- 5. announcement 公告（发布时间复用 created_at）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `announcement` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`      VARCHAR(128)    NOT NULL                COMMENT '标题',
    `content`    MEDIUMTEXT      NOT NULL                COMMENT '富文本内容（存储前做 XSS 白名单清洗）',
    `is_top`     TINYINT         NOT NULL DEFAULT 0      COMMENT '置顶 0否 1是',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '发布人 user.id',
    `updated_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted` TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_ann_top_created` (`is_top`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告';

-- ------------------------------------------------------------
-- 6. feedback 轻量反馈（提交免登录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `feedback` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `content`    VARCHAR(1000)   NOT NULL                COMMENT '反馈内容',
    `contact`    VARCHAR(64)     DEFAULT NULL            COMMENT '联系方式（选填）',
    `source`     TINYINT         NOT NULL DEFAULT 0      COMMENT '来源 1报名成功页 2成员端（sys_dict: feedback_source）',
    `handled`    TINYINT         NOT NULL DEFAULT 0      COMMENT '处理状态 0未处理 1已处理',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '创建人 user.id',
    `updated_by` BIGINT UNSIGNED DEFAULT NULL            COMMENT '更新人 user.id',
    `is_deleted` TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_fb_created_at` (`created_at`),
    KEY `idx_fb_source` (`source`),
    KEY `idx_fb_handled` (`handled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '轻量反馈';

-- ------------------------------------------------------------
-- 自检：列出本库表与行数
-- ------------------------------------------------------------
SELECT TABLE_NAME, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'osc'
ORDER BY TABLE_NAME;
