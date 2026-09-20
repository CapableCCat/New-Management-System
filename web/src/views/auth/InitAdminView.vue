<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { initAdmin } from '@/api/auth'
import { ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

/**
 * 首个超管初始化引导页（F-012）
 *
 * 仅当 user 表为空时可进入；创建成功后该超管首次登录仍需改一次密码。
 */
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ name: '', phone: '', password: '', confirmPassword: '' })

async function onSubmit() {
  loading.value = true
  try {
    await initAdmin({ name: form.name, phone: form.phone, password: form.password })
    ElMessage.success('超管创建成功，请登录')
    userStore.markInitialized()
    await router.replace(ROUTE_PATH.LOGIN)
  } catch {
    // 提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="init">
    <h2 class="page-title">系统初始化</h2>
    <p class="page-desc">
      系统还没有任何成员，请先创建第一个超管账号。创建完成后用该账号登录，并按提示设置正式密码。
    </p>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.name"
          name="name"
          label="姓名"
          maxlength="32"
          placeholder="请输入姓名"
          :rules="[{ required: true, message: '请填写姓名' }]"
        />
        <van-field
          v-model="form.phone"
          name="phone"
          label="手机号"
          type="tel"
          maxlength="11"
          placeholder="将作为登录账号"
          :rules="[
            { required: true, message: '请填写手机号' },
            { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
          ]"
        />
        <van-field
          v-model="form.password"
          name="password"
          label="密码"
          type="password"
          placeholder="8~20 位，须同时含字母和数字"
          :rules="[
            { required: true, message: '请填写密码' },
            { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/, message: '8~20 位，须同时包含字母和数字且不含空格' }
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          name="confirmPassword"
          label="确认密码"
          type="password"
          placeholder="请再次输入密码"
          :rules="[
            { required: true, message: '请再次输入密码' },
            { validator: (value) => value === form.password, message: '两次输入的密码不一致' }
          ]"
        />
      </van-cell-group>

      <div class="init__submit">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          创建超管并进入登录
        </van-button>
      </div>
    </van-form>

    <p class="init__tip">
      该接口只在系统未初始化时可用；已有成员后会被后端拒绝，避免重复初始化。
    </p>
  </div>
</template>

<style scoped>
.init__submit {
  margin: 16px;
}

.init__tip {
  margin: 0 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}
</style>
