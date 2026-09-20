<script setup>
/**
 * 个人中心（占位）—— T11 实现
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="个人中心"
      task="T11"
      desc="三块资料卡 + 编辑 + 学号补录 + 改密 + 头像上传（PRD F-007）"
      :points="[
        '个人信息（姓名/手机号/学号）· 专业信息（学院/专业）· 社团信息（部门/职位/加入时间）',
        '可自助修改：学院/专业/性别/生源地/个人简介/头像；姓名与手机号需联系管理员',
        '学号为空时可自助补录，冲突给出提示',
        '头像上传走 MinIO（≤2MB，jpg/png）'
      ]"
    />
  </div>
</template>
