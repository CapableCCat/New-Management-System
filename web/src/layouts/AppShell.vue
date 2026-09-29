<script setup>
/**
 * 内部场景统一外壳（T19 —— 取代原 MemberLayout / AdminLayout 两套界面；T22 加两级分组与图标；T23 加三档响应式）
 *
 * 全部内部页面共用这一套外壳，**菜单完全由路由表生成**（`router/menu.js`）：
 *   - 桌面：左侧菜单，**按 `MENU_GROUPS` 两级分组**，每项带图标
 *   - 移动：底部 tabbar（组序最靠前的 3 项，带图标）+「更多」抽屉（同样分组）
 *   - 右上角：账号区（头像/昵称）**hover 出下拉** —— 个人中心 / 退出登录（T22 从左侧菜单移上来）
 * 「谁能看到哪一项」只由路由 meta.menu.capability 决定 —— 与守卫判定同源（清单 §6 D108）。
 *
 * 三档布局（T23，《V1.0 收尾需求》§4.5）：
 *   ≥1024px    desktop  侧栏完整展开（图标 + 文字），无折叠按钮
 *   768~1023px compact  侧栏**图标态**（64px）：鼠标移入临时展开为浮层；点折叠按钮可固定展开
 *   <768px     mobile   隐藏侧栏，底部 tabbar + 「更多」→ **图标宫格**（T31）
 *
 * ⚠️ 手机档的「更多」**不再弹侧栏**（T31）：手机用户的心理模型是「应用宫格」而不是带缩进的纵向菜单。
 *    宫格复用与侧栏**同一份** `groups`（单一出处），只是换一种排布；见 `components/MoreGrid.vue`。
 *
 * ⚠️ 用的是本文件专属的 `useLayoutMode`（三档），**不是** `useIsMobile`（只有 <768 一档，
 *    被 13 个页面用来切表格/卡片，语义不能动）。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
import { useLayoutMode } from '@/composables/useLayoutMode'
import { useUserStore } from '@/stores/user'
import { useRouteMenu } from '@/router/menu'
import AnnouncementBell from '@/components/AnnouncementBell.vue'
import LocaleSwitch from '@/components/LocaleSwitch.vue'
import MoreGrid from '@/components/MoreGrid.vue'

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

const layoutMode = useLayoutMode()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { groups, tabbarItems } = useRouteMenu()

const isMobile = computed(() => layoutMode.value === 'mobile')
const isCompact = computed(() => layoutMode.value === 'compact')
const isDesktop = computed(() => layoutMode.value === 'desktop')

const drawerVisible = ref(false)
/** 紧凑档：鼠标移入侧栏时临时展开（浮层，不挤内容） */
const hoverExpand = ref(false)
/** 紧凑档：点折叠按钮**固定**展开（占位推内容）—— 触屏没有 hover，必须有这条出路 */
const pinnedExpand = ref(false)

/**
 * 视口高度（宫格弹层按内容算高度，但要留个上限，防止超出屏幕）。
 * 单独监听 resize：`useLayoutMode` 只暴露档位、不带尺寸。
 */
const viewportHeight = ref(typeof window === 'undefined' ? 800 : window.innerHeight)
const syncViewport = () => {
  viewportHeight.value = window.innerHeight
}

/**
 * 手机档「更多」宫格弹层的高度（px）。
 *
 * 按**行数**算而不是写死 —— 不同角色的菜单项数差很多（成员 4 项 / 超管 11 项），
 * 写死高度会让成员看到一大片空白。数字要与 `MoreGrid.vue` 里的 CSS 保持一致。
 */
const GRID_ITEM_HEIGHT = 74
const GRID_TITLE_HEIGHT = 24
const GRID_GROUP_GAP = 6
const GRID_COLUMNS = 4
const SHEET_PADDING = 48

