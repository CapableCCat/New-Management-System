<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/user'
import { ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

/** 修改密码（首登强制改密 + 个人中心主动改密共用，F-002 / F-007） */
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const isForced = computed(() => userStore.needChangePassword)

/** 与后端 PasswordPolicy 一致的强度校验 */
async function onSubmit() {
  loading.value = true
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请用新密码重新登录')
    userStore.clear()
    await router.replace(ROUTE_PATH.LOGIN)
  } catch {
    // 提示由 axios 拦截器统一处理，这里保留已填内容
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <h2 class="page-title">修改密码</h2>
    <p class="page-desc">
      <template v-if="isForced">首次登录需要先设置新密码，完成后请用新密码重新登录。</template>
      <template v-else>为了账号安全，修改成功后需要重新登录。</template>
    </p>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.oldPassword"
          name="oldPassword"
          label="旧密码"
          type="password"
          placeholder="请输入旧密码"
          :rules="[{ required: true, message: '请填写旧密码' }]"
        />
        <van-field
          v-model="form.newPassword"
          name="newPassword"
          label="新密码"
          type="password"
          placeholder="8~20 位，须同时含字母和数字"
          :rules="[
            { required: true, message: '请填写新密码' },
            { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/, message: '8~20 位，须同时包含字母和数字且不含空格' }
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          name="confirmPassword"
          label="确认新密码"
          type="password"
          placeholder="请再次输入新密码"
          :rules="[
            { required: true, message: '请再次输入新密码' },
            { validator: (value) => value === form.newPassword, message: '两次输入的密码不一致' }
          ]"
        />
      </van-cell-group>

      <div class="change-password__submit">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          确认修改
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<style scoped>
.change-password__submit {
  margin: 16px;
}
</style>
