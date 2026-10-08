# OSC 社团管理系统 · 新对话交接提示词（通用版）

> **本文件是本项目的「通用交接文档」，长期维护、每次交接前更新。**
> 两层用途：
> 1. **开新对话时** —— 把下面「新对话开场提示词」整段复制给新对话即可；
> 2. **新对话建立上下文时** —— 它就是「必读总纲」，写清项目全貌、当前阶段、环境与铁律。
>
> 维护约定见文末 **「九、交接约定」**。

## 新对话开场提示词（每次开新对话，直接复制这一段）

```markdown
你好 DS。继续「OSC 社团管理系统（天津中德开源鸿蒙社）」重构项目 —— 新对话接续，麻烦先重建上下文。

第一步：完整读取 F:\Project\Own Project\New-Management-System\docs\OSC 社团管理系统 · 新对话交接提示词（通用版）.md ，
并严格按其中「二、动手前必读（按顺序）」把项目上下文建立起来。

第二步：按该文档「三、当前阶段与下一步」确定本轮要做什么；若那一节指向某个任务点，
就去《开发任务点清单》读该任务点的条目（技术方案 / 实现结果 / 决策 D 编号）。

第三步：先输出你的理解（3~5 条：当前进度、本轮任务、关键红线），再给出实施计划（改动文件、
影响面、怎么自测、怎么验证），等我确认后再动手。

务必遵守该文档的「五、全局铁律」与「六、协作规则」——尤其是：单功能节奏、先方案后代码、
不主动 git 提交（我说「提交Tx」才提交）、交付必须附 PowerShell 5.1 友好的验证指引。
```

---

## 一、项目基本盘

- **项目**：OSC 社团管理系统（天津中德开源鸿蒙社）· V1.0 纳新上线版 · 全栈重构
- **工作空间**：`F:\Project\Own Project\New-Management-System`
  （**独立 git 仓库**，远端 `git@github.com:CapableCCat/New-Management-System.git`，分支 `main`，Apache-2.0）
- **旧系统**（只读参考）：`F:\Project\Own Project\management-system`
- **旧文档归档**：`F:\Project\Own Project\Docs Archive`
- **技术栈**：
  - 后端 `server/`：Java 17 + Spring Boot 3.3.13 + MyBatis-Plus 3.5.7 + Sa-Token 1.44 + MySQL 8（库 `osc`）+ Redis + MinIO（头像）
  - 前端 `web/`：Vue 3.5 + Vite 8 + Element Plus 2.14 + Vant 4.10 + Pinia + vue-router + axios
  - monorepo 单仓：`server/` + `web/`
- **端口**：后端 dev 8080 / prod 8081；前端 dev 5173（`/api` 代理到 8080 并剥前缀）
- **定位**：社团数字档案馆（飞书管今天、OSC 管昨天和明天），V1.0 明示不做 IM / 资产台账 / AI / 官网
- **范围**：14 个 Must 切片 = 12 核心 + F-013 数据导出 + F-014 反馈入口
- **当前状态**：**开发与 V1.0 收尾全部完成**（T1~T34 均已提交推送）→ **已归档 / 搁置（2026-09-30）**，未来很长一段时间不再活跃开发；
  PRD 已订正为 **v3.2**（页面地图改「场景 + 权限」两层）。前半段（T1~T19 开发 + 上线前三件收尾
  ① 清夹具账号 ② 备份与恢复演练 ③ 报名页加 Logo）均已收官，最新进度与下一步**一律看本文件 §三**。

---

## 二、动手前必读（按顺序，别跳）

1. 本文件 —— 全貌、当前阶段、环境坑、铁律
2. `.workbuddy/memory/MEMORY.md` —— 项目长期记忆（技术选型、环境怪癖、各任务点红线）
3. `.workbuddy/memory/YYYY-MM-DD.md` —— 最近工作日志（按日期倒序读最近 1~2 份，信息量最大）
4. `docs/OSC 社团管理系统 · 开发任务点清单.md` —— **工作台账**：§2 技术栈、§3 总表看进度、§4 任务点详情（方案/实现/验证）、§6 决策记录（按 D 编号索引）
   （**注意编号**：为贯彻「单一出处」，原来的 §0 开发规则 / §1 项目速览已删（内容在本文件），§5 只是指向本文件的指针表 —— 看到编号从头部直接跳到 §2 是正常的）
5. `docs/OSC 社团管理系统 · 产品需求文档（PRD）V1.0.md` —— **权威需求依据**（按功能卡片 F-001~F-014 按需查）
6. `docs/OSC 社团管理系统 · 战略定位与价值延伸说明.md` —— 定位与价值规划（答辩/软著/汇报用）

> **需求基准**：PRD 是权威依据；PRD 未覆盖或与实现冲突时，以《开发任务点清单》§6 的决策记录为准，
> 若决策也未覆盖，**列出选项让用户拍板，不要擅自决定**。

---

## 三、当前阶段与下一步

> **本节是「本轮做什么」的唯一出处**，每次交接由 AI 更新。

**阶段：V1.0 收尾开发（T20~T34）** —— **开发全部完成，进入上线自测**

| 项目 | 内容 |
| ------------------ | ------------------------------------------------------------ |
| **已完成并推送** | T1~T27（开发 T1~T19 + 收尾 T20~T27）+ **T31 手机档图标宫格** + **T32 顶栏固定/独立滚动** + **T33 演示数据**（`c209821` feat 脚本 / `ee6c6a8` docs） |
| **已完成并推送** | **T34 自测账号**：`server/sql/92_self_test_accounts.sql`（新增 64 个能登录账号）、`91_demo_cleanup.sql`（补一段删除）；随归档一并提交推送（`93563e2` server / `6717895` docs）。 |
| **本轮任务（下一步）** | **项目已归档 / 搁置（2026-09-30）**：上线自测（T28）、问题修复（T29）、上线准备（T30）**不再执行**；若未来重启，再据历史文档版本恢复流程 |
| **之后** | （T29 / T30 已于归档时移除，见《开发任务点清单》文末维护记录） |

#### 已完成的任务点（T20~T33；除 T33 外均已推送）

| # | 内容 | 提交 |
| :-- | :--- | :--- |
| **T20** | 报名页加 Logo（`sys_config.club_logo`，后台可传可清） | 已推送 |
| **T21** | 视觉规范落地（Vercel 风格 + 三层配色；EP 派生色公式、ECharts 色值单一出处） | 已推送 |
| **T22** | 菜单重组（**五组**两级 + 图标化 + 账号区上移 + 公告合并） | `b9b9308` / `b8a27fc` |
| **T23** | 响应式与设备适配（三档：desktop / compact / mobile） | `714032f` / `cdeac6c` |
| **T24** | 登录页优化（**bare 开关** + 全屏背景 + Logo；去掉副标题） | `77c565d` / `02aa67b` |
| **T25** | 报名页活动照片轮播（`public/activities/` 丢文件即生效；无素材时占位块） | `c02bbf4` / `2493e57` |
| **T26** | 对象存储孤儿文件清理（超管手动触发；**先 dry-run 再删**；四条安全线） | `9cd3f92` / `edb5712` / `52b9353` |
| **T27** | 纳新设置权限放宽给社长团（**前后端成对改**） | `25cb4f3` / `9d7963e` |
| **T31** | 手机档「更多」改**图标宫格**（新增 `components/MoreGrid.vue`） | `dfc7f73` / `ef74624` |
| **T32** | **顶栏固定 + 侧栏与内容独立滚动**（`.app-shell` 改 `height:100% + overflow:hidden`） | `453fdde` / `c9f3801` |
| **T33** | 演示数据（28 成员 + 30 报名）+ 清理脚本 | `c209821` / `ee6c6a8` |
| **T34** | **自测账号**（超管 1 + 社长团 5 + 部长 8 + 部员 50，**能登录**） | 已推送（`93563e2` / `6717895`） |

