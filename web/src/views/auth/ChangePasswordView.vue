<script setup>
/**
 * 修改密码（占位）—— T4 实现
 * 两个入口：首次登录强制改密 / 个人中心主动改密
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="修改密码"
      task="T4"
      desc="首登强制改密 + 主动改密共用本页"
      :points="[
        '校验旧密码 → 新密码（强度校验）→ 确认新密码',
        '改密成功后写入 activated_at，并强制登出重新登录',
        '首次登录未改密前，不允许访问其他页面'
      ]"
    />
  </div>
</template>
