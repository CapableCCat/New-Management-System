import request from './request'

/** 报名页配置：报名开关 + 社团简介 + 审核时效文案（公开） */
export const getRecruitInfo = () => request.get('/recruit/info')

/** 提交报名（公开，需图形验证码） */
export const submitApply = (data) => request.post('/recruit/apply', data)