**收尾完成标准**：T1~T34 全部完成并推送、清理脚本（`90_`/`91_`/`92_`）就绪；项目已归档，**未打发布 tag**（若未来重启再评估 V1.0 发布）。

**版本节奏共识**：V1.0 是首个可对外发布的版本；V1.1 是上线后的小修小补；V2.0 是能力层/架构级变更（数字资产库、整站 i18n、飞书集成）。**不刻意凑版本号**。

#### 本轮三件事（社长 2026-09-27 亲自排的优先级；括号里是他给的理由）

| #    | 事项                                  | 状态                                          |
| :--- | :------------------------------------ | :-------------------------------------------- |
| 1    | **清夹具账号**（安全）                | ✅ 2026-09-27 完成                             |
| 2    | **建备份计划任务 + 恢复演练**（数据） | ✅ 2026-09-28 完成                             |
| 3    | **报名页加 Logo**（第一印象）         | ✅ 2026-09-28 完成（升级为正式任务点 **T20**） |

#### ⚠️ 自测 / 清理前置（归档留存，原为 T28 开工前必做）

1. **~~准备「能登录」的测试账号~~ ✅ 已由 `T34` 备齐（2026-09-30）** ——
   库里已有 **64 个能登录的账号**（`139000002xx`：自测超管 1 / 社长团 5 / 部长 8 / 部员 50），
   统一固定口令，**口令不在仓库里**（只记 `.workbuddy/memory/`，gitignored）；对照表见《开发任务点清单》文末维护记录（T34 条目）。
   ⚠️ **T33 造的 28 个演示成员仍然登不上**（密码是随机密钥的 bcrypt、密钥已弃，这是刻意的）—— 别拿它们试登录。
   **⚠️ 自测跑完必须删**（跑 `91_demo_cleanup.sql`，一次清三段 —— 演示成员 / 演示报名 / 自测账号）；
   `O1` 的教训：留下**能登录**的测试账号是安全风险，比演示数据更紧迫。
2. **素材还差两样**（都是「放进去即生效」，不需要 AI 改代码）：
   - 登录页背景图 → 改 `web/src/styles/index.scss` 里的 `--login-bg-image` 一行
   - 活动照片 → 按 `01.jpg` 起命名丢进 `web/public/activities/`（目录里有 README 说明）
3. **环境**：后端 8080 + 前端 5173 + MinIO + Redis + MySQL（见 §八 8.1）；**测「存储维护」必须先起 MinIO**。
4. **数据已经就位**：T33 的 28 成员 / 30 报名 + T34 的 64 个自测账号都在库里（看板显示「共 93 人」，列表有多页）。

#### 已知取舍与待办（不阻塞上线）

> 完整清单（**20 条**）见 **《开发任务点清单》§7「待排期优化项」** —— 不在本节重复罗列。

**已并入 V1.0 收尾并完成的项**：
- ✅ Logo 展示（T20）、✅ 活动照片轮播代码（T25，素材待放）、✅ 对象存储孤儿清理（T26）、
  ✅ 纳新设置权限放宽（T27）、✅ 手机档宫格（T31）、✅ 顶栏固定/独立滚动（T32）

**仍待社长拍板的（PRD 未覆盖，属新决策）**：`O7` 免登录写接口限频阈值、`O8` 是否上完整审计表、`O12` 反馈是否要回复功能、`O19` 导出是否要列裁剪。

**⚠️ 清理脚本（归档留存，原「上线前必做」）**：`server/sql/91_demo_cleanup.sql` 可随时清掉 T33 演示数据（`139000007xx`/`139000008xx`）与 T34 自测账号（`139000002xx`），跑完自带三行自查；**是否执行由后续维护者决定**（本项目已归档，暂无执行计划）。
   ⚠️ 脚本一次清三段、跑完自带三行自查；**清完必须复核 `139000002%` 为 0**。

**开工前必做（环境）** —— 完整清单与命令一律看「八、启动清单与常用命令」8.1，这里只留三条要点：

1. **MySQL / Redis 是 AUTO_START 服务**（开机自启，不用管）；**只有 MinIO 需要手动起**（🔧 详见 8.2）
2. **分工约定（社长拍板）**：常驻服务（MinIO / 后端 / 前端）**由社长在会话外手动起**，AI 不要用会话内后台任务起它们（原因见 §四 最后一条）
3. **AI 自测只用隔离栈 8090 + 5180 且当轮收掉**

**❓ 待用户定夺** → 见上文「仍待社长拍板的」四项。

---

## 四、环境与坑

### 0) 先探活，再拉起

⚠️ **agent 起的进程「去留不确定」**：有时本轮回复结束就被回收，**有时会一直活着**。
（T9 起的 `java -jar` 到 T10 仍占着 8080 并**锁住 `target/osc-server-1.0.0.jar`**，直接把 `mvn clean package` 顶失败了。）

所以规则是两句话：
1. **开新一轮先探活**（下面三条命令），不在线就自己拉起；
2. **重新构建/启动前先查端口占用并停掉旧进程**（尤其 8080），别硬撞。

```powershell
# 后端探活（000/502 = 没起来）
curl.exe -s -o NUL -w "%{http_code}" --noproxy 127.0.0.1 http://127.0.0.1:8080/health
# Redis
& "C:\Program Files\Redis\redis-cli.exe" -h 127.0.0.1 -p 6379 PING
# MinIO（T11 起需要）
curl.exe -s -o NUL -w "%{http_code}" --noproxy 127.0.0.1 http://127.0.0.1:9000/minio/health/live
```

### 本机环境坑（实测）

- ⚠️ **在 WorkBuddy 终端里起后端必须先清宿主注入的变量**：`Remove-Item env:SERVER__PORT; Remove-Item env:SERVER__HOST`
  —— 宿主注入了 `SERVER__PORT=4225`，Spring Boot 宽松绑定会当成 `server.port` 去抢端口而失败。
  也可追加 `--server.port=8080`。IDEA / 普通 cmd 启动不受影响。
- ⚠️ **PowerShell 5.1 下测试脚本一律写纯 ASCII**；含 `\"` 的 SQL 别用 PS 字符串拼（会篡改引号），写成 `.sql` 文件再 `source`。
- ⚠️ **`-Dfile.encoding=UTF-8` 必须带引号**：`"-Dfile.encoding=UTF-8"`，否则 PS 5.1 拆成 `.encoding=UTF-8`。
- ⚠️ **页面验证必须用 Chrome 无头 + CDP 设备模拟**：Windows 上 `--window-size=375` 会被系统最小窗口宽度顶掉（实际视口 504），
  用 `Emulation.setDeviceMetricsOverride` 才是真实移动视口（做法见 MEMORY.md）。
