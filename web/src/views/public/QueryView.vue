<script setup>
/**
 * 审核状态查询页（占位）—— T8 实现
 * 公开访问，凭手机号 + 图形验证码查询（PRD F-005）
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="审核状态查询"
      task="T8"
      desc="凭手机号 + 图形验证码查询自己的审核结果"
      :points="[
        '三种结果：待审核 / 已通过（引导去登录激活）/ 已拒绝（展示拒绝原因）',
        '只返回状态与拒绝原因，不泄露其他信息',
        '图形验证码防脚本批量探测'
      ]"
    />
  </div>
</template>
