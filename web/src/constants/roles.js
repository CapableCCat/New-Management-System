/**
 * 角色资格推导（PRD 第五章权限矩阵 + 《开发任务点清单》§6 D3）
 *
 * 设计原则：不做角色枚举，一律由 role / department / duty 推导出来。
 *   - 超管：role = 2
 *   - 社长团（全社管理）：department = 0
 *   - 部长（本部门管理）：duty = 2
 *   - 其余为普通成员
 *
 * 注意：前端只控制"看得见/点得动"，真正的权限判定一律以后端为准。
 */

export const ROLE = {
  MEMBER: 0,
  SUPER_ADMIN: 2
}

export const DEPARTMENT = {
  LEADER_GROUP: 0,
  TECH: 1,
  OPERATION: 2,
  PUBLICITY: 3,
  SECRETARY: 4
}

export const DUTY = {
  MEMBER: 0,
  VICE_MINISTER: 1,
  MINISTER: 2,
  PRESIDENT: 3
}

export const isSuperAdmin = (user) => !!user && user.role === ROLE.SUPER_ADMIN

export const isLeaderGroup = (user) => !!user && user.department === DEPARTMENT.LEADER_GROUP

export const isMinister = (user) => !!user && user.duty === DUTY.MINISTER

/** 全社管理资格：超管 或 社长团 */
export const canManageAll = (user) => isSuperAdmin(user) || isLeaderGroup(user)

/** 部门管理资格：部长 或 全社管理 */
export const canManageDepartment = (user) => isMinister(user) || canManageAll(user)

/* ------------------------------------------------------------------
 * 具名能力（PRD 第五章权限矩阵逐行对应）—— 清单 §6 D108 / T19
 *
 * 页面**只**依赖这里的函数，不再自己写 isSuperAdminUser || canManageAllUsers 这类展开；
 * 路由 meta 的 `menu.capability` 也用它们，于是「菜单看得见」与「守卫放得进去」
 * 天然共用同一份声明，不可能再漂。
 * ------------------------------------------------------------------ */

/** 只要登录即可（工作台 / 公告 / 成员列表 / 个人中心 / 看板） */
export const isLoggedIn = (user) => !!user

/** 查看公告列表与详情（矩阵：无单独一行，登录后即有） */
export const canViewAnnouncement = isLoggedIn

/** 查看成员列表（成员展板，基础列）—— 矩阵「查看成员列表」成员起 */
export const canViewMemberRoster = isLoggedIn

/** 编辑本人资料 —— 矩阵「成员档案编辑」成员=仅本人（走个人中心） */
export const canEditOwnProfile = isLoggedIn

/** 提交反馈 —— 矩阵「反馈提交」全员（含游客，T16 会用到） */
export const canSubmitFeedback = (user) => !!user

/** 看板查看 —— 矩阵「看板查看」成员起（接口不限角色，D103） */
export const canViewDashboard = isLoggedIn

/** 评审报名 —— 矩阵「评审报名」部长（意向含本部门）/ 社长团 / 超管 */
export const canReviewRecruit = (user) => canManageDepartment(user)

/** 成员档案（管理侧）—— 行范围部长=本部门、社长团/超管=全部 */
export const canManageMemberRoster = (user) => canManageDepartment(user)

/** 发布/编辑/删除公告 —— 矩阵「发布公告」部长起 */
export const canManageAnnouncement = (user) => canManageDepartment(user)

/** Excel 导入 —— 矩阵「Excel 导入」仅社长团 / 超管 */
export const canImportMembers = (user) => canManageAll(user)

/** 数据导出 —— 矩阵「数据导出」仅社长团 / 超管 */
export const canExportData = (user) => canManageAll(user)

/** 查看/处理反馈 —— 矩阵「反馈查看」仅社长团 / 超管（T16） */
export const canViewFeedback = (user) => canManageAll(user)

/** 字典管理 —— 矩阵「字典管理」仅超管 */
export const canManageDict = (user) => isSuperAdmin(user)

/** 系统初始化 / 纳新设置 —— 矩阵「系统初始化/配置」仅超管 */
export const canManageConfig = (user) => isSuperAdmin(user)
