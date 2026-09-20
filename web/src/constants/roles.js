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

/** 能否进入管理端（审核台 / 成员档案 / 公告管理 / 看板 …） */
export const canEnterAdmin = (user) => canManageDepartment(user)
