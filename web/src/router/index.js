import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuard } from './guard'

/**
 * 路由骨架（PRD §8.2 页面地图）
 *
 * 三套布局：
 *   PublicLayout  公开端（登录 / 报名 / 状态查询）—— 移动优先，不做导航栏
 *   MemberLayout  成员端（首页公告 / 成员列表 / 个人中心）
 *   AdminLayout   管理端（审核台 / 成员档案 / 公告管理 / 看板 / 字典 / Excel 导入）
 *
 * meta 说明：
 *   title   页面标题
 *   public  是否免登录
 *   admin   是否需要管理端资格（超管 / 社长团 / 部长）
 *   task    该页面由哪个任务点实现（T3 阶段各页面均为占位）
 */
const routes = [
  { path: '/', redirect: '/login' },

  // ---------- 公开端 ----------
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

  // ---------- 成员端 ----------
  {
    path: '/',
    component: () => import('@/layouts/MemberLayout.vue'),
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/member/HomeView.vue'),
        meta: { title: '首页', task: 'T12' }
      },
      {
        path: 'announcement',
        name: 'announcement',
        component: () => import('@/views/member/AnnouncementView.vue'),
        meta: { title: '公告', task: 'T12' }
      },
      {
        path: 'members',
        name: 'members',
        component: () => import('@/views/member/MembersView.vue'),
        meta: { title: '成员列表', task: 'T10' }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/member/ProfileView.vue'),
        meta: { title: '个人中心', task: 'T11' }
      },
      {
        path: 'change-password',
        name: 'changePassword',
        component: () => import('@/views/auth/ChangePasswordView.vue'),
        meta: { title: '修改密码', task: 'T4' }
      }
    ]
  },

  // ---------- 管理端 ----------
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/admin/audit' },
      {
        path: 'audit',
        name: 'adminAudit',
        component: () => import('@/views/admin/AuditView.vue'),
        meta: { title: '审核管理台', admin: true, task: 'T7' }
      },
      {
        path: 'members',
        name: 'adminMembers',
        component: () => import('@/views/admin/MemberAdminView.vue'),
        meta: { title: '成员档案', admin: true, task: 'T10' }
      },
      {
        path: 'announcement',
        name: 'adminAnnouncement',
        component: () => import('@/views/admin/AnnouncementAdminView.vue'),
        meta: { title: '公告管理', admin: true, task: 'T12' }
      },
      {
        path: 'dashboard',
        name: 'adminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '看板', admin: true, task: 'T15' }
      },
      {
        path: 'dict',
        name: 'adminDict',
        component: () => import('@/views/admin/DictView.vue'),
        meta: { title: '字典管理', admin: true, superAdmin: true, task: 'T5' }
      },
      {
        path: 'settings',
        name: 'adminSettings',
        component: () => import('@/views/admin/SettingsView.vue'),
        meta: { title: '纳新设置', admin: true, superAdmin: true, task: 'T6' }
      },
      {
        path: 'import',
        name: 'adminImport',
        component: () => import('@/views/admin/ImportView.vue'),
        // leaderGroup：PRD 权限矩阵里「Excel 导入」只给社长团 / 超管，部长没有
        meta: { title: 'Excel 导入', admin: true, leaderGroup: true, task: 'T13' }
      }
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
