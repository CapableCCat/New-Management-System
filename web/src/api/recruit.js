import request from './request'

/* ---------------- 公开端 ---------------- */

/** 报名页配置：报名开关 + 社团简介 + 审核时效文案（公开） */
export const getRecruitInfo = () => request.get('/recruit/info')

/** 提交报名（公开，需图形验证码） */
export const submitApply = (data) => request.post('/recruit/apply', data)

/** 查询审核状态（公开，需图形验证码；只返回状态与拒绝原因，未找到时 data 为 null） */
export const queryApplyStatus = (data) => request.post('/recruit/status', data)

/* ---------------- 审核管理台（需登录） ---------------- */

/** 报名列表（分页 + 筛选；部长自动只返回意向含本部门的记录） */
export const getApplyList = (params) => request.get('/recruit/admin/list', { params })

/** 审核台统计（按可见范围） */
export const getApplyStats = () => request.get('/recruit/admin/stats')

/** 单条通过：建号并返回明文初始密码 */
export const approveApply = (data) => request.post('/recruit/admin/approve', data)

/** 批量通过：返回密码清单与失败原因 */
export const approveApplyBatch = (data) => request.post('/recruit/admin/approve-batch', data)

/** 拒绝：原因必填 */
export const rejectApply = (data) => request.post('/recruit/admin/reject', data)

/** 短信通知提效工具所需配置（系统链接 + 两个短信模板，审核台角色只读） */
export const getSmsConfig = () => request.get('/recruit/admin/sms-config')

/**
 * 导出报名/审核数据（PRD F-013，仅超管 / 社长团）
 *
 * `recruit_apply` 全量、含审核留痕；返回二进制 Blob，交给 utils/download 的 saveBlob 保存
 */
export const exportRecruitApplies = () => request.get('/recruit/admin/export', { responseType: 'blob' })
