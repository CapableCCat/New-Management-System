<script setup>
defineProps({
  /** 页面标题 */
  title: { type: String, required: true },
  /** 由哪个任务点实现（占位阶段对照用） */
  task: { type: String, default: '' },
  /** 一句话说明 */
  desc: { type: String, default: '' },
  /** 该页要做的要点清单 */
  points: { type: Array, default: () => [] }
})
</script>

<template>
  <section class="placeholder">
    <h2 class="page-title">{{ title }}</h2>
    <p class="page-desc">
      <span v-if="task" class="placeholder__tag">实现于 {{ task }}</span>
      {{ desc }}
    </p>
    <ul v-if="points.length" class="placeholder__list">
      <li v-for="point in points" :key="point">{{ point }}</li>
    </ul>
    <slot />
  </section>
</template>

<style scoped>
.placeholder__tag {
  display: inline-block;
  margin-right: 8px;
  padding: 0 8px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--brand-primary) 12%, #fff);
  color: var(--brand-primary);
  font-size: 12px;
}

.placeholder__list {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.9;
}
</style>
