import request from './request'

/** 全部系统配置（超管 / 社长团 —— T27 起放宽） */
export const getConfigList = () => request.get('/config/admin/list')

/** 更新单个系统配置（超管 / 社长团，仅允许改白名单键） */
export const updateConfig = (key, value) => request.put('/config/admin/update', { key, value })

/** 上传社团 Logo（jpg/png，≤2MB；超管 / 社长团）。返回新的 Logo 可访问地址 */
export const uploadClubLogo = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/config/admin/logo', form)
}

/** 清除社团 Logo（超管 / 社长团）；报名页回到不显示 Logo 的状态 */
export const removeClubLogo = () => request.delete('/config/admin/logo')
