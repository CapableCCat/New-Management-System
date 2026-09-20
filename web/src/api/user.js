import request from './request'

/** 当前登录用户信息（不含密码） */
export const getCurrentUser = () => request.get('/user/current')

/** 修改密码（成功后后端会强制登出） */
export const changePassword = (data) => request.post('/user/change-password', data)
