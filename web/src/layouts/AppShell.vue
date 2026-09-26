<script setup>
/**
 * 内部场景统一外壳（T19 —— 取代原 MemberLayout / AdminLayout 两套界面）
 *
 * 全部内部页面共用这一套外壳，**菜单完全由路由表生成**（`router/menu.js`）：
 *   - 桌面：左侧菜单
 *   - 移动：底部 tabbar（菜单里 order 最小的 3 项）+「更多」抽屉
 * 「谁能看到哪一项」只由路由 meta.menu.capability 决定 —— 与守卫判定同源（清单 §6 D108）。
 *
 * 因此这里不再有"管理端菜单 / 成员端菜单"，也不再需要「进入管理端 / 返回成员端」按钮。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useIsMobile } from '@/composables/useIsMobile'
import { useUserStore } from '@/stores/user'
import { useRouteMenu } from '@/router/menu'
import AnnouncementBell from '@/components/AnnouncementBell.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'

const isMobile = useIsMobile()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { items, tabbarItems } = useRouteMenu()

const drawerVisible = ref(false)
const currentTitle = computed(() => route.meta.title || APP_NAME)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  userStore.clear()
  router.replace({ path: ROUTE_PATH.LOGIN })
}
</script>

<template>
  <div class="app-shell">
    <!-- 桌面：左侧菜单 -->
    <aside v-if="!isMobile" class="app-aside">
      <div class="app-brand">{{ APP_NAME }}</div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item v-for="item in items" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="app-body">
      <header class="app-header">
        <el-button v-if="isMobile" text @click="drawerVisible = true">菜单</el-button>
        <span class="app-title">{{ currentTitle }}</span>
        <div class="app-actions">
          <AnnouncementBell />
          <span class="app-user">{{ userStore.profile?.name || '未登录' }}</span>
          <LocaleSwitch />
          <el-button text size="small" @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="app-main">
        <router-view />
      </main>
    </div>

    <!-- 移动：底部 tabbar（高频 3 项 + 更多） -->
    <nav v-if="isMobile" class="app-tabbar">
      <router-link
        v-for="item in tabbarItems"
        :key="item.path"
        :to="item.path"
        class="app-tabbar__item"
        :class="{ 'is-active': route.path === item.path }"
      >
        {{ item.label }}
      </router-link>
      <button
        class="app-tabbar__item app-tabbar__more"
        :class="{ 'is-active': drawerVisible }"
        type="button"
        @click="drawerVisible = true"
      >
        更多
      </button>
    </nav>

    <!-- 移动：全部菜单（按资格生成的那一份） -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="240px" :title="APP_NAME">
      <el-menu :default-active="route.path" router @select="drawerVisible = false">
        <el-menu-item v-for="item in items" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
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
  width: 200px;
  flex: none;
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.app-brand {
  padding: 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
}

.app-menu {
  flex: 1;
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
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.app-title {
  font-size: 15px;
  font-weight: 500;
}

.app-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.app-user {
  font-size: 13px;
  color: #909399;
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
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding-bottom: env(safe-area-inset-bottom);
}

.app-tabbar__item {
  flex: 1;
  padding: 10px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
  background: none;
  border: none;
  font-family: inherit;
}

.app-tabbar__item.is-active {
  color: var(--brand-primary);
  font-weight: 500;
}
</style>
