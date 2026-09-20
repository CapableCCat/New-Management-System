<script setup>
/**
 * 成员列表（占位）—— T10 实现
 * 成员端：只展示基础列（姓名/部门/职位/学院/专业/个人简介），不含手机号与学号
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="成员列表"
      task="T10"
      desc="成员端只读，敏感列分级可见（PRD 第五章）"
      :points="[
        '基础列：姓名、部门、职位、学院、专业、个人简介',
        '手机号 / 学号：仅部长、社长团、超管可见，成员之间不可见',
        '检索与筛选按角色与部门范围限制'
      ]"
    />
  </div>
</template>
