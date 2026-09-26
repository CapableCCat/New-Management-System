import request from './request'

/**
 * 轻量反馈（PRD F-014）
 *
 * 权限口径（后端为准）：
 *   - 提交：**免登录**（新生在报名结果态可能还没账号），但必须带一次性图形验证码
 *   - 列表 / 标记处理：社长团、超管
 */

/** 提交反馈（content / contact? / source / captchaKey / captchaCode） */
export const submitFeedback = (data) => request.post('/feedback/submit', data)

/** 反馈列表（来源、处理状态可筛；新→旧） */
export const getFeedbackList = (params) => request.get('/feedback/admin/list', { params })

/** 标记已处理 / 未处理（handled：0 未处理 1 已处理） */
export const handleFeedback = (id, handled) => request.post('/feedback/admin/handle', { id, handled })
