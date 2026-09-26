import { ElMessage } from 'element-plus'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

/**
 * 路由守卫
 *
 * 顺序：
 *   1. 页面标题
 *   2. 系统初始化：未初始化 → 强制去引导页；已初始化 → 引导页不可再进
 *   3. 登录态：公开页放行，其余未登录跳登录页（带 redirect）
 *   4. 首登强制改密：未改密只能待在改密页（后端拦截器同样会兜底）
 *   5. 角色骨架：管理端资格 / 社长团资格（leaderGroup）/ 超管专属页
 *
 * 真正的权限判定在后端；前端只负责"别让用户点进去白跑一趟"。
 */
export function setupRouterGuard(router) {
  router.beforeEach(async (to) => {
    document.title = to.meta.title ? `${to.meta.title} · ${APP_NAME}` : APP_NAME

    const userStore = useUserStore()

    // 1. 系统初始化状态（会话内只请求一次）
    let initialized = true
    try {
      initialized = await userStore.fetchInitStatus()
    } catch {
      // 后端不可用时保持 true（不强制跳引导页），避免把用户卡死在这一步
    }
    if (!initialized) {
      // 未初始化：只允许待在引导页
      return to.path === ROUTE_PATH.INIT ? true : { path: ROUTE_PATH.INIT }
    }
    if (to.path === ROUTE_PATH.INIT) {
      // 已初始化：引导页不可再进
      return { path: ROUTE_PATH.LOGIN }
    }

    // 2. 公开页
    if (to.meta.public) {
      if (to.name === 'login' && userStore.isLoggedIn) {
        return { path: ROUTE_PATH.HOME }
      }
      return true
    }

    // 3. 需要登录
    if (!userStore.isLoggedIn) {
      return { path: ROUTE_PATH.LOGIN, query: { redirect: to.fullPath } }
    }

    // 4. 首登强制改密
    if (userStore.needChangePassword && to.path !== ROUTE_PATH.CHANGE_PASSWORD) {
      return { path: ROUTE_PATH.CHANGE_PASSWORD }
    }

    // 5. 角色骨架
    if (to.meta.admin && !userStore.canEnterAdminPage) {
      ElMessage.warning('没有管理端访问权限')
      return { path: ROUTE_PATH.HOME }
    }
    if (to.meta.leaderGroup && !userStore.canManageAllUsers) {
      ElMessage.warning('仅社长团与超管可访问')
      return { path: ROUTE_PATH.HOME }
    }
    if (to.meta.superAdmin && !userStore.isSuperAdminUser) {
      ElMessage.warning('仅超管可访问')
      return { path: ROUTE_PATH.HOME }
    }

    return true
  })
}