const gridSheetHeight = computed(() => {
  const rows = groups.value.reduce(
    (sum, group) => sum + Math.max(1, Math.ceil(group.items.length / GRID_COLUMNS)),
    0
  )
  const groupCount = groups.value.length
  const content =
    SHEET_PADDING +
    groupCount * GRID_TITLE_HEIGHT +
    rows * GRID_ITEM_HEIGHT +
    Math.max(0, groupCount - 1) * GRID_GROUP_GAP
  // 最多占视口 80%，超出就在弹层里滚（同时再兜一个 240px 下限，避免算出来太小）
  return Math.max(240, Math.min(content, Math.round(viewportHeight.value * 0.8)))
})

/** 宫格点某一项 → 关弹层并跳转 */
function onGridNavigate(path) {
  drawerVisible.value = false
  router.push(path)
}

/** 侧栏是否处于「图标态」 */
const menuCollapsed = computed(
  () => isCompact.value && !hoverExpand.value && !pinnedExpand.value
)
/** 侧栏是否展开（含固定展开与悬停展开） */
const menuExpanded = computed(() => !menuCollapsed.value)

const currentTitle = computed(() => route.meta.title || APP_NAME)
const displayName = computed(() => userStore.profile?.name || '未登录')
const avatarInitial = computed(() => (userStore.profile?.name || '?').slice(0, 1))

function iconOf(name) {
  return MENU_ICONS[name] || null
}

/** 页头折叠按钮：手机档打开抽屉；紧凑档固定展开/收起侧栏 */
function onToggleMenu() {
  if (isMobile.value) {
    drawerVisible.value = true
    return
  }
  pinnedExpand.value = !pinnedExpand.value
  hoverExpand.value = false
}

/** 切档时复位临时状态，避免「在紧凑档固定展开、拉宽到桌面后状态残留」 */
watch(layoutMode, () => {
  hoverExpand.value = false
  pinnedExpand.value = false
})

onMounted(() => {
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})

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
    <!-- 桌面 / 紧凑：左侧菜单（两级分组 + 图标） -->
    <aside
      v-if="!isMobile"
      class="app-aside"
      :class="{
        'is-compact': isCompact,
        'is-expanded': isCompact && menuExpanded,
        'is-pinned': isCompact && pinnedExpand
      }"
      @mouseenter="hoverExpand = true"
      @mouseleave="hoverExpand = false"
    >
      <div class="app-brand">
        <span class="app-brand__text">{{ APP_NAME }}</span>
        <!-- 紧凑档图标态：品牌名缩成首字母，省空间 -->
        <span v-if="menuCollapsed" class="app-brand__mark">O</span>
      </div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item-group v-for="group in groups" :key="group.key" :title="group.label">
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon v-if="iconOf(item.icon)"><component :is="iconOf(item.icon)" /></el-icon>
            <span class="app-menu__label">{{ item.label }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </aside>

    <div class="app-body">
      <header class="app-header">
        <!-- 手机档：打开菜单抽屉；紧凑档：固定展开 / 收起侧栏。统一用标准三横杠图标（T22 换掉了文字「菜单」） -->
        <button
          v-if="!isDesktop"
          class="app-menu-toggle"
          type="button"
          :aria-label="isMobile ? '打开菜单' : '展开或收起菜单'"
          @click="onToggleMenu"
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

    <!-- 手机档：底部 tabbar（高频 3 项 + 更多，均带图标） -->
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

    <!--
      手机档：全部功能 —— **图标宫格**（T31），不是侧栏。
      从底部升起、高度按菜单项数算（见 gridSheetHeight），内容超高时内部滚动。
    -->
    <el-drawer
      v-model="drawerVisible"
      class="app-grid-sheet"
      direction="btt"
      :size="gridSheetHeight"
      :with-header="false"
      :z-index="2200"
    >
      <div class="app-grid-sheet__handle" aria-hidden="true"></div>
      <h3 class="app-grid-sheet__title">全部功能</h3>
      <MoreGrid :groups="groups" :icons="MENU_ICONS" @navigate="onGridNavigate" />
    </el-drawer>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  /* 紧凑档悬停展开的浮层要相对这里定位 */
  position: relative;
  /*
   * T32：**撑满视口高度 + 内部各自滚动** —— 顶栏固定、侧栏与内容各滚各的。
   * 改之前是 `min-height: 100%`（整页一起滚），所以内容一长，顶栏和侧栏都会被滚走。
   *
   * ⚠️ 用 `height: 100%`（沿 html/body/#app 的高度链）而**不是** `100dvh`：
   *    手机浏览器地址栏收放时 dvh 会变化，容器高度跟着变 → 内容会重排、滚动位置会跳；
   *    `100%` 跟踪的是布局视口，地址栏动它不动，更稳。
   */
  height: 100%;
  overflow: hidden;
}

