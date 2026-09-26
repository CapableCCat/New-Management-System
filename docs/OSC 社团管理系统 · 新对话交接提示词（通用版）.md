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
- **当前状态**：T1~T14 已提交推送；**T15 基础看板已完成、待提交**（工作树 20 项改动）；文档已按社长反馈订正为 **PRD v3.2**，
  并新增两个信息架构任务点 **T18 / T19**；执行顺序为 **提交 T15 → T18 → T19 → T16 → T17**

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

**阶段：纳新主链完成（T1~T11）→ 支撑链完成（T12~T15）→ 信息架构调整（T18、T19 新插入）→ 收尾（T16、T17）**

| 已完成并推送 | T1 后端工程 / T2 数据库与字典种子 / T3 前端工程 / T4 登录认证（含首登强制改密）/ T5 字典管理 / T6 公开报名页 / T7 审核管理台 / T8 状态查询页 / T9 短信提效工具 / T10 成员档案管理 / T11 个人中心（首次启用 MinIO）/ T12 公告系统（富文本双重 XSS 清洗）/ T13 Excel 批量导入（FastExcel + 组件库文案中英文切换）/ T14 数据导出（RFC 5987 中文文件名）/ **T15 基础看板**（双口径 + 省份归一化 + ECharts 按需 + 本地矢量中国地图含港澳台与九段线） |
| ------------ | ------------------------------------------------------------ |
| **已完成、未提交** | **T18 公开端报名链路梳理**（PRD F-001 / F-005、v3.2）：公开三页平级互链、报名结果态双出口、查询页按状态给主操作、根路径按 `recruit_open` 动态落地、`POST /recruit/apply` 的两个引导分支改 `200 + nextAction`；**附带**报名页简介由 397 字长文精简为**一句话**（并同步「纳新设置」页提示）。接口 35 项 + 页面 39 项 + 纳新设置页 6 项全过 |
| **下一步（按序）** | ① **先提交 T18**（等社长说「提交 T18」，按 server / web / docs 拆 commit）→ ② **T19 内部导航与首页工作台** → ③ **T16 反馈入口** → ④ **T17 联调上线** |

> **⚠️ 为什么 T18/T19 插在 T16 前面**：社长在 T15 之后提了两点质疑 —— ①「管理系统首页不该展示公告，公告该由官网负责，最多右上角搞个通知」；
> ②「不该分管理端/成员端，直接按后台给的权限看到自己能看的不就行了」。核对后都是真问题（详见《清单》T18/T19 条目与 §6 D106~D112），
> 而且 T16（反馈入口）**同时要动首页与报名结果态** —— 先把信息架构理顺，T16 才不会返工两次。

**本轮施工依据**：`docs/OSC 社团管理系统 · 开发任务点清单.md` 的「T19 内部导航与首页工作台」条目（含完整交付物清单）。
**需求依据**：`docs/OSC 社团管理系统 · 产品需求文档（PRD）V1.0.md` 已订正为 **v3.2**（§8.2 页面地图改「场景 + 权限」两层模型、F-001/F-005 的入口与出海口径）。

**T18（已完成、待提交，要点不在此重复）**：交付物与实测记录见《清单》§4「T18 实现结果」，实现层决策见 §6 D109~D116。

**T19 要点（内部导航与首页工作台，中等，其次做）**
1. **菜单从路由表生成**（单一出处）：meta 声明 `{ group, label, order, capability }`，布局不再手写数组 → 菜单可见性与守卫判定共用同一份声明
2. **能力判定收敛**：页面里的手写展开（如 `AuditView.isFullScope`）全收进 `constants/roles.js` 的具名能力函数
3. **消除切端**：「公告」「个人中心」在内部外壳里也列出（同一路由两处可达），干部不必再点「返回成员端」
4. **首页改「工作台」**：去掉公告摘要卡（D107 —— 摘要卡本来就不是 PRD 要求），改为欢迎卡 + **按角色的待办与入口**
5. **右上角「公告」通知红点**：`localStorage` 记"上次查看时间"，不落库不加表
6. **不做**：统一外壳（3 套 layout 合并成 1 套 AppShell）—— 架构级改动，V1.0 上线优先，已记 D112 待办

**开工前必做（环境）** —— **完整清单与命令一律看「八、启动清单与常用命令」8.1**，这里只留三条要点：
1. **MySQL / Redis 是 AUTO_START 服务**（开机自启，不用管）；**只有 MinIO 需要手动起**（🔧 详见 8.2）
2. **分工约定（社长拍板）**：常驻服务（MinIO / 后端 / 前端）**由社长在会话外手动起**，
   AI 不要用会话内后台任务起它们（原因见 §四 最后一条：后台任务结束会唤醒旧会话）；AI 自测只用隔离栈 8090 + 5180 且当轮收掉
3. 后端本轮无新依赖；重启/构建前**先在 IDEA 刷新 Maven**、`mvn package` **不带 `clean`**

**⚠️ 工作树约定（T18 之后）**：T18 的改动（server 3 项 / web 6 项 + 新增 `web/src/stores/recruit.js` / docs 2 份）**全部还在工作树里，未 commit** ——
提交时按功能拆多个 commit（建议：server 引导分支 / web 公开端三页与落地页 / docs 台账），**只 add 明确路径**，`git commit` 与 `git push` 分两条命令。

**❓ 待用户定夺**
1. 三处字典种子存疑项 —— 附件3「航空航天」是否补成"学院"、「电子商务」（三年制高职）是否保留、「德语」（仅附件2）是否保留。可在字典管理页直接改。
2. 公告配图与导入产生的对象存储文件**不做孤儿清理**（D86）—— 需回收可另开任务点。
3. 语言切换**只覆盖组件库内置文案**（D94）—— 整站 i18n 需单独立项。
4. 导出**不做列裁剪**（D96）；看板**只放管理端**（D103）；**统一外壳**留待后续（D112）。

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
| 权威需求依据           | `docs/OSC 社团管理系统 · 产品需求文档（PRD）V1.0.md`（F-001~F-014 功能卡片） |
| 开发台账 / 进度 / 决策 | `docs/OSC 社团管理系统 · 开发任务点清单.md`（§3 总表 / §4 任务点详情 / §6 决策记录，按 D 编号索引） |
| 定位与价值             | `docs/OSC 社团管理系统 · 战略定位与价值延伸说明.md`          |
| 项目记忆               | `.workbuddy/memory/MEMORY.md` + 最新日志                     |
| 后端工程               | `server/`（`com.tsguosc`，主类 `OscServerApplication`）      |
| 前端工程               | `web/`（三套布局：Public / Member / Admin）                  |
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
| 菜单与权限单一出处     | 菜单**从路由表生成**（meta 声明 `{group,label,order,capability}`），与守卫判定共用同一份声明（T19 / D108） |

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
