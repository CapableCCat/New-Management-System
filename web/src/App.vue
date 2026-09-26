<script setup>
/**
 * 根组件：渲染当前路由对应的布局（布局由路由表决定），
 * 并给整个应用套上 Element Plus 的语言配置。
 *
 * `el-config-provider`（组件由 unplugin-vue-components 自动引入）负责把 locale 下发给
 * 该组件树下的所有 Element Plus 组件 —— 不配的话内置文案是英文（OK / Cancel）。
 * Vant 没有对应的 provider，走 stores/locale.js 里的命令式 `Locale.use()`。
 */
import { useLocaleStore } from '@/stores/locale'

const localeStore = useLocaleStore()
</script>

<template>
  <el-config-provider :locale="localeStore.elementLocale">
    <router-view />
  </el-config-provider>
</template>
