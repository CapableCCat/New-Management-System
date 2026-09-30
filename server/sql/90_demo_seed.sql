-- ============================================================
-- 演示数据 · 种子（T33）
-- ============================================================
-- 用途：库里有真实数据的只有社长 1 人 → 看板图表几乎空的、成员列表只有 1 行，
--       看不出真实版式效果。本脚本造一批**有分布的**演示数据，用于检查看板 / 成员列表 / 报名的观感。
--
-- ⚠️ 上线前必须清理：执行同目录的 `91_demo_cleanup.sql`
--
-- 标记约定（清理脚本据此精确删除，绝不误伤真实数据）：
--   演示成员  手机号 13900000701 ~ 13900000728   （共 28 人）
--   演示报名  手机号 13900000801 ~ 13900000830   （共 30 条）
--
-- 安全说明（重要）：
--   ① **密码是「一次性随机密钥」的 bcrypt 哈希，密钥生成后即弃、不写进任何文件** ——
--      也就是说这些演示账号**谁都登录不了**。刻意不写成"公开密码"，
--      避免重演 O1（夹具账号用公开密码，最后必须清理）那次的情况。
--   ② 演示成员**全部 role=0 且 department 不含 0（社长团）** —— 不产生任何管理角色；
--      仅保留每部门 1 个 duty=2（部长），用于让成员列表的职位列有内容。
--   ③ 手机号段 139 0000 07xx / 08xx 不会有真实用户，无法通过短信找回密码。
--
-- 数据分布（刻意打散，否则饼图只有一块、地图只有一两个省）：
--   学院：软件与通信 6 / 智能制造 4 / 机械工程 4 / 航空航天 3 / 汽车 3 / 经贸 3 / 艺术 3 / 能源 2
--   专业：与学院合理配对，覆盖 1~24 共 18 个专业码
--   性别：男 17 / 女 9 / 未填写 2
--   省份：24 条覆盖 21 个省级行政区（天津最多），另有 2 条未识别（1 空 + 1「海外」）
--   部门：技术部 10 / 运营部 7 / 宣传部 6 / 秘书处 5
-- ============================================================

USE osc;

-- 幂等：先清掉上一次的演示成员，再插入（可反复执行）
DELETE FROM user WHERE phone LIKE '139000007%';

INSERT INTO user
  (phone, password, name, student_id, college, major, major_text, department, duty,
   role, status, gender, province, city, bio, activated_at, created_at, updated_at)
