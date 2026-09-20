import request from './request'

/** 字典类型元数据（公开） */
export const getDictTypes = () => request.get('/dict/types')

/** 某类型下启用的条目（公开，报名页/查询页/成员端用） */
export const getDict = (type) => request.get(`/dict/${type}`)

/** 管理端：某类型下全部条目（含停用，仅超管） */
export const getDictAdminList = (type) => request.get('/dict/admin/list', { params: { type } })

/** 管理端：新增条目（仅超管） */
export const createDict = (data) => request.post('/dict/admin/create', data)

/** 管理端：编辑条目（仅超管，编码不可改） */
export const updateDict = (id, data) => request.put(`/dict/admin/${id}`, data)

/** 管理端：上移 / 下移（仅超管） */
export const moveDict = (id, direction) =>
  request.put(`/dict/admin/${id}/move`, null, { params: { direction } })
