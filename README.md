<div align="center">

# OSC 社团管理系统

**天津中德开源鸿蒙社（OSC）· V1.0 纳新上线版**

社团的**数字档案馆** —— 飞书管「当下聊天」，官网管「对外展示」，本系统管「历史资产与流程状态」。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-409eff)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

> ⚠️ **项目已归档 / 搁置（2026-09-30）**：本系统作用下降，已停止活跃开发，短期内不再更新。代码与文档保留供参考；若未来重启，清理脚本（server/sql/90_~92_）与历史文档版本仍可用。

---


## 一、项目简介

本项目是一套面向高校社团的**一体化管理系统**，覆盖「纳新报名 → 审核建号 → 成员档案 → 公告 / 导入导出 / 数据看板」的完整链路。

它**不是**官网，也**不是** IM：官网负责「把成果讲出去」，飞书负责「喊人开会」，本系统负责「把成果存下来并有据可查」——回答飞书回答不了的问题（谁借了资产、报名卡在哪一步、三年前的成员去哪查）。

**V1.0 范围**：14 个 Must 切片 = 12 个核心功能 + 数据导出（F-013）+ 轻量反馈入口（F-014）。
明确**不做**：即时通讯、资产台账、AI 能力、社团官网。

**典型使用场景**

| 场景 | 谁在用 | 做什么 |
| :--- | :--- | :--- |
| 纳新期（公开场景） | 新生（免登录） | 扫码填报名表 → 查询审核进度 → 反馈意见 |
| 纳新期（内部场景） | 部长 / 社长团 | 审核报名、批量通过并导出一次性密码清单、发短信通知 |
| 常态运营（内部场景） | 全体成员 | 查公告、看成员展板、维护个人资料与头像 |
| 学期归档（内部场景） | 社长团 / 超管 | Excel 批量导入成员、按条件导出名册、看数据看板 |

---

## 二、功能特性

需求编号与 PRD 的 F-001 ~ F-014 卡片一一对应，「主要角色」一列为后端实际权限边界。

| 编号 | 功能 | 说明 | 主要角色 |
| :--- | :--- | :--- | :--- |
| F-001 | 公开报名页 | 移动优先 H5：一句话介绍 + 报名表单 + 隐私勾选 + 结果态；手机号唯一防重复，被拒可覆盖重提 | 公开（免登录） |
| F-002 | 手机号 + 密码登录 | 算术图形验证码（一次性作废）、连续失败锁定、首次登录强制改密 | 全体成员 |
| F-003 | 审核管理台 | 列表 / 筛选 / 统计、通过建号（随机初始密码 + 回填 `user_id`）、拒绝留痕、批量通过、部长仅见本部门 | 部长及以上 |
| F-004 | 短信通知提效工具 | 按模板生成短信文案并一键复制 / 唤起短信 App（不做自动发送） | 部长及以上 |
| F-005 | 审核状态查询页 | 免登录查进度与拒绝原因；已拒绝者给「重新报名」出口 | 公开（免登录） |
| F-006 | 成员档案管理 | 行范围（部长 = 本部门）+ 列范围裁剪（普通成员仅见基础列，手机号 / 学号后端置空） | 全员（按权限裁剪） |
| F-007 | 个人中心 | 资料维护 + MinIO 头像上传（姓名 / 手机号不可自助修改） | 全员 |
| F-008 | 公告系统 | 富文本（前后端**双重 XSS 白名单清洗**）+ 置顶 + 配图 + 未读红点 / 一键已读 | 发布：部长及以上；阅读：全员 |
| F-009 | Excel 批量导入 | 模板下载 + 文件头魔数校验 + 逐行校验 + 建号 + 错误行清单 | 社长团 / 超管 |
| F-010 | 基础看板 | 成员现状 + 招新复盘双口径；ECharts 图表 + 本地矢量中国地图（含港澳台与九段线） | 全员 |
| F-011 | 基础字典管理 | 学院 / 专业 / 部门 / 职位 / 标签等启停维护（**只启停不删除**） | 超管 |
| F-012 | 首个超管初始化 | 首次部署引导创建超管（本人设密码，不触发强制改密） | 一次性公开 |
| F-013 | 数据导出 | 成员名册按筛选导出 + 报名数据全量导出（含留痕）；中文文件名按 RFC 5987 | 社长团 / 超管 |
| F-014 | 轻量反馈入口 | 免登录提交（一次性验证码）+ 后台列表 + 标记已处理 | 提交：公开；查看：社长团 / 超管 |

