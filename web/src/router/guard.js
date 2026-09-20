import { ElMessage } from 'element-plus'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

/**
 * 路由守卫骨架（T3）
 *
 * 三条职责：
 *   1. 设置页面标题
 *   2. 登录态：非公开页未登录 → 去登录页（带 redirect）
 *   3. 角色骨架：管理端页面需要管理端资格（超管 / 社长团 / 部长）
 *
 * 真正的权限判定在后端；前端只负责"别让用户点进去白跑一趟"。
 */
export function setupRouterGuard(router) {
  router.beforeEach((to) => {
    document.title = to.meta.title ? `${to.meta.title} · ${APP_NAME}` : APP_NAME

    const userStore = useUserStore()

    // 公开页（登录 / 报名 / 查询 / 404）
    if (to.meta.public) {
      // 已登录用户访问登录页 → 回首页
      if (to.name === 'login' && userStore.isLoggedIn) {
        return { path: ROUTE_PATH.HOME }
      }
      return true
    }

    // 需要登录
    if (!userStore.isLoggedIn) {
      return { path: ROUTE_PATH.LOGIN, query: { redirect: to.fullPath } }
    }

    // 管理端资格（T4 接入真实登录后由 profile 推导生效）
    if (to.meta.admin && !userStore.canEnterAdminPage) {
      ElMessage.warning('没有管理端访问权限')
      return { path: ROUTE_PATH.HOME }
    }
    if (to.meta.superAdmin && !userStore.isSuperAdminUser) {
      ElMessage.warning('仅超管可访问')
      return { path: ROUTE_PATH.HOME }
    }

    return true
  })
}
