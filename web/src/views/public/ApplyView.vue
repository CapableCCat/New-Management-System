<script setup>
/**
 * 公开报名页（占位）—— T6 实现
 * 纳新主链起点，移动第一体验（PRD F-001）
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="入社报名"
      task="T6"
      desc="移动优先 H5 报名页：社团简介 + 表单 + 隐私勾选 + 成功页"
      :points="[
        '表单字段：姓名·手机号·学院·专业（含其他兜底）·意向部门多选·兴趣标签多选·性别（选填）·生源地（选填）',
        '提交写入 recruit_apply（status=待审），不建账号；手机号唯一防重复',
        '被拒绝者可重新提交（更新原记录，状态重置为待审）',
        '弱网重试且保留已填内容；提交成功页告知审核时效与查询方式'
      ]"
    />
  </div>
</template>