---

## 三、技术栈

### 后端（`server/`）

| 类别 | 技术 | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| 语言 | Java | 17 | — |
| 框架 | Spring Boot | 3.3.13 | Web + Validation |
| ORM | MyBatis-Plus | 3.5.7 | 逻辑删除 `is_deleted`，驼峰映射 |
| 鉴权 | Sa-Token | 1.44.0 | Header 模式（`osc-token`），token 存 Redis |
| 数据库 | MySQL | 8.x | 库 `osc`；应用账号 `osc_app` 无 DDL 权限 |
| 缓存 | Redis | 6.x / 3.x | 登录 token、图形验证码、失败计数 |
| 对象存储 | MinIO | 8.5.12 | 头像、公告配图（未配置时降级，不影响启动） |
| 富文本清洗 | jsoup | 1.21.2 | 公告 XSS 白名单清洗（入库 + 出参双端） |
| Excel | FastExcel | 1.3.0 | Excel 导入 / 导出（EasyExcel 官方续作，包名 `cn.idev.excel`） |
| 验证码 | easy-captcha + nashorn-core | 1.6.2 / 15.7 | 算术图形验证码（JDK 17 已移除 Nashorn，需另补） |
| 密码 | spring-security-crypto | — | 仅用 BCrypt 编码器，不引入鉴权链 |
| 接口文档 | Knife4j | 4.5.0 | **仅 dev 开启**，prod 由过滤器彻底拦住 |

### 前端（`web/`）

| 类别 | 技术 | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| 框架 | Vue | 3.5.43 | Composition API + `<script setup>` |
| 构建 | Vite | 8.3.0 | 组件 / API 按需自动引入（unplugin） |
| 桌面组件库 | Element Plus | 2.14.6 | 内部场景（后台式页面） |
| 移动组件库 | Vant | 4.10.2 | 公开场景（纳新 H5）+ 移动端布局 |
| 状态管理 | Pinia | 4.0.3 | user / dict / config / locale / recruit |
| 路由 | Vue Router | 5.3.1 | 路由表即菜单来源，守卫与菜单共用能力声明 |
| 请求 | axios | 1.20.0 | 统一封装，**拦截器已脱壳 `body.data`** |
| 图表 | ECharts | 6.1.0 | 按需引入 + 本地矢量中国地图（无 key、断网可用） |
| 富文本 | wangEditor-next | 6.4.2 | 公告编辑器 |
| XSS 兜底 | DOMPurify | 3.4.15 | 与后端 jsoup 构成双重清洗 |

### 基础设施

MySQL 8 · Redis · MinIO · Nginx（生产同源反代）；生产环境支持**公网 HTTPS** 与**现场局域网**两套部署形态。

---

## 四、系统架构

```
        浏览器（移动优先 H5 / 桌面后台，同一套产物）
                          │
                    HTTPS / HTTP
                          ▼
        ┌─────────────────────────────────────┐
        │  Nginx：托管 web/dist（SPA 回退）    │
        │          反代 /api/ → 后端（剥前缀） │
        └─────────────────────────────────────┘
                          │
                          ▼
        ┌─────────────────────────────────────┐
        │  Spring Boot（osc-server）          │
        │  ├─ Sa-Token 鉴权（Header: osc-token）│
        │  ├─ Controller → Service → Mapper   │
        │  └─ 统一返回 { code, message, data } │
        └─────────────────────────────────────┘
              │            │             │
              ▼            ▼             ▼
          MySQL 8       Redis         MinIO
        （业务数据）  （token/验证码）（头像/配图）
```

**分层与外壳**

- 页面地图**两层正交**：**场景层**（公开 / 内部，决定外壳）+ **权限层**（决定功能显隐）。
  - 公开场景 → `PublicLayout`（不得出现登录后元素）
  - 内部场景 → `AppShell`（桌面左侧菜单 / 移动端底部 Tab + 「更多」抽屉）
