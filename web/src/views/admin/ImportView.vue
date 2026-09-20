<script setup>
/**
 * Excel 批量导入（占位）—— T13 实现
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="Excel 导入"
      task="T13"
      desc="下载模板 → 上传 → 逐行校验 → 建号 → 导出密码清单（PRD F-009）"
      :points="[
        '模板列头：姓名、手机号、学号、学院、专业、部门、职位',
        '逐行校验：手机号格式与重复、学号冲突、部门/职位编码合法性',
        '错误行清单带行号与原因；学号可为空',
        '导入成功后可一次性导出密码清单（姓名 + 手机号 + 初始密码）'
      ]"
    />
  </div>
</template>
