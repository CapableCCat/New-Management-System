import request from './request'

/** 全部系统配置（仅超管） */
export const getConfigList = () => request.get('/config/admin/list')

/** 更新单个系统配置（仅超管，仅允许改白名单键） */
export const updateConfig = (key, value) => request.put('/config/admin/update', { key, value })