- 「管理端 / 成员端」不是两套界面：`/admin/*` 只是路径命名空间，内部页面**共用一套外壳**。
- **谁能看什么只有一处声明** —— 路由 `meta.menu.capability`，菜单可见性与守卫判定共用同一份。

**目录结构**

```
New-Management-System/
├─ server/                         后端（Maven 单体）
│  ├─ sql/                         建库建表与种子脚本（00 ~ 04，幂等）
│  └─ src/main/
│     ├─ java/com/tsguosc/
│     │  ├─ common/                统一返回体、错误码、常量、全局异常
│     │  ├─ config/                配置类（Sa-Token、MinIO、prod 加固等）
│     │  ├─ controller/            接口层（公开 / 内部）
│     │  ├─ service/ + impl/       业务层
│     │  ├─ mapper/ + entity/      持久层（MyBatis-Plus）
│     │  ├─ dto/                   请求 / 响应对象
│     │  └─ util/                  图片校验、MinIO、清洗、导出等通用件
│     └─ resources/                application.yml + dev / prod profile
├─ web/                            前端（Vue 3 + Vite）
│  └─ src/
│     ├─ api/                      接口封装（axios 统一实例）
│     ├─ router/                   路由表 + 守卫 + 菜单生成
│     ├─ stores/                   Pinia（user / dict / config / locale / recruit）
│     ├─ layouts/                  PublicLayout（公开）+ AppShell（内部）
│     ├─ views/public             公开场景页面
│     ├─ views/auth               登录 / 引导 / 改密
│     ├─ views/member、views/admin 内部场景页面（目录名仅为命名空间）
│     ├─ components/               公告铃铛、富文本编辑器、图表等
│     └─ utils/                    清洗、下载、图表、时区等
└─ docs/                           四份长期文档（需求 / 台账 / 战略 / 交接）
```

---

## 五、角色与权限模型

**不做角色枚举** —— 角色一律由三个字段推导（后端 `StpInterfaceImpl` 与前端 `constants/roles.js` 同口径）：

| 角色 | 判定条件 | 权限范围 |
| :--- | :--- | :--- |
| 超管 `super-admin` | `role = 2` | 全部功能 + 字典管理 + 纳新设置 |
| 社长团 `leader-group` | `department = 0` | 全社管理：审核、导入、导出、反馈 |
| 部长 `minister` | `duty = 2` | 本部门管理：审核本部门报名、成员档案（行范围 = 本部门） |
| 成员 `member` | 其余 | 个人资料、公告、成员展板（基础列）、看板、反馈 |

> 前端只控制「看得见 / 点得动」，**真正的权限判定一律以后端为准**。

---

## 六、快速开始

### 0. 环境要求

| 依赖 | 版本 | 必需 |
| :--- | :--- | :--- |
| JDK | 17 | ✅ |
| Maven | 3.6+ | ✅ |
| Node.js | ≥ 22 | ✅ |
| MySQL | 8.x | ✅ |
| Redis | 3.x / 6.x | ✅ |
| MinIO | 任意近期版本 | ⬜ 可选（未配置时上传功能降级，其余正常） |

### 1. 初始化数据库（root 执行，按顺序）

```bash
# 建库（00）→ 建表（01，无 DROP）→ 字典种子（02，幂等）→ 增量补列（04，幂等）
mysql --user=root --password=root --default-character-set=utf8mb4 --execute="source server/sql/00_init_database.sql"
mysql --user=root --password=root --default-character-set=utf8mb4 --execute="source server/sql/01_schema.sql"
mysql --user=root --password=root --default-character-set=utf8mb4 --execute="source server/sql/02_seed_dict.sql"
mysql --user=root --password=root --default-character-set=utf8mb4 --execute="source server/sql/04_alter.sql"
```

> `03_migrate_old_data.sql` 是从旧系统迁移历史数据用的，全新部署**跳过**。
> 建库脚本会一并创建应用账号 `osc_app`（仅 `osc.*` 的 DML 权限，无 DDL）。

