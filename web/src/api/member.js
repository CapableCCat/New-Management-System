import request from './request'

/**
 * 成员档案（F-006）—— 成员端「成员展板」与管理端「成员档案」共用。
 *
 * 行范围（部长=本部门）与列范围（成员只拿基础列）都由**后端**裁剪，
 * 前端只负责控制列/按钮的可见性。
 */

/** 成员列表（分页 + 检索；不传的筛选项请直接省略，避免空串落进查询条件） */
export const getMemberList = (params) => request.get('/member/list', { params })

/** 成员详情（越出行范围后端返回 40300） */
export const getMemberDetail = (id) => request.get(`/member/${id}`)

/** 编辑成员档案（超管 / 社长团 / 部长） */
export const updateMember = (data) => request.put('/member/admin/update', data)