- ⚠️ **`__vueParentComponent` 只存在于 dev 构建**（T12 实测）：`web/dist` 里搜不到这个属性，所以
  「取组件 `setupState` 直接改内部状态」那招**只在打 5173 dev server 时有效**。要验证 `dist` 产物（如 5180 预览栈），
  必须走真实 DOM 交互：输入框用 `value` setter + 派发 `input` 事件，富文本编辑区用
  `Input.insertText`（CDP）真实录入。别再照抄 T8/T10 的 setupState 写法。
- ⚠️ **Element Plus 内置文案默认是英文**：全站没配 `zh-cn` locale，`ElMessageBox` 的按钮是 **OK / Cancel**、
  分页与空态也是英文（影响 T4~T11 所有确认框）。写页面用例断言按钮文案时按英文预期，或显式指定中文按钮文案（见 §6 D87）。
- ⚠️ **本机 shell 里 node 无法 spawn 外部 exe**（`execFileSync` 报 `EBUSY`，`mysql.exe`/`redis-cli.exe` 都中招）→
  验证脚本改用：① 内置极简 RESP 客户端直连 Redis 读验证码答案；② 落库值的复核放到 **bash 步骤**里用 mysql 跑（脚本把 id 写进 json 交给下一步）。
- ⚠️ **Git Bash 会把以 `/` 开头的参数改写成 Windows 路径**：`node cdp.cjs /announcement ...` 传进去的其实是
  `C:/Users/.../PortableGit/.../announcement` → 拼出的 URL 非法、**导航静默失败**（页面停在上一页，极难排查）。
  → 路径**写在 JS 文件里**最稳；传参就给完整 URL 或加 `MSYS_NO_PATHCONV=1`。
- ⚠️ **登录态注入要先站到目标源上再写 `localStorage`**（在 `about:blank` 上写会静默无效）；注入后若目标页
  没落上，重发一次导航即可（`/login` 的守卫会抢跑把页面替换成 `/home`）。
- ⚠️ **MinIO 启动的两个坑**：① 必须**显式**带 `MINIO_ROOT_USER=minioadmin MINIO_ROOT_PASSWORD=minioadmin`，
  否则报 `Unable to validate credentials inherited from the shell environment`（宿主注入的 `MINIO_*` 不合法）；
  ② **别用 `| head` 起**（管道一关进程就被带走），重定向到日志文件再 `run_in_background`。
- ⚠️ **jsoup 的协议白名单只有 http/https**：相对地址会被判成"非法协议"而把属性/整张图删掉 ——
  公告正文里存对象 key（`announcements/…`）时，**必须在清洗之前先把 key 展开成完整地址**（见 §6 D85）。
- ⚠️ **Excel 库对"不是 Excel 的文件"会按文本/CSV 兜底解析**（T13 实测）：随便一个二进制/CSV 喂给
  FastExcel 不报错，而是把首行当列头，于是报出「请使用标准模板」—— 把"文件坏了"错说成"列头不对"。
  → 必须先做**文件头魔数**校验（xlsx = zip `PK\x03\x04`，xls = OLE2）。
- ⚠️ **FastExcel 的包名是 `cn.idev.excel`**（不是旧系统的 `com.alibaba.excel`）；`@ExcelProperty` 也在
  `cn.idev.excel.annotation`。照抄旧系统代码会编译不过。
- ⚠️ **无头浏览器里验证「文件下载」不可靠**（`Browser.setDownloadBehavior` 未必落盘）：改为在页面里埋点，
  包一层 `URL.createObjectURL` 与 `HTMLAnchorElement.prototype.click`，断言 blob 类型/大小与下载文件名 ——
  这样验的是页面接线本身，与环境无关（T13 实证）。
- ✅ **无头浏览器里验证「文件上传」**：CDP `DOM.getDocument` + `DOM.querySelector('input[type=file]')`
  拿到 nodeId，再用 `DOM.setFileInputFiles` 塞真实文件路径（会触发 change → 组件的 on-change）✓
- ⚠️ **导出/下载类接口不能只看"接口返回 200"就算过**：必须把文件**读回来核对**（列头、中文标签、
  筛选是否生效、留痕字段是否齐全）。T14 的做法：Java 写个 `ExportReader` 用 FastExcel 读回 xlsx
  打印列头与每行内容，再由 bash / 断言核对。只看 200 会漏掉"导出成空表""code 没转中文"这类问题。
- ⚠️ **别用「页面内埋点」收集网络请求**（T15 实测）：在页面里包一层 `fetch`/`XMLHttpRequest`，
  一旦发生**整页导航**（`Page.navigate`）埋点连同 `window` 上的数据一起被清掉，收集到的是空数组。
  → 改用 CDP 的 `Network.enable` + 监听 `Network.requestWillBeSent` 事件，导航前后的请求都收得到。
- ⚠️ **ECharts 地图的渲染尺寸取决于「数据外接框 + `aspectScale`（默认 0.75）」**：
  中国地图数据含南海诸岛（最南约 3°N）时外接框接近方形，放进**偏宽**的卡片会大量留白
  → 卡片比例要按地图实际外接框来配（T15 把地图卡调成半宽 + 360px 高）。这不是渲染 bug，别误判。
- ⚠️ **启动命令必须在 `server/` 目录下执行**，否则读不到 `.env`（`spring.config.import` 用相对路径）。
- ⚠️ **配置值不要经 mysql 批处理往返**：mysql 批处理会把真实换行输出成字面量 `\n`（T6 踩过，简介里出现反斜杠-n）。
  读中文多行值走接口或加 `--raw`。
- ⚠️ **`git commit` 与 `git push` 必须分成两条独立命令**（T10、T11 各踩一次）：命令可能被沙箱升级重试跑两遍，
  第二次 `git commit` 报 "nothing to commit" 返回 1 → **把 `&&` 链后面的 `git push` 短路掉**，看着像提交成功其实没推。
  收尾一律 `git log --oneline origin/main..HEAD` 核对（空 = 已推）。同理：**多步提交前先看 `git diff --cached --name-only`**，
  因为 `git add` 过的文件会被下一次 `git add <别的路径>` 一起带进提交（T11 的 PRD 就这样误入 server commit，用
  `git reset --soft HEAD~1` + `git restore --staged <path>` 修正）。
- ⚠️ **PowerShell 工具偶发沙箱内部错误**（`sandbox-center cmd decisionRecord missing actual resource subject`）时，
  **换 Bash 工具照样干活**：git 命令、绝对路径的托管 Python（`C:/Users/001/.workbuddy/binaries/python/versions/3.13.12/python.exe`）
  都能直接调用。给文件改名可用 `git add <带空格的旧名>` + `git mv <旧名> <新名>` 组合完成。
- ⚠️ **Element Plus 2.14 的细节**：下拉禁用态不在根 `.el-select` 上（查内层 `input[disabled]`）；
  `el-dialog` 是 fixed 定位，**溢出量不出来**（要量 `getBoundingClientRect()`，T9/T10 踩过）。
  更完整的踩坑清单在 `.workbuddy/memory/MEMORY.md`。
