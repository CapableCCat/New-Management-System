import request from './request'

/** 当前登录用户信息（不含密码） */
export const getCurrentUser = () => request.get('/user/current')

/** 修改密码（成功后后端会强制登出） */
export const changePassword = (data) => request.post('/user/change-password', data)

/** 编辑自己的资料：学院/专业/性别/生源地/个人简介（姓名、手机号不可自助修改） */
export const updateProfile = (data) => request.put('/user/profile', data)

/** 学号自助补录（仅当学号为空） */
export const updateStudentId = (studentId) => request.put('/user/student-id', { studentId })

/** 上传头像（jpg/png，≤2MB）；返回更新后的当前用户 */
export const uploadAvatar = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/user/avatar', form)
}