### 2. 配置环境变量

```bash
cp server/.env.example server/.env    # 填入真实值，.env 已被 .gitignore 忽略
```

至少填写 `MYSQL_PASSWORD`；`.env` 不存在时应用靠默认值兜底（详见第七节）。

### 3. 启动后端（**必须在 `server/` 目录下执行**，`.env` 走相对路径）

```bash
cd server
mvn package -DskipTests
java -jar target/osc-server-1.0.0.jar          # dev，默认 8080
```

验证：`curl http://127.0.0.1:8080/health` → `{"code":200,...}`

> 首次启动后访问前端根路径，会引导创建**首个超管**（F-012，一次性）。

### 4. 启动前端

```bash
cd web
npm install
npm run dev                                     # http://127.0.0.1:5173
```

开发服务器会把 `/api` 代理到 `http://127.0.0.1:8080` 并**剥掉 `/api` 前缀**（与线上 Nginx 行为一致）。

其他脚本：`npm run lint`、`npm run format`、`npm run build`（产物 `web/dist/`）。

---

## 七、环境变量

后端配置项一律走环境变量（`server/.env` 或系统环境变量），明文密码**不入仓**。完整样例见 [`server/.env.example`](server/.env.example)。

| 分组 | 键 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| 应用 | `APP_PORT` | dev 8080 / prod 8081 | 服务端口 |
| 应用 | `SPRING_PROFILES_ACTIVE` | `dev` | `dev` / `prod` |
| MySQL | `MYSQL_HOST` / `MYSQL_PORT` | `127.0.0.1` / `3306` | — |
| MySQL | `MYSQL_DB` / `MYSQL_USER` / `MYSQL_PASSWORD` | `osc` / `osc_app` / 空 | 应用账号不带 DDL 权限 |
| Redis | `REDIS_HOST` / `REDIS_PORT` / `REDIS_DB` | `127.0.0.1` / `6379` / `0` | — |
| Redis | `REDIS_PASSWORD` | 空 | **线上必须设置** |
| 鉴权 | `SA_TOKEN_NAME` | `osc-token` | 请求头名称 |
| 鉴权 | `SA_TOKEN_TIMEOUT` | `2592000` | token 有效期（秒），30 天 |
| MinIO | `MINIO_ENDPOINT` | `http://127.0.0.1:9000` | — |
| MinIO | `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 空 | **留空即降级**：应用照常启动，仅上传不可用 |
| MinIO | `MINIO_BUCKET` | `osc` | 启动时自动创建并设为公开读 |
| MinIO | `MINIO_PUBLIC_URL` | 空 | 对外访问前缀；留空回退 `endpoint/bucket`。库里只存对象 key，**换域名不失效** |
| 安全 | `OSC_LOGIN_MAX_FAIL` | `5` | 连续登录失败锁定阈值 |
| 安全 | `OSC_LOGIN_LOCK_MINUTES` | `15` | 锁定时长（分钟） |
| 安全 | `OSC_CAPTCHA_TTL` | `120` | 图形验证码有效期（秒），校验后立即作废 |
| 安全 | `OSC_PASSWORD_MIN_LENGTH` / `MAX_LENGTH` | `8` / `20` | 密码强度（须同时含字母与数字） |

---

## 八、生产部署

两套方案，均已在《交接文档》§八 写好完整步骤与配置样例：

| 方案 | 适用 | 要点 |
| :--- | :--- | :--- |
| **A. 公网 + 域名 + HTTPS**（主路径） | 云主机 / 学校服务器（Linux，2C4G 起） | Nginx 同源反代 `/api/` → `127.0.0.1:8081/`（**末尾 `/` 负责剥前缀**）、托管 `dist` + SPA 回退、certbot 签证书 |
| **B. 现场局域网**（兜底） | 纳新摊位：笔记本跑服务、新生手机连同一 WiFi | 同一份 Nginx 配置改监听端口；⚠️ 四个必查坑：**手机热点带机量仅 8~15 台**、Windows 防火墙放行、内网 IP 变动（设静态 IP）、**校园网客户端隔离** |

生产补充动作：

```bash
# 1) 用 prod profile 启动（在 server/ 目录下）
java -jar osc-server-1.0.0.jar --spring.profiles.active=prod