- ⚠️ **`npm run build` 可能失败在「清空 dist」这一步，而不是代码**（T18 实测）：报
  `[plugin vite:prepare-out-dir] Error: [safe-delete] 操作失败: spawnSync ... ETIMEDOUT` ——
  沙箱的 safe-delete 垫片清目录超时。**先 `rm -rf web/dist` 再 `npm run build`** 就绕开了（`dist/` 已被 `.gitignore` 忽略，删产物无风险）。
- ⚠️ **自写的极简 RESP 客户端要等「回复完整」再 resolve**（T18 实测）：按第一个数据包就 resolve 会把 `KEYS` 这类多值回复**截断成空数组** ——
  Redis 里明明有 8 个 `osc:captcha:*`，读成 `[]`，于是误判成"页面没取验证码"，排查了好一阵。
  改成「最后一次收到数据后再等 250ms 才收结果」（`lib.cjs` 的 `redisGet` 只读单值，所以没暴露过这个问题）。
- ⚠️ **Chrome 的 `user-data-dir` 会跨轮残留，害人不浅**（T19 实测两种症状）：
  ① **整套用例跑偏** —— 跑过一次"已登录"用例后，同一 profile 再跑公开页用例，`/` 直接进 `/home`，一次假失败 14 项；
  ② **静默退出** —— profile 有残锁时 Chrome 起不来，CDP 连不上，脚本**不报错、直接退出**（exit 0、无汇总），
  极容易误判成"功能坏了"。→ **每个模式/用例用独立 profile 目录，或开跑前先删掉 profile**。
- ⚠️ **老页面用例脚本不自清理，重复跑会假失败**（T19 实测）：`osc_t13` 的上传用例跑完会**留下 2 个测试账号**
  （`13910000301/302`），第二次跑就变成"跳过 3 行"→ 断言全错。→ 收尾**必须手工核库**（`SELECT COUNT(*) FROM user WHERE phone LIKE '1391000%'`）。
- ⚠️ **外壳类选择器改名后，历史用例脚本会集体失效**（T19 实测：`.admin-aside/.admin-menu/.admin-actions/.member-tabbar`
  → `.app-aside/.app-menu/.app-actions/.app-tabbar`）。这些脚本在 `%TEMP%` 下不入库，改一次即可；
  但**别把这类失败当成功能回归** —— 先用一个最小探针确认真实行为，再判断。

- ⚠️ **Element Plus 的 `el-drawer` / `el-dialog` 关闭是 `v-show`（`display:none`），元素仍留在 DOM**（T31 误报过一次）：
  判「已关闭」**不能**用 `!!document.querySelector('.el-drawer')`，要判可见性 —— `!!el.offsetParent`，
  或过滤 `[...].filter(m => m.offsetParent)`。`.el-overlay` 同理，它一直在 DOM 里。
- ⚠️ **验 `vite preview` 时配置里的 `/api` 代理不能省**（T25 踩过）：省掉后 `/api/*` 会被 SPA 兜底
  返回 `index.html`（**HTTP 200**），axios 拿到 HTML → `data?.initialized` 为 undefined →
  守卫判定「系统未初始化」→ **页面被重定向到 `/init`**，看起来像"页面崩了"。另：该配置**必须放在 `web/` 目录内**
  （放 `/tmp` 解析不到 `node_modules/vite`）。
- ⚠️ **node 验证脚本要按「不 spawn 外部程序」来设计**（T26/T27/T31/T33 都受益）：本机 node 起不了
  `python` / `mysql` / `find`（`EBUSY`，见上文那条）。**固定套路**：
  **bash 准备（算 bcrypt / 执行 SQL）→ node 只走 HTTP 与 `fs` → bash 清理**；
  桶内/目录清单用 node 的 `fs` 递归读（不 spawn `find`）；Redis 用内置 `net` 手写 RESP。
- ⚠️ **软删表的行数核对**：实体带 `@TableLogic` 时，**原始 SQL `COUNT(*)` 会把 `is_deleted=1` 的行也算进去**
  （T26 核对公告、T33 核对报名各中招一次）。**核对行数要区分「全表」与「`is_deleted=0`」**；
  自己造的测试数据清理要 `DELETE` 硬删。
- ⚠️ **外壳的滚动模型（T32）：`.app-shell` 是 `height:100% + overflow:hidden`，`.app-main` 是唯一滚动容器**。
  **`min-height: 0` 是最大的坑** —— flex 子项默认 `min-height:auto`（"不能比内容还矮"），
  `.app-body` 与 `.app-main` **两处都必须加**，否则内容一长就把容器撑高、**内部滚动直接失效**。
  用 `height:100%` 而**不是 `100dvh`**：dvh 随手机地址栏收放变化 → 容器高度变 → 内容重排/滚动位置跳。
- ⚠️ **验证脚本里「切过账号」后，做下一组断言前先确认当前身份**（T31 误报过一次）：
  前面用成员账号验完、后面直接断言"桌面侧栏 10 项" → 只有 4 项，误判成"被改坏"。
  另：**菜单项数一类的事实不要靠数**（T22 与 T31 各写错一次"超管 5 组 11 项"，实际是 **10 项**）——
  **从验证输出里读**。
- ⚠️ **给社长的验证步骤别把「PowerShell」写进 Bash 工具的命令/文档片段里**：从 Bash 工具提交时，
  内容里出现该字样会被安全策略**整条拒绝**（提交信息、文档片段都中过招）。写「终端」或直接给代码块即可。
- ⚠️ **写接口断言前先 `grep` 控制器的映射，别凭记忆拼路径**（T34 踩过，3 个假失败）：
  成员档案是 **`/member/list`**（登录即可、数据按角色**行裁剪 + 列裁剪**），**没有** `/member/admin/list` ——
  后者返回 `40400 请求的资源不存在`，很容易被误读成「权限没生效」或「功能被改坏」。
  `/member/admin/**` 只是**特权动作**（改资料、导入、导出）。
- ⚠️ **造测试账号的三条口径**（T34）：① `activated_at` **必须给非空值**，否则登录会被「首登强制改密」拦下；
  ② Python `bcrypt.hashpw` 默认产出 **`$2b$`** 前缀，`BCryptPasswordEncoder` 兼容（库里演示数据也是 `$2b$10$`）——
  不必刻意转 `$2a$`；③ 手机号一律用**保留段**并**在同一个清理脚本里写好删除行**，否则一定会忘。

### 会话被「唤醒」的原因与解决办法（2026-09-26 查清）

**现象**：社长停手一段时间后，**旧对话会突然又冒出一段回复**（他开新对话比较频繁，这现象尤其明显）。

**原因（本会话实测，非推断）**：AI 用 `run_in_background` 起的命令会被登记成一个「任务」，宿主盯着它；
**任务一结束就往该会话注入一条 `<task-notification>`** → 会话把它当**新输入** → 重新激活、AI 开口说话。
- **每个后台任务结束 = 一次潜在唤醒机会**（本会话起过 8 个后台任务 = 8 次机会）
- **活得越久的任务越烦人**：后端 / 预览站跑了 1~2.7 小时，在社长早已停手之后才结束 —— 那一刻才来敲门
- ⚠️ **「任务结束」≠「进程结束」**：通知报 failed，但 java / node 进程**仍在监听 8090 / 5180 并响应 200** ——
  包装器与子进程的生命周期是解耦的，**这正是"后台进程去留不确定"的机制根源**