VALUES
('13900000701', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '张明宇', '202100001', '5', '6', NULL, 1, 2, 0, 0, 1, '天津市', '天津市', '喜欢折腾嵌入式，正在学鸿蒙设备开发。', '2025-09-01 10:00:00', NOW(), NOW()),
('13900000702', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '李思远', '202200002', '5', '12', NULL, 1, 0, 0, 0, 1, '天津市', '天津市', NULL, '2025-09-14 17:00:00', NOW(), NOW()),
('13900000703', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '王雨桐', '202300003', '5', '13', NULL, 1, 0, 0, 0, 1, '天津市', '天津市', NULL, '2025-09-28 00:00:00', NOW(), NOW()),
('13900000704', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '刘晨曦', '202400004', '5', '14', NULL, 1, 0, 0, 0, 1, '天津市', '天津市', NULL, '2025-10-11 07:00:00', NOW(), NOW()),
('13900000705', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '陈嘉豪', '202500005', '5', '6', NULL, 1, 0, 0, 0, 1, '河北省', '石家庄市', NULL, '2025-10-23 14:00:00', NOW(), NOW()),
('13900000706', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '杨紫萱', '202100006', '5', '12', NULL, 1, 0, 0, 0, 1, '河北省', '保定市', NULL, '2025-11-05 21:00:00', NOW(), NOW()),
('13900000707', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '赵子轩', '202200007', '2', '4', NULL, 1, 0, 0, 0, 1, '山西省', '太原市', '前端方向，做过几个小工具站。', '2025-11-19 04:00:00', NOW(), NOW()),
('13900000708', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '黄文博', '202300008', '2', '5', NULL, 1, 0, 0, 0, 1, '山东省', '济南市', NULL, '2025-12-01 11:00:00', NOW(), NOW()),
('13900000709', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '周晓航', '202400009', '2', '7', NULL, 1, 0, 0, 0, 1, '山东省', '青岛市', NULL, '2025-12-14 18:00:00', NOW(), NOW()),
('13900000710', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '吴思怡', '202500010', '2', '4', NULL, 1, 0, 0, 0, 1, '河南省', '郑州市', NULL, '2025-12-28 01:00:00', NOW(), NOW()),
('13900000711', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '徐浩宇', '202100011', '1', '2', NULL, 2, 2, 0, 0, 1, '辽宁省', '沈阳市', NULL, '2026-01-10 08:00:00', NOW(), NOW()),
('13900000712', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '孙梦琪', '202200012', '1', '3', NULL, 2, 0, 0, 0, 1, '吉林省', '长春市', NULL, '2026-01-22 15:00:00', NOW(), NOW()),
('13900000713', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '马嘉诚', '202300013', '1', '1', NULL, 2, 0, 0, 0, 1, '黑龙江省', '哈尔滨市', '对操作系统内核感兴趣。', '2026-02-04 22:00:00', NOW(), NOW()),
('13900000714', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '朱若曦', '202400014', '1', '2', NULL, 2, 0, 0, 0, 1, '江苏省', '南京市', NULL, '2026-02-18 05:00:00', NOW(), NOW()),
('13900000715', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '胡安琪', '202500015', '3', '8', NULL, 2, 0, 0, 0, 1, '浙江省', '杭州市', NULL, '2026-03-02 12:00:00', NOW(), NOW()),
('13900000716', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '郭泽宇', '202100016', '3', '9', NULL, 2, 0, 0, 0, 1, '安徽省', '合肥市', NULL, '2026-03-15 19:00:00', NOW(), NOW()),
('13900000717', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '何欣怡', '202200017', '3', '8', NULL, 2, 0, 0, 0, 1, '江西省', '南昌市', NULL, '2026-03-29 02:00:00', NOW(), NOW()),
('13900000718', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '高俊杰', '202300018', '4', '11', NULL, 3, 2, 0, 0, 2, '湖北省', '武汉市', NULL, '2026-04-11 09:00:00', NOW(), NOW()),
('13900000719', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '林墨言', '202400019', '4', '10', NULL, 3, 0, 0, 0, 2, '湖南省', '长沙市', '会一点 Python，想参与社团项目练手。', '2026-04-23 16:00:00', NOW(), NOW()),
('13900000720', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '罗雨辰', '202500020', '4', '11', NULL, 3, 0, 0, 0, 2, '四川省', '成都市', NULL, '2026-05-06 23:00:00', NOW(), NOW()),
('13900000721', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '郑文轩', '202100021', '6', '15', NULL, 3, 0, 0, 0, 2, '陕西省', '西安市', NULL, '2026-05-20 06:00:00', NOW(), NOW()),
('13900000722', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '谢佳宁', '202200022', '6', '16', NULL, 3, 0, 0, 0, 2, '甘肃省', '兰州市', NULL, '2026-06-01 13:00:00', NOW(), NOW()),
('13900000723', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '韩子涵', '202300023', '6', '17', NULL, 3, 0, 0, 0, 2, '内蒙古自治区', '呼和浩特市', NULL, '2026-06-14 20:00:00', NOW(), NOW()),
('13900000724', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '唐嘉怡', '202400024', '7', '21', NULL, 4, 2, 0, 0, 2, '广西壮族自治区', '南宁市', NULL, '2026-06-28 03:00:00', NOW(), NOW()),
('13900000725', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '曹明轩', '202500025', '7', '19', NULL, 4, 0, 0, 0, 2, '云南省', '昆明市', '主要做视觉设计，也写点脚本。', '2026-07-10 10:00:00', NOW(), NOW()),
('13900000726', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '邓思彤', '202100026', '7', '20', NULL, 4, 0, 0, 0, 2, '贵州省', '贵阳市', NULL, '2026-07-23 17:00:00', NOW(), NOW()),
('13900000727', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '冯浩宇', '202200027', '8', '22', NULL, 4, 0, 0, 0, 0, NULL, NULL, NULL, '2026-08-06 00:00:00', NOW(), NOW()),
('13900000728', '$2b$10$pAtRo72rqwKC5ttcOHTqBupqZOQXpkRS.tCHsSgfiTlK.3D4N1OUO', '沈心怡', '202300028', '8', '23', NULL, 4, 0, 0, 0, 0, '海外', NULL, NULL, '2026-08-19 07:00:00', NOW(), NOW());

