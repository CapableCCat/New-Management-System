<script setup>
import { APP_FULL_NAME, APP_NAME } from '@/constants/app'
</script>

<template>
  <div class="public-layout">
    <header class="public-header">
      <span class="public-brand">{{ APP_NAME }}</span>
      <span class="public-sub">社团管理系统</span>
    </header>

    <main class="public-main">
      <router-view />
    </main>

    <footer class="public-footer">{{ APP_FULL_NAME }} · V1.0</footer>
  </div>
</template>

<style scoped>
.public-layout {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.public-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 14px 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.public-brand {
  font-size: 16px;
  font-weight: 500;
  color: var(--brand-primary);
}

.public-sub {
  font-size: 12px;
  color: #909399;
}

.public-main {
  flex: 1;
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 12px;
  padding-bottom: 40px;
}

.public-footer {
  padding: 16px;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