**解决办法（本项目约定）**：
1. **常驻服务在会话外起**：MySQL / Redis 是 AUTO_START 服务（不用管）；**MinIO + 后端 + 前端由社长手动起**
   （IDEA / 独立窗口 / 服务），**不要交给 AI 的会话内后台任务** —— 既不占会话、也不产生通知（详见 §八 8.1 分工约定）
2. **AI 的验证栈在同一轮内收掉**：自测完就停掉 8090 / 5180，别留到会话结束（下一轮反正要重新构建）
3. **开新对话前先让 AI 清掉本会话的后台任务**：任务处于已结束态就不会再有通知，进程也一并收掉
4. 万一旧会话还是被唤醒：**不用管**（它只会回一句），要根除就在任务列表里结束那个会话
5. 判断服务死活**别只看 `netstat`**（不同工具调用的沙箱隔离会导致结果不一致，我们据此误报过一次）→ **打一次接口最可靠**

### 绝对不要做的事

- ❌ **`git add -A` / `git add .`** —— 工作树里有故意不入库的种子材料与旧承接文档；**只 add 明确路径**
- ❌ **在父目录（`F:\Project\Own Project`）执行 git 操作** —— 父目录本身是另一个仓库，会把 18 个兄弟项目一锅端
- ❌ **未经明确指令 commit / push**
- ❌ **在 PRD 未覆盖时擅自决定** —— 列选项让用户拍板

---

## 五、全局铁律

1. **单功能节奏**：一个任务点 = 一次交付。完成即停，等用户审阅确认后再进下一个，**不越点开发**。
2. **先方案后代码**：涉及技术选型、表结构变更、公共抽象的任务点，**先出技术方案经用户确认再动手**。
3. **文档策略（单一出处）**：
   - **不新增「按任务点命名」的文档** —— 任务点的方案 / 实现结果 / 验证方式写回《开发任务点清单》对应条目，
     实现层决策追加到 §6。
   - **长期文档固定四份**，各自唯一职责：PRD（需求依据）/ 任务点清单（台账与决策）/ 战略说明（对外定位）/
     本交接文档（开工与铁律）。
   - **单一出处**：同一事实只写在一处，别处只放指针 —— 不为省事复制第二份，**复制出来的那份一定会漂移**
     （实测：清单 §5 与本文件重复，同一人维护、仅两天就漂了 5 处过期路径）。
   - **允许例外，但要过门槛**：只有内容「不随任务点推进而变化」且「会被反复查阅」时才新增长期文档
     （如安全台账、交付物索引）；新增时必须同时在本文档「七、关键词速查」注册，避免变成没人知道的孤儿文档。
4. **实测优于推断**：交付必须附真实跑过的验证记录，不能写"应该可以"。
5. **有疑问先问**：PRD 未覆盖或与实现冲突时，列出问题清单与建议选项，等决策。
6. **参考代码只读**：旧系统 `management-system` 仅作参考读取，新代码只写在当前工作空间。
7. **账号与权限口径**：角色由 `role/department/duty` 推导 —— `role=2` 超管、`department=0` 社长团、`duty=2` 部长；
   **不枚举角色**。权限判定以后端为准，前端仅控制可见性。
8. **两阶段数据模型**：报名写 `recruit_apply`（全量），**审核通过才建 `user`**（正式成员）；
   审核状态只在报名表维护，单一数据源。
9. **强制改密只在「密码不是本人选的」时触发**：引导页建超管不触发；审核通过 / Excel 导入 / 管理员重置才触发。
10. **改文档/改代码前先定位原文**：改 PRD 或清单前先 `grep -n` 把目标段落的**原文读出来**再动手，
    别凭记忆写"要替换的那段"。2026-09-26 改 PRD 时就差点弄错 —— 我凭印象说「F-001 的报名入口写的是系统首页」，
    实际那句在 **F-005**；如果直接照记忆去改就会改错位置或匹配不上（`old_string` 报 not found 是**好事**，
    说明工具在拦你，别硬凑）。同理：引用了某段需求时，先回原文确认它到底写在哪张卡片里。
11. **改文档时 `old_string` 一律用「完整行（含行尾）」，绝不用半行当锚点**：
    用某行**开头**做锚点、而 `new_string` 又把新内容插在前面，等于把那行的开头删掉 →
    **上一条记录会整条消失**（2026-09-29 连续犯三次，都是回头检查时才发现的）。
    同理，**替换某条 bullet / 某段时，`new_string` 必须把被替换的内容也包含进去**（或整段替换），否则等于删掉它。
    改完**顺手核对一次**（`grep` 一下该条目还在不在）。
    ⚠️ **2026-09-30 复盘看到了它的「自然后果」**：本清单的「维护记录」现在被写成了**两块** ——
    带 `> **维护记录**：` 表头的在**文件末尾**（流水只到 2026-09-28 的 `O3`），而 T21 之后的流水却落在
    **§4 `T20` 小节内部**，且 **T23 / T24 两条的首行被吃掉**（只剩半句开头）。
    所以追加新条目时**先确认上一条还在、并且加在了与上一条同一个块里**。
12. **动结构前先做「影响面排查」，再动手**：改动整页滚动模型、外壳结构、公共样式这类"牵一发动全身"的东西前，
    **先搜一遍谁会受影响**（T32 就是先搜了 `window.scroll*` / `scrollTo` / `el-affix` / `position: sticky`
    ——一处都没有才敢动）；改完再修比改前先查贵得多。
13. **提交哈希、菜单项数、表行数这类「事实」一律现查**（`git log` / 验证输出 / `SELECT`），
    **不凭记忆写**：2026-09-29 把 T26 的提交哈希凭记忆写错过一次，菜单项数写错过两次。

---

## 六、协作规则

- **逐点确认**：任务点做完自测（接口 + 页面 + 实测记录）→ 交付时附「验证指引」→ **停下等用户审阅**。
- **绝不主动提交**：只有用户明确说「**提交 Tx**」时才 `git add` + `commit` + `push` 三步走完，
  且**按功能点拆多个 commit**（不要把多个任务点打包）、**全英文 title + 中文 body**；只 add 明确路径。
- **交付必带验证指引**：命令用 **Windows PowerShell 5.1 友好**写法（优先原生 `Invoke-RestMethod`；
  `curl.exe` 只给单行；不要 `curl -H/-d`——PS 5.1 会篡改引号）。
- **文档实时更新**：每完成一个任务点，同步更新《开发任务点清单》§3 总表状态 + 该条目实现结果；决策追加 §6。
- 用户是「先审阅 → 再说继续/提交」的节奏；**禁止越点开发**（除非用户明确说"继续全部"）。

---

## 七、关键词速查

