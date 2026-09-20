import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { STORAGE_KEY } from '@/constants/app'
import { canEnterAdmin, canManageAll, canManageDepartment, isSuperAdmin } from '@/constants/roles'

function readCachedProfile() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY.PROFILE) || 'null')
  } catch {
    return null
  }
}

/**
 * 当前登录用户
 *
 * T3 只搭骨架：token 与 profile 都从 localStorage 读，供路由守卫与布局判断使用。
 * T4 接入真实登录后，由登录接口写入 /user/current 拉取并回填。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(STORAGE_KEY.TOKEN) || '')
  const profile = ref(readCachedProfile())

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
  }

  return {
    token,
    profile,
    isLoggedIn,
    isSuperAdminUser,
    canManageAllUsers,
    canManageDeptUsers,
    canEnterAdminPage,
    setToken,
    setProfile,
    clear
  }
})
