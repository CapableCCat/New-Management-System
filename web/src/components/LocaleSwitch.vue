<script setup>
/**
 * 语言切换（中 / 英）
 *
 * 只影响**组件库内置文案**（Element Plus / Vant 的确认框、分页、上传、空态等），
 * 页面自己写的文案仍是中文 —— 说明见 stores/locale.js。
 *
 * 按钮显示的是「另一种语言」，点一下切过去；样式刻意做小做轻，不抢视觉。
 */
import { useLocaleStore } from '@/stores/locale'

const localeStore = useLocaleStore()
</script>

<template>
  <button
    type="button"
    class="locale-switch"
    :title="localeStore.isChinese ? 'Switch to English' : '切换为中文'"
    @click="localeStore.toggle()"
  >
    {{ localeStore.switchLabel }}
  </button>
</template>

<style scoped>
.locale-switch {
  min-width: 30px;
  padding: 2px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: transparent;
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.locale-switch:hover {
  color: var(--brand-primary);
  border-color: var(--brand-primary);
}
</style>