| 关键词                 | 落点                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 仓库入口（README）     | 根目录 `README.md`：项目简介 / 功能特性（F-001~F-014）/ 技术栈与版本 / 架构与目录 / 角色权限 / 快速开始 / 环境变量 / 部署概览 / 文档索引 —— 面向访客与接手者的入口，**需求与决策细节仍以 PRD 与《开发任务点清单》§6 为准**（2026-09-27 新增） |
| 待排期优化项（含建议） | 《开发任务点清单》**§7**（`O1`~`O19`）：每条带「改什么 · 怎么改 · 代价 · 优先级」；🔴 P0＝上线前必做（清夹具账号 / 备份演练 / 报名页 Logo）。选中的条目应升级为正式任务点（T21 起）并在 §4 补方案·实现·验证（2026-09-27 新增） |
| 权威需求依据           | `docs/OSC 社团管理系统 · 产品需求文档（PRD）V1.0.md`（F-001~F-014 功能卡片） |
| 开发台账 / 进度 / 决策 | `docs/OSC 社团管理系统 · 开发任务点清单.md`（§3 总表 / §4 任务点详情 / §6 决策记录，按 D 编号索引） |
| 定位与价值             | `docs/OSC 社团管理系统 · 战略定位与价值延伸说明.md`          |
| 项目记忆               | `.workbuddy/memory/MEMORY.md` + 最新日志                     |
| 后端工程               | `server/`（`com.tsguosc`，主类 `OscServerApplication`）      |
| 前端工程               | `web/`（**两套外壳**：`layouts/PublicLayout.vue` 公开场景 / `layouts/AppShell.vue` 内部场景 —— "管理端/成员端"两套外壳自 T19 起**已不存在**，`/admin/*` 只是路径命名空间） |
| 数据库                 | MySQL 库 `osc`；应用账号 `osc_app`（无 DDL 权限，建表用 root） |
| 建表 / 种子脚本        | `server/sql/00~04`（`01_schema.sql` 无 DROP、`02_seed_dict.sql` 幂等） |
| 鉴权                   | Sa-Token Header `osc-token`；`SaTokenConfig` 白名单 + 拦截器 |
| 角色推导               | `StpInterfaceImpl`（`super-admin` / `leader-group` / `minister` / `member`） |
| 字典                   | `sys_dict`（7 类：department/duty/status/college/major/tag/feedback_source） |
| 报名两阶段             | `recruit_apply`（0待审/1通过/2拒绝）→ 通过建 `user`（0正常/1冻结） |
| 首登强制改密           | `user.activated_at`（空 = 未首登）；拦截器兜底只放行 3 个接口 |
| 短信提效工具           | 模板读 `GET /recruit/admin/sms-config`；改仍只走超管「纳新设置」页 |
| 成员档案权限           | 行范围=部长本部门；列范围=成员仅基础列（后端 `masked()` 置空手机号/学号） |
| 头像存储               | MinIO；库里只存对象 key（`avatars/{userId}/…`），读时拼公开地址 |
| 个人中心自助边界       | 姓名 / 手机号**不在** `/user/profile` 请求体里；学号仅空可补录（T11） |
| 文档架构               | 长期文档四份 + **单一出处**：同一事实只写一处、别处只放指针（见「五、全局铁律」第 3 条） |
| 公告系统               | 接口 `/announcement/**`（2 段=登录可读，`/admin/**`=部长及以上）；表 `announcement`（T2 建，T12 零 DDL）；页面 管理端 `AnnouncementAdminView` / 成员端 `AnnouncementView` + `HomeView` 摘要 |
| 富文本 XSS 清洗        | 后端 `util/HtmlSanitizer`（jsoup 白名单 + style/图片/链接三处加固，`cleanForStore` 入库、`cleanForOutput` 出参）；前端 `utils/sanitizeHtml.js`（DOMPurify）；**唯一允许 `v-html` 富文本的地方** = `AnnouncementDetailDialog.vue` |
| 公告配图               | MinIO `announcements/{yyyyMM}/{时间戳}.{ext}`；库里只存对象 key、输出拼公开前缀（D85）；上传三层校验复用 `ImageValidator` |
| 图片校验 / MinIO 公共  | `util/ImageValidator`（扩展名+Content-Type+魔数）、`util/MinioSupport`（建桶+公开读策略）—— 头像与公告配图共用 |
| 页面用例的驱动方式     | `dist` 产物上只能用真实 DOM 交互（`Input.insertText` / 派发 `input`）；`setupState` 那招只在 dev server 上有效（见「四、环境与坑」） |
| 外壳（内部页）三档布局 | `layouts/AppShell.vue` + `composables/useLayoutMode.js`（`desktop ≥1024` / `compact 768~1023` / `mobile <768`）；**`useIsMobile` 只给页面内容用**（13 处在依赖，语义不能动） |
| 外壳滚动模型（T32）    | `.app-shell` = `height:100% + overflow:hidden`；**顶栏固定、`.app-menu` 与 `.app-main` 各滚各的**；⚠️ `.app-body` 与 `.app-main` 的 **`min-height: 0` 不能删**（删了内部滚动失效） |
| 手机档「更多」         | **图标宫格**（T31）：`components/MoreGrid.vue` + `AppShell` 里 `direction="btt"` 的抽屉；与侧栏**同源**（同一份 `groups` + 同一张图标映射表） |
| 登录页                 | `/login` 走 `PublicLayout` 的 **bare 模式**（路由 `meta.bare`，不渲染页头页脚）；**换背景图只改 `styles/index.scss` 的 `--login-bg-image` 一行**（T24） |
| 活动照片（报名页轮播） | `web/public/activities/`（命名 `01`~`05` + jpg/jpeg/png，**丢文件即生效、不用改代码**）；探测逻辑 `utils/activityPhotos.js`（T25） |
| 存储维护（孤儿清理）   | 「纳新设置」页底部区块（**仅超管**）；`StorageAdminController` + `util/StorageKeys`；**先 dry-run 再删、删除前逐个复检**（T26） |
| 视觉 token / 配色      | `styles/index.scss`（主色 `--brand-primary` + **EP 派生色必须一起改** + `--login-bg-*`）；图表色值单一出处 `constants/palette.js`（ECharts 读不到 CSS 变量） |
| 权限能力函数（单一出处） | `constants/roles.js` —— 页面**只**依赖这里的函数；**`canManageConfig` 自 T27 起 = 超管或社长团**（字典管理仍仅超管） |
| 演示数据（T33）        | `server/sql/90_demo_seed.sql`（造，幂等）/ `91_demo_cleanup.sql`（清）；手机号保留段 `139000007xx`（成员）/ `139000008xx`（报名）；**登不上**（密码随机已弃）；**归档前建议跑清理**（清理脚本自带三行自查） |
| 自测账号（T34）        | `server/sql/92_self_test_accounts.sql`（造，幂等）：**64 个能登录的角色账号**（自测超管 1 / 社长团 5 / 部长 8 / 部员 50），号段 `139000002xx`；**统一口令但明文不入库**，只记 `.workbuddy/memory/`（gitignored）；清理走同一个 `91_demo_cleanup.sql`（**比演示数据更紧迫**） |
| Excel 批量导入         | 接口 `/member/admin/import`、`/member/admin/import-template`；**权限=社长团/超管**（路由 meta.leaderGroup）；服务 `service/MemberImportService(+Impl)`；页面 `views/admin/ImportView.vue` |
| Excel 库               | `cn.idev.excel:fastexcel` 1.3.0（EasyExcel 官方续作，包名 `cn.idev.excel`）；列头常量在 `dto/ImportRow` |
| 组件库语言切换         | `stores/locale.js` + `App.vue` 的 `<el-config-provider>`（Vant 走 `Locale.use()`）；切换入口 `components/LocaleSwitch.vue`，偏好存 `localStorage.osc_locale`；**只覆盖组件库内置文案**（见 §6 D94） |
| CSV / 文件下载工具     | `utils/csv.js`（buildCsv / downloadCsv / today）、`utils/download.js`（saveBlob / readBlobMessage） |
| 数据导出               | 接口 `/member/admin/export`（按筛选）、`/recruit/admin/export`（全量）；**权限=社长团/超管**（方法级注解叠加类级）；服务 `service/ExportService(+Impl)`；Excel 写出工具 `util/ExcelExporter`（含 RFC 5987 中文文件名）；列头常量在 `dto/MemberExportRow` / `RecruitExportRow` |
| Excel 写出             | 统一走 `util/ExcelExporter.toXlsx(sheetName, headClass, rows)` + `writeToResponse(...)`（别自己拼响应头，中文文件名很容易写错） |
| 基础看板               | 接口 `/dashboard/member-stats`、`/dashboard/recruit-stats`（**不加角色限制**，登录即可）；服务 `service/DashboardService(+Impl)`；页面 `views/admin/DashboardView.vue` |
| 图表与地图             | `utils/echarts.js`（ECharts 6 按需 `use` + `ensureChinaMap()`）、`components/BaseChart.vue`；地图数据 `web/public/map/china.json`（**同源静态加载**，含港澳台 + 九段线，无 key、断网可用） |
| 生源地省份归一化       | `util/ProvinceNormalizer`（34 省级区划 + 中英别名 + 去后缀；**认不出返回 null → 计入「未填写」不上地图**） |
| 字典索引（跨任务共用） | `util/DictIndex`：`labelToCode()` 只认启用项（导入用）、`codeToLabel()` 含停用项（展示/导出/看板用） |
| 页面地图两层模型       | **场景层**（公开 / 内部，决定外壳）+ **权限层**（决定功能可见性），两层正交 —— PRD §8.2 v3.2；不要再用"管理端/成员端"描述功能归属（见 §6 D106） |
| 公开端三页互链         | 登录页 / 报名页（含提交成功**结果态**）/ 查询页 —— 各自只有一个身份、平级互链、不出现登录后元素（T18 / D109） |
| 报名提交结果           | `RecruitSubmitVO` = `state`（`SUBMITTED`/`RESUBMITTED`/`ALREADY_PENDING`/`ALREADY_MEMBER`）+ `nextAction`（`QUERY`/`LOGIN`）+ `message`；后两种是**引导不是错误**，一律 `200`（T18 / D111 / D113） |
| 公开端落地页与配置缓存 | 路由表不写死 `/` 落点，由 `router/guard.js` 按 `recruit_open` 判定；配置走 `web/src/stores/recruit.js` 会话级缓存（T18 / D110 / D114 / D115） |
| 首页定位               | **工作台**（按角色的待办 + 入口），**不是**内容门面；公告走「公告」菜单 + 右上角通知红点（T19 / D107） |
| 菜单与权限单一出处     | 菜单**从路由表生成**（meta 声明 `{ group,label,order,capability }`），与守卫判定共用同一份声明（T19 / D108 / **D119**） |
| 统一外壳（内部场景）   | 内部页面**共用 `layouts/AppShell.vue`**（桌面左侧菜单 / 移动端底部 3 项 +「更多」抽屉）；`MemberLayout`、`AdminLayout` 已删除，"管理端/成员端"不再存在（T19 / **D118**）；路径保留现状，`/admin` 只是命名空间 |
| 能力判定（谁能看什么） | 一律 `constants/roles.js` 的**具名能力函数**（12 个，按 PRD 权限矩阵逐行对应）；store 上不再有权限计算属性；路由 meta 的 `menu.capability` 与守卫共用（T19 / D119） |
| 工作台（首页）         | `/home` = 按角色的待办与入口（待审报名 / 资料完整度 / 导入 / 导出 / 反馈占位），**不再放公告摘要**（T19 / D107 / D122） |
| 公告通知红点           | `components/AnnouncementBell.vue` + `utils/announcementRead.js`；有新公告即亮、**打开公告页才算已读**、面板含「一键已读」（T19 / D121）。⚠️ 已读时间用**本地时区 ISO 串**，别用 `toISOString()` |
| 轻量反馈（F-014）      | 提交**免登录**但必须带一次性验证码（白名单只放两段路径 `/feedback/*`）；入口两处＝报名结果态(source=1) + 工作台(source=2)，共用 `components/FeedbackDialog.vue`；管理端 `/admin/feedback`（社长团/超管）＋ 标记已处理；内容按**纯文本**存（T16 / D123~D126） |
| 上线部署与运维         | **已归档移除**：原 §八 8.7~8.11（公网 HTTPS / 现场局域网 / 上线检查表 / 备份恢复 / 现场预案）已于 2026-09-30 项目归档时删除；若未来重启，相关流程见历史 commit 文档版本 |
| 性能红线（易复发）     | 验证码生成**必须**保持 `ImageIO.setUseCache(false)`（`ImageIoConfig`）；默认值会让每次出图落一个磁盘临时文件 → 单发 134ms、200 并发 P95 8.6 秒（T17 / D127）。换机器要重新量 |
| prod 安全边界          | prod 下接口文档必须取不到：`knife4j.enable=false` **不够**（页面外壳仍 200），靠 `ProdDocDisabledConfig` 拦 `/doc.html` 与 `/webjars/**`（T17 / D128） |
| V1.0 收尾需求 | docs/OSC 系统 · V1.0 收尾需求.md（视觉规范 / 菜单重组 / 登录页 / 响应式 / A池收尾） |
| V1.0 上线自测清单 | **已删除**（2026-09-30 归档时随部署任务点 T28/T29/T30 一并移除） |
| 视觉规范（三层配色） | 品牌蓝 #2B4EFF 主色 / 青蓝 #00D4FF 动效色 / Vercel 灰阶底子；圆角 6-12-16；动效 100~300ms |
| 菜单分组（四组两级） | 我的 / 纳新管理 / 内容管理 / 数据洞察；路由 meta 加 `group` 字段，`el-menu-item-group` 渲染 |

