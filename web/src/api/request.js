import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ROUTE_PATH, STORAGE_KEY, TOKEN_HEADER } from '@/constants/app'

/**
 * 后端统一返回体：{ code, message, data }
 * 成功 200；其余见《开发任务点清单》§2 错误码约定
 */
export const CODE = {
  SUCCESS: 200,
  PARAM_ERROR: 40000,
  CAPTCHA_INVALID: 40001,
  LOGIN_FAILED: 40002,
  ACCOUNT_LOCKED: 40003,
  ACCOUNT_FROZEN: 40004,
  NEED_CHANGE_PASSWORD: 40005,
  UNAUTHORIZED: 40100,
  FORBIDDEN: 40300,
  NOT_FOUND: 40400,
  SYSTEM_ERROR: 50000,
  DOWNSTREAM_ERROR: 50001
}

/** 业务异常：拦截器已经提示过，业务层一般只需 catch 后忽略 */
export class ApiError extends Error {
  constructor(code, message) {
    super(message || '操作失败')
    this.name = 'ApiError'
    this.code = code
  }
}

const service = axios.create({
  // Vite dev 代理把 /api 转给后端并剥掉前缀（见 vite.config.js）
  baseURL: '/api',
  timeout: 15000
})

let redirectingToLogin = false

/** 清掉本地登录态并跳登录页（避免与 router 形成循环依赖，故动态引入） */
async function redirectToLogin() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  localStorage.removeItem(STORAGE_KEY.TOKEN)
  localStorage.removeItem(STORAGE_KEY.PROFILE)
  try {
    const { default: router } = await import('@/router')
    await router.replace({
      path: ROUTE_PATH.LOGIN,
      query: { redirect: router.currentRoute.value.fullPath }
    })
  } finally {
    redirectingToLogin = false
  }
}

/** 跳到改密页（首登强制改密；同样的动态引入避免循环依赖） */
async function redirectToChangePassword() {
  const { default: router } = await import('@/router')
  if (router.currentRoute.value.path !== ROUTE_PATH.CHANGE_PASSWORD) {
    await router.replace({ path: ROUTE_PATH.CHANGE_PASSWORD })
  }
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(STORAGE_KEY.TOKEN)
    if (token) {
      config.headers[TOKEN_HEADER] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  async (response) => {
    const body = response.data
    // 非统一返回体（如文件流、静态资源）直接放行
    if (!body || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === CODE.SUCCESS) {
      // 业务层只关心 data
      return body.data
    }
    if (body.code === CODE.UNAUTHORIZED) {
      await redirectToLogin()
      ElMessage.warning(body.message || '登录已过期，请重新登录')
    } else if (body.code === CODE.NEED_CHANGE_PASSWORD) {
      // 首登未改密：后端会拦下其他接口，这里同步把用户送到改密页
      ElMessage.warning(body.message || '首次登录需先修改密码')
      await redirectToChangePassword()
    } else if (body.code === CODE.FORBIDDEN) {
      ElMessage.error(body.message || '无权限访问')
    } else if (body.code === CODE.SYSTEM_ERROR || body.code === CODE.DOWNSTREAM_ERROR) {
      ElMessage.error(body.message || '系统繁忙，请稍后重试')
    } else {
      ElMessage.error(body.message || '操作失败')
    }
    return Promise.reject(new ApiError(body.code, body.message))
  },
  (error) => {
    const message = error?.response
      ? `请求失败（HTTP ${error.response.status}）`
      : '网络异常，请检查网络后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