-- ---------------- 演示报名（30 条，铺在 2026-09 的纳新窗口内）----------------
-- 目的：招新复盘的折线/占比有形状（原来只有 9 条、挤在少数几天）
DELETE FROM recruit_apply WHERE phone LIKE '139000008%';

INSERT INTO recruit_apply
  (name, phone, college, major, major_text, intent_departments, tags, tag_text,
   gender, province, city, status, reject_reason, reviewer_id, reviewed_at,
   created_at, updated_at)
VALUES
('苏一诺', '13900000801', '5', '6', NULL, CAST('["1"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '天津市', '天津市', 0, NULL, NULL, NULL, '2026-09-02 09:00:00', NOW()),
('潘子豪', '13900000802', '5', '12', NULL, CAST('["2"]' AS JSON), NULL, NULL, 1, '天津市', '天津市', 0, NULL, NULL, NULL, '2026-09-01 12:17:00', NOW()),
('范雨欣', '13900000803', '5', '13', NULL, CAST('["3"]' AS JSON), NULL, NULL, 1, '天津市', '天津市', 0, NULL, NULL, NULL, '2026-09-02 15:34:00', NOW()),
('彭嘉文', '13900000804', '5', '14', NULL, CAST('["4"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '天津市', '天津市', 0, NULL, NULL, NULL, '2026-09-03 18:51:00', NOW()),
('董思远', '13900000805', '5', '6', NULL, CAST('["1","3"]' AS JSON), NULL, NULL, 1, '河北省', '石家庄市', 0, NULL, NULL, NULL, '2026-09-04 09:08:00', NOW()),
('袁梦洁', '13900000806', '5', '12', NULL, CAST('["2","4"]' AS JSON), NULL, NULL, 1, '河北省', '保定市', 0, NULL, NULL, NULL, '2026-09-05 12:25:00', NOW()),
('蒋浩然', '13900000807', '2', '4', NULL, CAST('["1","2"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '山西省', '太原市', 0, NULL, NULL, NULL, '2026-09-06 15:42:00', NOW()),
('余安妮', '13900000808', '2', '5', NULL, CAST('["3","4"]' AS JSON), NULL, NULL, 1, '山东省', '济南市', 0, NULL, NULL, NULL, '2026-09-07 18:59:00', NOW()),
('杜晓峰', '13900000809', '2', '7', NULL, CAST('["1"]' AS JSON), NULL, NULL, 1, '山东省', '青岛市', 0, NULL, NULL, NULL, '2026-09-07 09:16:00', NOW()),
('蔡雨桐', '13900000810', '2', '4', NULL, CAST('["2"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '河南省', '郑州市', 0, NULL, NULL, NULL, '2026-09-08 12:33:00', NOW()),
('叶文昊', '13900000811', '1', '2', NULL, CAST('["3"]' AS JSON), NULL, NULL, 1, '辽宁省', '沈阳市', 0, NULL, NULL, NULL, '2026-09-09 15:50:00', NOW()),
('程佳琪', '13900000812', '1', '3', NULL, CAST('["4"]' AS JSON), NULL, NULL, 1, '吉林省', '长春市', 0, NULL, NULL, NULL, '2026-09-10 18:07:00', NOW()),
('汪子墨', '13900000813', '1', '1', NULL, CAST('["1","3"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '黑龙江省', '哈尔滨市', 0, NULL, NULL, NULL, '2026-09-11 09:24:00', NOW()),
('田欣雨', '13900000814', '1', '2', NULL, CAST('["2","4"]' AS JSON), NULL, NULL, 1, '江苏省', '南京市', 0, NULL, NULL, NULL, '2026-09-12 12:41:00', NOW()),
('石俊熙', '13900000815', '3', '8', NULL, CAST('["1","2"]' AS JSON), NULL, NULL, 1, '浙江省', '杭州市', 1, NULL, 9, '2026-09-17 00:00:00', '2026-09-13 15:58:00', NOW()),
('卢诗涵', '13900000816', '3', '9', NULL, CAST('["3","4"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '安徽省', '合肥市', 1, NULL, 9, '2026-09-18 00:00:00', '2026-09-13 18:15:00', NOW()),
('贾明轩', '13900000817', '3', '8', NULL, CAST('["1"]' AS JSON), NULL, NULL, 1, '江西省', '南昌市', 1, NULL, 9, '2026-09-19 00:00:00', '2026-09-14 09:32:00', NOW()),
('邱子涵', '13900000818', '4', '11', NULL, CAST('["2"]' AS JSON), NULL, NULL, 1, '湖北省', '武汉市', 1, NULL, 9, '2026-09-20 00:00:00', '2026-09-15 12:49:00', NOW()),
('方雨泽', '13900000819', '4', '10', NULL, CAST('["3"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '湖南省', '长沙市', 1, NULL, 9, '2026-09-21 00:00:00', '2026-09-16 15:06:00', NOW()),
('侯嘉怡', '13900000820', '4', '11', NULL, CAST('["4"]' AS JSON), NULL, NULL, 1, '四川省', '成都市', 1, NULL, 9, '2026-09-22 00:00:00', '2026-09-17 18:23:00', NOW()),
('邹浩宇', '13900000821', '6', '15', NULL, CAST('["1","3"]' AS JSON), NULL, NULL, 1, '陕西省', '西安市', 1, NULL, 9, '2026-09-23 00:00:00', '2026-09-18 09:40:00', NOW()),
('熊思彤', '13900000822', '6', '16', NULL, CAST('["2","4"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '甘肃省', '兰州市', 1, NULL, 9, '2026-09-24 00:00:00', '2026-09-19 12:57:00', NOW()),
('孟子轩', '13900000823', '6', '17', NULL, CAST('["1","2"]' AS JSON), NULL, NULL, 1, '内蒙古自治区', '呼和浩特市', 1, NULL, 9, '2026-09-25 00:00:00', '2026-09-19 15:14:00', NOW()),
('秦婉清', '13900000824', '7', '21', NULL, CAST('["3","4"]' AS JSON), NULL, NULL, 1, '广西壮族自治区', '南宁市', 1, NULL, 9, '2026-09-26 00:00:00', '2026-09-20 18:31:00', NOW()),
('白嘉豪', '13900000825', '7', '19', NULL, CAST('["1"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '云南省', '昆明市', 1, NULL, 9, '2026-09-27 00:00:00', '2026-09-21 09:48:00', NOW()),
('江若彤', '13900000826', '7', '20', NULL, CAST('["2"]' AS JSON), NULL, NULL, 1, '贵州省', '贵阳市', 1, NULL, 9, '2026-09-03 00:00:00', '2026-09-22 12:05:00', NOW()),
('阎子豪', '13900000827', '8', '22', NULL, CAST('["3"]' AS JSON), NULL, NULL, 1, NULL, NULL, 2, '本学期名额已满', 9, '2026-09-04 00:00:00', '2026-09-23 15:22:00', NOW()),
('费心怡', '13900000828', '8', '23', NULL, CAST('["4"]' AS JSON), CAST('["1","5"]' AS JSON), NULL, 2, '海外', NULL, 2, '联系不上本人', 9, '2026-09-05 00:00:00', '2026-09-23 18:39:00', NOW()),
('温浩轩', '13900000829', '5', '6', NULL, CAST('["1","3"]' AS JSON), NULL, NULL, 1, '天津市', '天津市', 2, '专业方向与本社当前项目不太匹配', 9, '2026-09-06 00:00:00', '2026-09-25 09:56:00', NOW()),
('霍思琪', '13900000830', '5', '12', NULL, CAST('["2","4"]' AS JSON), NULL, NULL, 1, '天津市', '天津市', 2, '报名信息填写不完整', 9, '2026-09-07 00:00:00', '2026-09-25 12:13:00', NOW());

-- 造完之后可以这样自查：
--   SELECT COUNT(*) AS demo_members FROM user WHERE phone LIKE '139000007%';   -- 期望 28
--   SELECT COUNT(*) AS demo_applies FROM recruit_apply WHERE phone LIKE '139000008%'; -- 期望 30
