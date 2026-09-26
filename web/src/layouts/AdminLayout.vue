<script setup>
import { computed, ref } from 'vue'
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

const drawerVisible = ref(false)

/** 管理端菜单（字典管理仅超管可见） */
const menuItems = computed(() => {
  const items = [
    { index: '/admin/audit', label: '审核管理台' },
    { index: '/admin/members', label: '成员档案' },
    { index: '/admin/announcement', label: '公告管理' },
    { index: '/admin/dashboard', label: '看板' }
  ]
  // Excel 导入按 PRD 权限矩阵只给社长团 / 超管（部长没有），故与字典、纳新设置一样按资格显示
  if (userStore.canManageAllUsers) {
    items.push({ index: '/admin/import', label: 'Excel 导入' })
  }
  if (userStore.isSuperAdminUser) {
    items.push({ index: '/admin/dict', label: '字典管理' })
    items.push({ index: '/admin/settings', label: '纳新设置' })
  }
  return items
})

const currentTitle = computed(() => route.meta.title || '管理端')

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
  <div class="admin-layout">
    <!-- 桌面：左侧菜单 -->
    <aside v-if="!isMobile" class="admin-aside">
      <div class="admin-brand">{{ APP_NAME }} · 管理端</div>
      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
      <div class="admin-aside__foot">
        <router-link :to="ROUTE_PATH.HOME">返回成员端</router-link>
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-header">
        <el-button v-if="isMobile" text @click="drawerVisible = true">菜单</el-button>
        <span class="admin-title">{{ currentTitle }}</span>
        <div class="admin-actions">
          <span class="admin-user">{{ userStore.profile?.name || '未登录' }}</span>
          <LocaleSwitch />
          <el-button text size="small" @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>

    <!-- 移动：抽屉菜单 -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="240px" title="管理端菜单">
      <el-menu :default-active="route.path" router @select="drawerVisible = false">
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100%;
}

.admin-aside {
  display: flex;
  flex-direction: column;
  width: 200px;
  flex: none;
  background: #fff;
  border-right: 1px solid #ebeef5;
}

.admin-brand {
  padding: 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
}

.admin-menu {
  flex: 1;
  border-right: none;
}

.admin-aside__foot {
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  font-size: 13px;
}

.admin-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.admin-title {
  font-size: 15px;
  font-weight: 500;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.admin-user {
  font-size: 13px;
  color: #909399;
}

.admin-main {
  flex: 1;
  min-width: 0;
}
</style>
