<script setup>
/**
 * 公开场景外壳（报名 / 查询 / 登录三个页面共用）
 *
 * T24 起支持 **bare（无壳）模式**：路由 `meta.bare === true` 时**不渲染页头与页脚**，
 * 内容区也不再限宽 —— 登录页要用它做「全屏背景 + 表单垂直水平居中」。
 *
 * 为什么用「同一外壳 + 开关」而不是给登录页单独做一个 layout：
 * 公开三页共享一套外壳，将来改公开端只需改一处；只为一个页面的排版需求分叉出第二套外壳，
 * 后面每改一次公开端都要改两处（《V1.0 收尾需求》§九 Q4 定案）。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { APP_FULL_NAME, APP_NAME } from '@/constants/app'
import LocaleSwitch from '@/components/LocaleSwitch.vue'

const route = useRoute()

/** 无壳模式：不显示页头页脚、内容不限宽不限高（登录页用） */
const bare = computed(() => route.meta?.bare === true)
</script>

<template>
  <div class="public-layout" :class="{ 'is-bare': bare }">
    <header v-if="!bare" class="public-header">
      <span class="public-brand">{{ APP_NAME }}</span>
      <span class="public-sub">社团管理系统</span>
      <LocaleSwitch class="public-locale" />
    </header>

    <main class="public-main" :class="{ 'is-bare': bare }">
      <router-view />
    </main>

    <footer v-if="!bare" class="public-footer">{{ APP_FULL_NAME }} · V1.0</footer>
  </div>
</template>

<style scoped>
.public-layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* 无壳模式：铺满视口，让页面自己掌控全屏背景与居中 */
.public-layout.is-bare {
  min-height: 100vh;
}

.public-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 14px 16px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border-color);
}

.public-brand {
  font-size: 16px;
  font-weight: 500;
  color: var(--brand-primary);
}

.public-sub {
  font-size: 12px;
  color: var(--text-secondary);
}

.public-locale {
  margin-left: auto;
}

.public-main {
  flex: 1;
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 12px;
  padding-bottom: 40px;
}

/* 无壳模式下取消限宽与内边距，交给页面自己排 */
.public-main.is-bare {
  max-width: none;
  padding: 0;
}
</style>
