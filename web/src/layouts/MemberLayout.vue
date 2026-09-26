<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { APP_NAME, ROUTE_PATH } from '@/constants/app'
import { useIsMobile } from '@/composables/useIsMobile'
import { useUserStore } from '@/stores/user'
import LocaleSwitch from '@/components/LocaleSwitch.vue'

const isMobile = useIsMobile()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 成员端导航（PRD §8.2 成员端页面） */
const navItems = [
  { path: ROUTE_PATH.HOME, label: '首页' },
  { path: '/announcement', label: '公告' },
  { path: '/members', label: '成员' },
  { path: '/profile', label: '我的' }
]

const currentPath = computed(() => route.path)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  // T4 接入真实登出接口后再补：await logoutApi()
  userStore.clear()
  router.replace({ path: ROUTE_PATH.LOGIN })
}
</script>

<template>
  <div class="member-layout">
    <header class="member-header">
      <span class="member-brand">{{ APP_NAME }}</span>
      <nav v-if="!isMobile" class="member-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="member-nav__item"
          :class="{ 'is-active': currentPath === item.path }"
        >
          {{ item.label }}
        </router-link>
      </nav>
      <div class="member-actions">
        <span class="member-user">{{ userStore.profile?.name || '未登录' }}</span>
        <LocaleSwitch />
        <el-button text size="small" @click="handleLogout">退出</el-button>
      </div>
    </header>

    <main class="member-main">
      <router-view />
    </main>

    <nav v-if="isMobile" class="member-tabbar">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="member-tabbar__item"
        :class="{ 'is-active': currentPath === item.path }"
      >
        {{ item.label }}
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.member-layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.member-brand {
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
  white-space: nowrap;
}

.member-nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.member-nav__item {
  padding: 4px 12px;
  border-radius: var(--brand-radius);
  color: #606266;
}

.member-nav__item.is-active {
  background: color-mix(in srgb, var(--brand-primary) 10%, #fff);
  color: var(--brand-primary);
}

.member-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.member-user {
  font-size: 13px;
  color: #909399;
}

.member-main {
  flex: 1;
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
}

.member-tabbar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding-bottom: env(safe-area-inset-bottom);
}

.member-tabbar__item {
  flex: 1;
  padding: 10px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.member-tabbar__item.is-active {
  color: var(--brand-primary);
  font-weight: 500;
}
</style>
