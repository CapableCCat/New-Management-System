<script setup>
/**
 * 公告管理（占位）—— T12 实现
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="公告管理"
      task="T12"
      desc="发布 / 编辑 / 删除公告（PRD F-008）"
      :points="[
        '字段：标题、富文本内容、是否置顶',
        '富文本前后端双重 XSS 白名单清洗',
        '列表按「置顶优先 + 发布时间倒序」；部长及以上可发布'
      ]"
    />
  </div>
</template>