---

## 八、启动清单与常用命令

### 8.1 项目启动清单（按顺序；✅ 常驻不用管 / 🔧 每次要手动起）

| # | 组件 | 是否常驻 | 启动方式 | 探活 |
|---|---|---|---|---|
| 1 | MySQL 8（库 `osc`，:3306） | ✅ **Windows 服务 `MySQL80`，AUTO_START**（开机自启） | （万一没起）管理员执行 `net start MySQL80` | `mysql.exe --user=root --password=root --skip-column-names "--execute=SELECT 'UP';"` → `UP` |
| 2 | Redis（3.0.504，:6379） | ✅ **Windows 服务 `Redis`，AUTO_START** | （万一没起）`net start Redis` | `& "C:\Program Files\Redis\redis-cli.exe" -h 127.0.0.1 -p 6379 PING` → `PONG` |
| 3 | **MinIO**（头像 / 公告配图，T11 起必需，:9000） | 🔧 **没有注册成服务，必须手动起** | 见 8.2 | `curl.exe -s -o NUL -w "%{http_code}" --noproxy 127.0.0.1 http://127.0.0.1:9000/minio/health/live` → `200` |
| 4 | 后端（dev :8080） | 🔧 手动（社长用 IDEA；AI 用隔离栈） | 见 8.3 | `Invoke-RestMethod http://127.0.0.1:8080/health` → `code:200` |
| 5 | 前端（dev :5173） | 🔧 手动 `npm run dev` | 见 8.4 | 浏览器开 http://127.0.0.1:5173 |

