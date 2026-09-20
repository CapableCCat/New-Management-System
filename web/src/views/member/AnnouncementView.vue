<script setup>
/**
 * 公告列表 / 详情（占位）—— T12 实现
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="公告"
      task="T12"
      desc="成员端只读：公告列表按「置顶优先 + 发布时间倒序」"
      :points="[
        '列表：标题、发布时间、置顶标记',
        '详情：富文本渲染，内容经 XSS 白名单清洗',
        '实时刷新该模块数据；空态友好提示'
      ]"
    />
  </div>
</template>
