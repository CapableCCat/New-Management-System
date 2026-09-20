<script setup>
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'

/**
 * 后端连通性检查（仅 dev 显示）
 *
 * 用途：一眼验证「Vite 代理 /api → 后端 8080 + axios 封装 + 统一返回体」这条链路是通的。
 * T4 之后可移除，或只在登录页 debug 面板里保留。
 */
const loading = ref(false)
const health = ref(null)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    health.value = await getHealth()
  } catch (e) {
    health.value = null
    error.value = e?.message || '请求失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="health">
    <div class="health__head">
      <span class="health__title">后端连通性检查</span>
      <el-button text size="small" :loading="loading" @click="load">重新检测</el-button>
    </div>
    <p v-if="error" class="health__error">链路异常：{{ error }}</p>
    <ul v-else-if="health" class="health__list">
      <li>{{ health.app }} · profile={{ health.profile }} · v{{ health.version }}</li>
      <li>运行时长：{{ health.uptime }}</li>
      <li>服务器时间：{{ health.time }}</li>
      <li v-for="(component, name) in health.components" :key="name">
        {{ name }}：<b :class="component.status === 'UP' ? 'is-up' : 'is-down'">{{
          component.status
        }}</b>
        <span v-if="component.latencyMs >= 0">（{{ component.latencyMs }}ms）</span>
        <span v-if="component.detail"> — {{ component.detail }}</span>
      </li>
    </ul>
    <p v-else class="health__loading">检测中…</p>
  </div>
</template>

<style scoped>
.health {
  margin-top: 16px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: var(--brand-radius);
  background: #fff;
  font-size: 12px;
  color: #606266;
}

.health__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.health__title {
  font-weight: 500;
  color: #303133;
}

.health__list {
  margin: 0;
  padding-left: 16px;
  line-height: 1.9;
}

.health__error {
  margin: 0;
  color: #f56c6c;
}

.health__loading {
  margin: 0;
  color: #909399;
}

.is-up {
  color: #67c23a;
}

.is-down {
  color: #f56c6c;
}
</style>
