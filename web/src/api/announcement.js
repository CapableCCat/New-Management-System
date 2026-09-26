import request from './request'

/**
 * 公告系统（PRD F-008）
 *
 * 权限口径（后端为准，前端只控制可见性）：
 *   - 列表 / 详情：登录即可（成员端只读）
 *   - 发布 / 编辑 / 删除 / 上传配图：超管、社长团、部长
 */

/** 公告列表（置顶优先 + 发布时间倒序；不传的筛选项请直接省略，避免空串落进查询条件） */
export const getAnnouncementList = (params) => request.get('/announcement/list', { params })

/** 公告详情（正文为已清洗的富文本 HTML，渲染前前端还会再清一次） */
export const getAnnouncementDetail = (id) => request.get(`/announcement/${id}`)

/** 发布公告 */
export const createAnnouncement = (data) => request.post('/announcement/admin/create', data)

/** 编辑公告（整体提交：标题 / 正文 / 是否置顶都要带上） */
export const updateAnnouncement = (id, data) => request.put(`/announcement/admin/${id}`, data)

/** 删除公告（逻辑删除） */
export const deleteAnnouncement = (id) => request.delete(`/announcement/admin/${id}`)

/** 上传公告配图（jpg / png，≤2MB）；返回可直接嵌进正文的完整地址 */
export const uploadAnnouncementImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/announcement/admin/image', form)
}