.app-aside {
  display: flex;
  flex-direction: column;
  width: 208px;
  flex: none;
  background: var(--bg-surface);
  border-right: 1px solid var(--border-color);
}

/* ---------- 紧凑档（768~1023px）：图标态 ---------- */
.app-aside.is-compact {
  width: 64px;
  transition: width var(--motion-fast) var(--motion-ease);
}

.app-aside.is-compact.is-expanded {
  width: 208px;
}

/*
 * 悬停展开用**浮层**：只改宽度会让右侧内容跟着抖动。
 * 「点折叠按钮固定展开」时留在文档流里（is-pinned），因为那是用户的明确意图、可以挤内容。
 */
.app-aside.is-compact.is-expanded:not(.is-pinned) {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 30;
  box-shadow: var(--shadow-popup);
}

/* 图标态：隐藏文字，图标居中；组标题退化成一条分隔线，保留分组感 */
.app-aside.is-compact:not(.is-expanded) :deep(.app-menu__label) {
  display: none;
}

.app-aside.is-compact:not(.is-expanded) :deep(.el-menu-item) {
  justify-content: center;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.app-aside.is-compact:not(.is-expanded) .app-brand__text {
  display: none;
}

.app-aside.is-compact.is-expanded .app-brand__mark {
  display: none;
}

.app-brand {
  display: flex;
  align-items: center;
  padding: 16px;
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
  white-space: nowrap;
}

.app-brand__mark {
  font-size: 18px;
  font-weight: 600;
}

.app-menu {
  flex: 1;
  /* 分组后条目变多，高度不够时允许滚动 */
  overflow-y: auto;
  border-right: none;
}

/* 紧凑档图标态：组标题的留白太占高度，压成一条细分隔线（用 :deep 因为标题是 EP 内部渲染的） */
.app-aside.is-compact:not(.is-expanded) :deep(.el-menu-item-group__title) {
  height: 1px;
  margin: 6px 14px;
  padding: 0;
  overflow: hidden;
  font-size: 0;
  line-height: 0;
  background: var(--border-color);
}

.app-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  /* min-height: 0 必须有：flex 子项默认 min-height: auto，内容一长它会被撑高，
     那样内部滚动就失效了（T32） */
  min-height: 0;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 12px;
  /* flex: none —— 顶栏是固定高度条，不能被内容挤压（T32） */
  flex: none;
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
  /* T32：**内容在这里滚**（顶栏与侧栏不跟着动）。min-height: 0 是关键 ——
     flex 子项默认 min-height: auto，不加它内容会被撑高而不是滚动 */
  min-height: 0;
  overflow-y: auto;
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

<style>
/*
 * 宫格弹层的容器样式。
 * ⚠️ 这里**故意不用 scoped** —— el-drawer 的内容是 teleport 到 body 的，
 *    scoped 生成的 `[data-v-xxx] .el-drawer__body` 选择器在 body 层级找不到祖先，规则不会生效。
 * 类名限定在 `.app-grid-sheet` 内，所以不会污染其它抽屉。
 */
.app-grid-sheet .el-drawer__body {
  padding: 8px 16px calc(16px + env(safe-area-inset-bottom));
  overflow-y: auto;
}

.app-grid-sheet.el-drawer.btt {
  border-radius: 16px 16px 0 0;
}

.app-grid-sheet__handle {
  width: 36px;
  height: 4px;
  margin: 0 auto 10px;
  border-radius: 2px;
  background: var(--border-color);
}

.app-grid-sheet__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}
</style>
