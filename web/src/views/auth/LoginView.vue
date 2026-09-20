<script setup>
/**
 * 登录页（占位）—— T4 实现
 * 公开端入口：登录 / 我要报名 / 查询审核状态
 */
const isDev = import.meta.env.DEV
</script>

<template>
  <div class="login">
    <PlaceholderCard
      title="登录"
      task="T4"
      desc="手机号 + 密码 + 图形验证码（F-002）"
      :points="[
        'BCrypt 校验 + 失败 5 次锁定 15 分钟（Redis）',
        '首登强制改密：activated_at 为空 → 跳改密页',
        '登录成功由 Sa-Token 返回 token，前端存本地并在 Header 回传',
        '忘记密码：联系管理员重置'
      ]"
    />
    <div class="login-links">
      <router-link to="/apply">我要报名</router-link>
      <router-link to="/query">查询审核状态</router-link>
    </div>
    <HealthCheckCard v-if="isDev" />
  </div>
</template>

<style scoped>
.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  font-size: 13px;
}
</style>