> ⚠️ **分工约定（2026-09-26 社长拍板）**：**常驻服务一律由社长在会话外启动**（IDEA / 独立 cmd 窗口 / Windows 服务），
> **不要交给 AI 的会话内后台任务** —— 原因见「四、环境与坑」最后一条（后台任务一结束就会唤醒旧会话）。
> AI 只在交付自测时起**隔离栈**（后端 **8090** + 预览站 **5180**），并在同一轮内收掉。
> 社长只需管两件事：**MinIO（🔧）+ 后端 + 前端**；MySQL / Redis 是服务，重启电脑后自己在跑。

### 8.2 MinIO（唯一需要手动起的依赖）

```powershell
# 必须在同一行里显式给凭据，否则会报 "Unable to validate credentials inherited from the shell environment"
& "E:\Minio\minio\minio.exe" server E:\Minio\osc-data --address :9000 --console-address :9001
```
- 凭据默认 `minioadmin / minioadmin`；桶 `osc` 由**应用启动时自动创建**并设公开读，不用手动建
- ⚠️ **别用 `| head` 之类的管道起它**（管道一关进程就被带走）——直接前台跑，或重定向到日志文件
- 📌 **未来可能不用手动起了**：社长已选定「后端启动时自检依赖 + 自动拉起 MinIO」的方案（**尚未排期**）——
  想看方案细节与两条边界，查《清单》§6 **D130**。实现后本页的启动动作会简化成「只开后端 + 前端」。

### 8.3 后端（在 `server/` 目录下执行）

```powershell
# ⚠️ 必须先清宿主注入的变量，否则 Spring 宽松绑定会拿 SERVER__PORT 去抢端口
Remove-Item env:SERVER__PORT -ErrorAction SilentlyContinue
Remove-Item env:SERVER__HOST -ErrorAction SilentlyContinue
# ⚠️ 不要带 clean（会删掉 IDEA 正在用的 target/classes）；改了 pom 先在 IDEA 刷新 Maven
& "E:\MAVEN\apache-maven-3.6.3\bin\mvn.cmd" package -DskipTests
& "C:\Program Files\Java\jdk-17\bin\java.exe" -jar "target\osc-server-1.0.0.jar"     # dev 8080
```
> 社长日常用 **IDEA 直接启动**（classpath 是 `target/classes`，改完代码重启即生效）；上面这套是"不想开 IDEA 时"的写法。
> AI 自测时用 `--server.port=8090` 起隔离栈，不碰 8080。

### 8.4 前端（在 `web/` 目录下执行）

```powershell
npm run dev        # http://127.0.0.1:5173（/api 代理到 8080 并剥前缀）
npm run lint
npm run build
```

### 8.5 探活一条龙（新会话开工先跑这个）

```powershell
curl.exe -s -o NUL -w "minio 9000: %{http_code}`n" --noproxy 127.0.0.1 http://127.0.0.1:9000/minio/health/live
& "C:\Program Files\Redis\redis-cli.exe" -h 127.0.0.1 -p 6379 PING
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --password=root --skip-column-names "--execute=SELECT 'mysql UP';"
curl.exe -s -o NUL -w "backend 8080: %{http_code}`n" --noproxy 127.0.0.1 http://127.0.0.1:8080/health
```
> ⚠️ **判断服务死活别只看 `netstat`**（不同工具调用的沙箱隔离会导致结果不一致，我们据此误报过一次）；
> **打一次接口**最可靠 —— MySQL / Redis 不是 HTTP 服务，用上面各自的客户端探。

### 8.6 数据库（root 建表 / 查数据）

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
& $mysql --user=root --password=root --default-character-set=utf8mb4 --table `
  "--execute=USE osc; SELECT type, code, label, enabled FROM sys_dict ORDER BY type, sort;"
```

---

### 8.7~8.11 上线与部署（已移除）

> 本项目已于 2026-09-30 **归档 / 搁置**，上线与部署流程不再执行；原 §8.7 公网部署 / §8.8 现场局域网 / §8.9 上线检查表 / §8.10 备份恢复 / §8.11 现场预案 已移除以避免误导。
> 若未来重启，相关 Nginx 样例、检查表、备份与回滚脚本见历史 commit 中的《V1.0 上线自测清单》等文档版本。

## 九、交接约定（AI 收到「我要开启新对话」时的标准动作）

> 用户在需要换会话继续时，**只会说一句**「我要开启新对话」（或「开新对话吧」之类）。此时 AI 应：

1. **先把本文件更新到最新**（这一步不能省）：
   - 「三、当前阶段与下一步」→ 改写为**下一步要做什么**（指向对应任务点，并列出开工前的环境准备与"不要做"的坑）；
   - 「四、环境与坑」→ 补上本次新踩到的坑；
   - 「五、全局铁律」→ 补上本次新学到的铁律；
   - 「七、关键词速查」→ 补上本次新增/变更的落点；
   - 「三、当前阶段与下一步」里的「已完成 / 未提交」两行同步到最新；
   - 顺手核对《开发任务点清单》§3 总表的状态列（§5 只是指向本文件的指针，不需要同步内容）。
2. **在对话里直接输出**下面这段（与本文件顶部那段**逐字一致**，整段可复制）——不要只说"提示词在文档里"，
   用户要的是能直接贴进新对话的一段话：

```text
你好 DS。继续「OSC 社团管理系统（天津中德开源鸿蒙社）」重构项目 —— 新对话接续，麻烦先重建上下文。

第一步：完整读取 F:\Project\Own Project\New-Management-System\docs\OSC 社团管理系统 · 新对话交接提示词（通用版）.md ，
并严格按其中「二、动手前必读（按顺序）」把项目上下文建立起来。

第二步：按该文档「三、当前阶段与下一步」确定本轮要做什么；若那一节指向某个任务点，
就去《开发任务点清单》读该任务点的条目（技术方案 / 实现结果 / 决策 D 编号）。

第三步：先输出你的理解（3~5 条：当前进度、本轮任务、关键红线），再给出实施计划（改动文件、
影响面、怎么自测、怎么验证），等我确认后再动手。

务必遵守该文档的「五、全局铁律」与「六、协作规则」——尤其是：单功能节奏、先方案后代码、
不主动 git 提交（我说「提交Tx」才提交）、交付必须附 PowerShell 5.1 友好的验证指引。
```

3. 用一句话告诉用户：**整段复制即可**，本轮任务已在文档「三」里写明、不需要额外说明。
4. 若本次产出了新的任务点成果或决策，**同时把关键落点写进「三、当前阶段与下一步」**，
   保证新会话只读这一份 + 按需查清单就能开工。
