<script setup>
/**
 * 内部场景统一外壳（T19 —— 取代原 MemberLayout / AdminLayout 两套界面；T22 加两级分组与图标）
 *
 * 全部内部页面共用这一套外壳，**菜单完全由路由表生成**（`router/menu.js`）：
 *   - 桌面：左侧菜单，**按 `MENU_GROUPS` 两级分组**，每项带图标
 *   - 移动：底部 tabbar（组序最靠前的 3 项，带图标）+「更多」抽屉（同样分组）
 *   - 右上角：账号区（头像/昵称）**hover 出下拉** —— 个人中心 / 退出登录（T22 从左侧菜单移上来）
 * 「谁能看到哪一项」只由路由 meta.menu.capability 决定 —— 与守卫判定同源（清单 §6 D108）。
 *
 * 因此这里不再有"管理端菜单 / 成员端菜单"，也不再需要「进入管理端 / 返回成员端」按钮。
 *
 * ⚠️ 本轮（T22）**只做分组 / 图标 / 账号区**，不改断点行为：
 *    768~1024px 的「图标折叠态」是 T23 的事，现在仍是 `isMobile` 二选一（≥768 侧栏 / <768 底栏）。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Bell,
  ChatDotRound,
  Collection,
  DataAnalysis,
  Finished,
  HomeFilled,
  Menu as MenuIcon,
  Notebook,
  Setting,
  Upload,
  UserFilled
} from '@element-plus/icons-vue'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useIsMobile } from '@/composables/useIsMobile'
import { useUserStore } from '@/stores/user'
import { useRouteMenu } from '@/router/menu'
import AnnouncementBell from '@/components/AnnouncementBell.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'

/**
 * 菜单图标映射：路由 `meta.menu.icon` 存的是**组件名字符串**（路由表是纯数据，不该 import 组件），
 * 这里做**显式**映射 —— 显式 import 才能被 tree-shake；漏配时降级为不显示图标（不报错）。
 */
const MENU_ICONS = {
  Bell,
  ChatDotRound,
  Collection,
  DataAnalysis,
  Finished,
  HomeFilled,
  Notebook,
  Setting,
  Upload,
  UserFilled
}

const isMobile = useIsMobile()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { groups, tabbarItems } = useRouteMenu()

const drawerVisible = ref(false)
const currentTitle = computed(() => route.meta.title || APP_NAME)
const displayName = computed(() => userStore.profile?.name || '未登录')
const avatarInitial = computed(() => (userStore.profile?.name || '?').slice(0, 1))

function iconOf(name) {
  return MENU_ICONS[name] || null
}

/** 右上角账号下拉：个人中心 / 退出登录 */
async function onAccountCommand(command) {
  if (command === 'profile') {
    router.push(ROUTE_PATH.PROFILE)
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    userStore.clear()
    router.replace({ path: ROUTE_PATH.LOGIN })
  }
}
</script>

<template>
  <div class="app-shell">
    <!-- 桌面：左侧菜单（两级分组 + 图标） -->
    <aside v-if="!isMobile" class="app-aside">
      <div class="app-brand">{{ APP_NAME }}</div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item-group v-for="group in groups" :key="group.key" :title="group.label">
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon v-if="iconOf(item.icon)"><component :is="iconOf(item.icon)" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </aside>

    <div class="app-body">
      <header class="app-header">
        <!-- 移动端折叠入口：用标准三横杠图标（原来的文字「菜单」不显眼，用户找不到 —— T22） -->
        <button
          v-if="isMobile"
          class="app-menu-toggle"
          type="button"
          aria-label="打开菜单"
          @click="drawerVisible = true"
        >
          <el-icon :size="20"><MenuIcon /></el-icon>
        </button>
        <span class="app-title">{{ currentTitle }}</span>
        <div class="app-actions">
          <AnnouncementBell />
          <LocaleSwitch />
          <!-- 账号区：hover 出下拉（个人中心 / 退出登录） -->
          <el-dropdown trigger="hover" @command="onAccountCommand">
            <span class="app-account">
              <el-avatar :size="26" :src="userStore.profile?.avatarUrl || undefined">
                {{ avatarInitial }}
              </el-avatar>
              <span class="app-user">{{ displayName }}</span>
              <el-icon class="app-account__caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-main">
        <router-view />
      </main>
    </div>

    <!-- 移动：底部 tabbar（高频 3 项 + 更多，均带图标） -->
    <nav v-if="isMobile" class="app-tabbar">
      <router-link
        v-for="item in tabbarItems"
        :key="item.path"
        :to="item.path"
        class="app-tabbar__item"
        :class="{ 'is-active': route.path === item.path }"
      >
        <el-icon v-if="iconOf(item.icon)" :size="18"><component :is="iconOf(item.icon)" /></el-icon>
        <span>{{ item.label }}</span>
      </router-link>
      <button
        class="app-tabbar__item app-tabbar__more"
        :class="{ 'is-active': drawerVisible }"
        type="button"
        @click="drawerVisible = true"
      >
        <el-icon :size="18"><MenuIcon /></el-icon>
        <span>更多</span>
      </button>
    </nav>

    <!-- 移动：全部菜单（按资格生成的那一份，同样分组 + 图标） -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="260px" :title="APP_NAME">
      <el-menu :default-active="route.path" router @select="drawerVisible = false">
        <el-menu-item-group v-for="group in groups" :key="group.key" :title="group.label">
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon v-if="iconOf(item.icon)"><component :is="iconOf(item.icon)" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-drawer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100%;
}

.app-aside {
  display: flex;
  flex-direction: column;
  width: 208px;
  flex: none;
  background: var(--bg-surface);
  border-right: 1px solid var(--border-color);
}

.app-brand {
  padding: 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
}

.app-menu {
  flex: 1;
  /* 分组后条目变多，高度不够时允许滚动 */
  overflow-y: auto;
  border-right: none;
}

.app-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-color);
}

.app-menu-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 6px;
  color: var(--text-regular);
  background: none;
  border: none;
  border-radius: var(--radius-control);
  cursor: pointer;
}

.app-menu-toggle:hover {
  background: var(--el-fill-color-light, #f5f7fa);
  color: var(--brand-primary);
}

.app-title {
  font-size: 15px;
  font-weight: 500;
}

.app-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

/* 账号区：hover 触发下拉（触发器本身也要有 hover 反馈，否则用户不知道这里能点） */
.app-account {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 6px;
  border-radius: var(--radius-pill);
  cursor: pointer;
  outline: none;
}

.app-account:hover,
.app-account:focus-visible {
  background: var(--el-fill-color-light, #f5f7fa);
}

.app-user {
  font-size: 13px;
  color: var(--text-regular);
}

.app-account__caret {
  font-size: 12px;
  color: var(--text-secondary);
}

.app-main {
  flex: 1;
  min-width: 0;
  /* 移动端给底部 tabbar 留出位置，避免最后一行被挡住 */
  padding-bottom: 56px;
}

@media (min-width: 768px) {
  .app-main {
    padding-bottom: 0;
  }
}

.app-tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  background: var(--bg-surface);
  border-top: 1px solid var(--border-color);
  padding-bottom: env(safe-area-inset-bottom);
}

.app-tabbar__item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 0;
  font-size: 12px;
  color: var(--text-secondary);
  background: none;
  border: none;
  font-family: inherit;
}

.app-tabbar__item.is-active {
  color: var(--brand-primary);
  font-weight: 500;
}
</style>
