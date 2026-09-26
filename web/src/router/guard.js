import { ElMessage } from 'element-plus'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useRecruitStore } from '@/stores/recruit'
import { useUserStore } from '@/stores/user'

/**
 * 路由守卫
 *
 * 顺序：
 *   1. 页面标题
 *   2. 系统初始化：未初始化 → 强制去引导页；已初始化 → 引导页不可再进
 *   3. 根路径落地页：登录用户进工作台；未登录按 recruit_open 决定报名页 / 登录页
 *   4. 登录态：公开页放行，其余未登录跳登录页（带 redirect）
 *   5. 首登强制改密：未改密只能待在改密页（后端拦截器同样会兜底）
 *   6. 资格判定：按路由 `meta.menu.capability`（与菜单可见性共用一份声明）
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

    // 2. 根路径落地页（清单 §6 D110）：落点不写死，按 recruit_open 动态决定 ——
    //    登录用户直接进工作台；未登录时报名开关打开 → 报名页，关闭 → 登录页。
    //    开关取不到（后端不可用）时按更保守的登录页处理。
    if (to.path === '/') {
      if (userStore.isLoggedIn) {
        return { path: ROUTE_PATH.HOME }
      }
      const open = await useRecruitStore().loadOpen()
      return { path: open ? ROUTE_PATH.APPLY : ROUTE_PATH.LOGIN }
    }

    // 3. 公开页
    if (to.meta.public) {
      if (to.name === 'login' && userStore.isLoggedIn) {
        return { path: ROUTE_PATH.HOME }
      }
      return true
    }

    // 4. 需要登录
    if (!userStore.isLoggedIn) {
      return { path: ROUTE_PATH.LOGIN, query: { redirect: to.fullPath } }
    }

    // 5. 首登强制改密
    if (userStore.needChangePassword && to.path !== ROUTE_PATH.CHANGE_PASSWORD) {
      return { path: ROUTE_PATH.CHANGE_PASSWORD }
    }

    // 6. 资格判定（T19）：用路由 `meta.menu.capability` —— 与菜单可见性**共用同一份声明**，
    //    所以不可能出现"菜单里点不进去"或"能进但菜单不显示"（清单 §6 D108）
    const capability = to.meta.menu && to.meta.menu.capability
    if (typeof capability === 'function' && !capability(userStore.profile)) {
      ElMessage.warning(to.meta.title ? `没有访问「${to.meta.title}」的权限` : '没有访问权限')
      return { path: ROUTE_PATH.HOME }
    }

    return true
  })
}
