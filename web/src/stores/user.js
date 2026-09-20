import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { STORAGE_KEY } from '@/constants/app'
import { canEnterAdmin, canManageAll, canManageDepartment, isSuperAdmin } from '@/constants/roles'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'

function readCachedProfile() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY.PROFILE) || 'null')
  } catch {
    return null
  }
}

/**
 * 当前登录用户 + 系统初始化状态。
 *
 * T4 起接真实接口：
 *   - login() 写入 token 与 profile（token 存 localStorage，请求时由 axios 拦截器带 osc-token 头）
 *   - 首登未改密时后端会拦下其他接口（code 40005），前端同步把用户送到改密页
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(STORAGE_KEY.TOKEN) || '')
  const profile = ref(readCachedProfile())
  const needChangePassword = ref(false)

  /** 系统是否已初始化；null 表示还没查过 */
  const initialized = ref(null)
  let initStatusPromise = null

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdminUser = computed(() => isSuperAdmin(profile.value))
  const canManageAllUsers = computed(() => canManageAll(profile.value))
  const canManageDeptUsers = computed(() => canManageDepartment(profile.value))
  const canEnterAdminPage = computed(() => canEnterAdmin(profile.value))

  function setToken(value) {
    token.value = value || ''
    if (token.value) {
      localStorage.setItem(STORAGE_KEY.TOKEN, token.value)
    } else {
      localStorage.removeItem(STORAGE_KEY.TOKEN)
    }
  }

  function setProfile(value) {
    profile.value = value || null
    if (profile.value) {
      localStorage.setItem(STORAGE_KEY.PROFILE, JSON.stringify(profile.value))
    } else {
      localStorage.removeItem(STORAGE_KEY.PROFILE)
    }
  }

  function clear() {
    setToken('')
    setProfile(null)
    needChangePassword.value = false
  }

  /** 查询初始化状态（整个会话只真正请求一次） */
  async function fetchInitStatus() {
    if (initialized.value !== null) {
      return initialized.value
    }
    if (!initStatusPromise) {
      initStatusPromise = authApi
        .getInitStatus()
        .then((data) => {
          initialized.value = !!data?.initialized
          return initialized.value
        })
        .catch((error) => {
          initStatusPromise = null
          throw error
        })
    }
    return initStatusPromise
  }

  function markInitialized() {
    initialized.value = true
  }

  async function login(payload) {
    const data = await authApi.login(payload)
    setToken(data.tokenValue)
    setProfile(data.user)
    needChangePassword.value = !!data.needChangePassword
    return data
  }

  async function fetchCurrent() {
    const data = await userApi.getCurrentUser()
    setProfile(data)
    return data
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // 忽略：本地登录态照样清掉
    }
    clear()
  }

  return {
    token,
    profile,
    needChangePassword,
    initialized,
    isLoggedIn,
    isSuperAdminUser,
    canManageAllUsers,
    canManageDeptUsers,
    canEnterAdminPage,
    setToken,
    setProfile,
    clear,
    fetchInitStatus,
    markInitialized,
    login,
    fetchCurrent,
    logout
  }
})
