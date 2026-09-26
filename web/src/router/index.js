import { createRouter, createWebHistory } from 'vue-router'
import {
  canEditOwnProfile,
  canImportMembers,
  canManageAnnouncement,
  canManageConfig,
  canManageDict,
  canManageMemberRoster,
  canReviewRecruit,
  canViewAnnouncement,
  canViewDashboard,
  canViewMemberRoster
} from '@/constants/roles'
import { setupRouterGuard } from './guard'

/**
 * 路由表（PRD §8.2 页面地图 v3.2 —— 场景层 + 权限层）
 *
 * 外壳按**场景**分，只有两套：
 *   - 公开场景：`PublicLayout`（登录 / 报名 / 状态查询）—— 页面上不得出现任何登录后才有的元素
 *   - 内部场景：`AppShell`（其余全部）—— 登录后**共用同一套外壳**，
 *     菜单按资格从本表生成（`meta.menu`），不再有"管理端 / 成员端"两套界面
 *
 * 路径策略（T19 拍板）：**保留现有路径**，`/admin` 退化为纯命名空间，不再代表另一套外壳。
 *
 * meta 说明：
 *   title  页面标题（也用作文档标题）
 *   task   该页面由哪个任务点实现（便于追账）
 *   menu   内部导航声明：`{ label, order, capability }`
 *          - 不写 `menu` 的路由不会出现在任何菜单里（公开页、404、改密页）
 *          - `capability` 是 `constants/roles.js` 的具名能力函数，**守卫判定与菜单可见性共用它**
 *            （清单 §6 D108：加一个页面只改这一张表）
 */
const routes = [
  // ---------- 公开场景 ----------
  {
    path: '/',
    component: () => import('@/layouts/PublicLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '登录', public: true, task: 'T4' }
      },
      {
        path: 'init',
        name: 'initAdmin',
        component: () => import('@/views/auth/InitAdminView.vue'),
        meta: { title: '系统初始化', public: true, task: 'T4' }
      },
      {
        path: 'apply',
        name: 'apply',
        component: () => import('@/views/public/ApplyView.vue'),
        meta: { title: '入社报名', public: true, task: 'T6' }
      },
      {
        path: 'query',
        name: 'query',
        component: () => import('@/views/public/QueryView.vue'),
        meta: { title: '审核状态查询', public: true, task: 'T8' }
      }
    ]
  },

  // ---------- 内部场景（统一外壳；/admin 只是路径命名空间） ----------
  {
    path: '/',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/member/HomeView.vue'),
        meta: { title: '工作台', task: 'T19', menu: { order: 10, capability: canViewDashboard } }
      },
      {
        path: '/admin/audit',
        name: 'adminAudit',
        component: () => import('@/views/admin/AuditView.vue'),
        meta: { title: '审核管理台', task: 'T7', menu: { order: 20, capability: canReviewRecruit } }
      },
      {
        path: 'announcement',
        name: 'announcement',
        component: () => import('@/views/member/AnnouncementView.vue'),
        meta: { title: '公告', task: 'T12', menu: { order: 30, capability: canViewAnnouncement } }
      },
      {
        path: 'members',
        name: 'members',
        component: () => import('@/views/member/MembersView.vue'),
        meta: {
          title: '成员列表',
          task: 'T10',
          menu: { order: 40, capability: canViewMemberRoster }
        }
      },
      {
        path: '/admin/members',
        name: 'adminMembers',
        component: () => import('@/views/admin/MemberAdminView.vue'),
        meta: {
          title: '成员档案',
          task: 'T10',
          menu: { order: 50, capability: canManageMemberRoster }
        }
      },
      {
        path: '/admin/announcement',
        name: 'adminAnnouncement',
        component: () => import('@/views/admin/AnnouncementAdminView.vue'),
        meta: {
          title: '公告管理',
          task: 'T12',
          menu: { order: 60, capability: canManageAnnouncement }
        }
      },
      {
        path: '/admin/dashboard',
        name: 'adminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '看板', task: 'T15', menu: { order: 70, capability: canViewDashboard } }
      },
      {
        path: '/admin/import',
        name: 'adminImport',
        component: () => import('@/views/admin/ImportView.vue'),
        meta: { title: 'Excel 导入', task: 'T13', menu: { order: 80, capability: canImportMembers } }
      },
      {
        path: '/admin/dict',
        name: 'adminDict',
        component: () => import('@/views/admin/DictView.vue'),
        meta: { title: '字典管理', task: 'T5', menu: { order: 90, capability: canManageDict } }
      },
      {
        path: '/admin/settings',
        name: 'adminSettings',
        component: () => import('@/views/admin/SettingsView.vue'),
        meta: { title: '纳新设置', task: 'T6', menu: { order: 100, capability: canManageConfig } }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/member/ProfileView.vue'),
        meta: { title: '个人中心', task: 'T11', menu: { order: 110, capability: canEditOwnProfile } }
      },
      {
        // 首登强制改密的落点：不放进菜单
        path: 'change-password',
        name: 'changePassword',
        component: () => import('@/views/auth/ChangePasswordView.vue'),
        meta: { title: '修改密码', task: 'T4' }
      },
      // 老书签兼容：/admin 进审核台（能否进由守卫按该页资格判定）
      { path: '/admin', redirect: '/admin/audit' }
    ]
  },

  // ---------- 兜底 ----------
  {
    path: '/404',
    name: 'notFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

setupRouterGuard(router)

export default router
