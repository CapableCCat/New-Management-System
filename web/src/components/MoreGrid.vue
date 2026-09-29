<script setup>
/**
 * 「全部功能」图标宫格（T31）—— 手机档点「更多」时弹出。
 *
 * 为什么不用侧栏（`el-drawer direction="ltr"` + `el-menu`）：
 * 大屏下左侧侧栏是对的，但**手机用户的心理模型是「应用宫格」而不是「带缩进的纵向菜单」**——
 * 两屏信息量相同，但宫格的目标更大、更好点、也更符合直觉（社长 2026-09-29 反馈）。
 *
 * 数据来源：与侧栏**同一份** `useRouteMenu().groups`（清单 §6 D108 的单一出处）——
 * 这里不新增任何菜单数据，只是换一种排布方式。
 *
 * 图标：由父组件把 `AppShell` 里那张**显式映射表**传进来（`icons`），不在这里再建一份，
 * 避免"图标名 → 组件"出现两个出处。
 */
defineProps({
  /** `useRouteMenu().groups`：`[{ key, label, items: [{ path, label, icon }] }]` */
  groups: { type: Array, required: true },
  /** 图标映射表：`{ 组件名: 组件 }`（来自 AppShell 的 MENU_ICONS） */
  icons: { type: Object, required: true }
})

const emit = defineEmits(['navigate'])
</script>

<template>
  <div class="more-grid">
    <section v-for="group in groups" :key="group.key" class="more-grid__group">
      <h4 class="more-grid__title">{{ group.label }}</h4>
      <div class="more-grid__items">
        <button
          v-for="item in group.items"
          :key="item.path"
          class="more-grid__item"
          type="button"
          @click="emit('navigate', item.path)"
        >
          <el-icon v-if="icons[item.icon]" class="more-grid__icon" :size="22">
            <component :is="icons[item.icon]" />
          </el-icon>
          <span class="more-grid__label">{{ item.label }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.more-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.more-grid__title {
  margin: 0 0 2px;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-secondary);
}

/* 4 列宫格：手机上每项仍有 ~80px 宽的可点区域，比纵向菜单项好点得多 */
.more-grid__items {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px 0;
}

.more-grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  /* 固定高度：让宫格高度可预测（外层按行数算弹层高度，见 AppShell） */
  height: 74px;
  padding: 4px 2px;
  font-family: inherit;
  color: var(--text-regular);
  background: none;
  border: none;
  border-radius: var(--radius-control);
  cursor: pointer;
}

.more-grid__item:active {
  background: var(--el-fill-color-light, #f5f7fa);
}

.more-grid__icon {
  color: var(--brand-primary);
}

.more-grid__label {
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
  /* 超长菜单名最多两行，不撑破宫格 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
