<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

/** 登录页（F-002）：手机号 + 密码 + 算术图形验证码；移动端用 Vant */
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isDev = import.meta.env.DEV
const loading = ref(false)
const captchaImage = ref('')
const form = reactive({ phone: '', password: '', captchaKey: '', captchaCode: '' })

/** 刷新验证码（点击图片也可刷新） */
async function refreshCaptcha() {
  form.captchaCode = ''
  try {
    const data = await getCaptcha()
    form.captchaKey = data.captchaKey
    captchaImage.value = data.captchaImage
  } catch {
    // 失败提示由 axios 拦截器统一处理
  }
}

async function onSubmit() {
  loading.value = true
  try {
    const data = await userStore.login({ ...form })
    ElMessage.success('登录成功')
    if (data.needChangePassword) {
      // 首登未改密 → 强制去改密页
      await router.replace(ROUTE_PATH.CHANGE_PASSWORD)
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ROUTE_PATH.HOME
    await router.replace(redirect)
  } catch {
    // 验证码是一次性的，失败后必须换一张
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 查询页「去登录」会把报名手机号带过来，省得再输一遍
  const phone = route.query.phone
  if (typeof phone === 'string') {
    form.phone = phone
  }
  refreshCaptcha()
})
</script>

<template>
  <div class="login">
    <h2 class="page-title">登录</h2>
    <p class="page-desc">手机号 + 密码 + 图形验证码</p>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="form.phone"
          name="phone"
          label="手机号"
          type="tel"
          maxlength="11"
          placeholder="请输入 11 位手机号"
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
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
        <van-field
          v-model="form.captchaCode"
          name="captchaCode"
          label="验证码"
          type="digit"
          maxlength="6"
          placeholder="计算结果"
          :rules="[{ required: true, message: '请填写验证码' }]"
        >
          <template #button>
            <div class="login__captcha-box" title="看不清？点击刷新" @click="refreshCaptcha">
              <img v-if="captchaImage" class="login__captcha" :src="captchaImage" alt="图形验证码" />
              <span v-else class="login__captcha-loading">加载中…</span>
            </div>
          </template>
        </van-field>
      </van-cell-group>

      <div class="login__submit">
        <van-button round block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>

    <!-- 公开三页平级互链（清单 §6 D109）：登录页只给「去报名 / 查审核进度」两个出口 -->
    <div class="login__links">
      <router-link to="/apply">去报名</router-link>
      <router-link to="/query">查审核进度</router-link>
    </div>

    <HealthCheckCard v-if="isDev" />
  </div>
</template>

<style scoped>
/* 验证码：固定宽度容器，图片按高度缩放，避免撑破输入行 */
.login__captcha-box {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 96px;
  overflow: hidden;
  cursor: pointer;
}

.login__captcha {
  height: 34px;
  max-width: 96px;
  border-radius: 4px;
}

.login__captcha-loading {
  font-size: 12px;
  color: #909399;
}

.login__submit {
  margin: 16px;
}

.login__links {
  display: flex;
  justify-content: space-between;
  padding: 0 4px;
  font-size: 13px;
}
</style>