# 2) 数据库备份（纳新期建议每天至少一次；备份文件与库分盘存放）
mysqldump --single-transaction --routines --triggers osc > /backup/osc_$(date +%Y%m%d).sql
```

> prod 已关闭接口文档与错误详情：`knife4j.enable=false` 之外，另有 prod 专属过滤器拦掉 `/doc.html` 与 `/webjars/**`（仅关配置项并不够）。
> 上线前请照《交接文档》§八 8.9 检查表逐条打勾，并按 8.10 真做一次**恢复演练**（"建过备份"和"能恢复"是两回事）。

---

## 九、项目文档

长期文档固定四份，**单一出处**：同一事实只写在一处，别处只放指针。

| 文档 | 作用 |
| :--- | :--- |
| [产品需求文档（PRD）V1.0](<docs/OSC 社团管理系统 · 产品需求文档（PRD）V1.0.md>) | **权威需求依据**，按功能卡片 F-001 ~ F-014 查阅 |
| [开发任务点清单](<docs/OSC 社团管理系统 · 开发任务点清单.md>) | **工作台账**：§3 总表进度 / §4 任务点详情（方案·实现·验证）/ §6 决策记录（按 D 编号索引） |
| [战略定位与价值延伸说明](<docs/OSC 社团管理系统 · 战略定位与价值延伸说明.md>) | 定位与价值规划（答辩 / 软著 / 汇报） |
| [新对话交接提示词（通用版）](<docs/OSC 社团管理系统 · 新对话交接提示词（通用版）.md>) | 开工总纲：项目全貌、当前阶段、环境与坑、铁律、部署与运维 |

> 需求有冲突时，以 PRD 为准；PRD 未覆盖时以《开发任务点清单》§6 的决策记录为准。

---

## 十、开发约定（摘要）

- **统一返回体** `{ code, message, data }`，且 **HTTP 状态码一律 200** —— 业务结果靠 `code` 表达，前端拦截器已脱壳 `data`。
- **错误码**：`40000` 参数 / `40001` 验证码 / `40002` 登录 / `40003` 锁定 / `40004` 冻结 / `40005` 需改密 / `40100` 未登录 / `40300` 无权限 / `40400` 不存在 / `50000` 服务异常。
- **「不是错误、只是引导」** 的分支（如重复报名、已是成员）一律 `200` + `data.nextAction`，前端就地渲染提示与按钮。
- **两阶段数据模型**：报名写 `recruit_apply`（全量），**审核通过才建 `user`**；审核状态只在报名表维护。
- **逻辑删除**：`user` 用 `status=1` 冻结代替删除，`sys_dict` 用 `enabled=0` 停用代替删除。
- **字典**只启停不删除；学号空串统一归一为 `NULL`（唯一索引只给 NULL 一个名额）。
- **不新增按任务点命名的文档**：方案 / 实现 / 验证写回《开发任务点清单》对应条目。

---

## 十一、版本与路线图

| 阶段 | 状态 |
| :--- | :--- |
| V1.0 开发（T1 ~ T19） | ✅ 已完成：后端骨架 / 数据库与字典 / 前端工程 / 登录认证 / 报名 / 审核 / 查询 / 短信工具 / 成员档案 / 个人中心 / 公告 / Excel 导入 / 数据导出 / 看板 / 公开端链路 / 内部导航与工作台 / 反馈入口 / 全链路联调 |
| V1.0 上线前三项收尾 | 🔄 进行中：① 清理测试夹具账号 ② 建立备份计划任务 + 恢复演练 ③ 报名页补充 Logo |
| V1.0 上线 | ⬜ 待部署（见第八节） |

**已知待办（不阻塞上线）**：活动照片轮播、对象存储孤儿文件清理、飞书免登、公告真推送、整站 i18n、仓库内自动化回归测试。

---

## 十二、许可证

[Apache License 2.0](LICENSE)

<div align="center">
<sub>天津中德开源鸿蒙社 · OSC 社团管理系统</sub>
</div>
