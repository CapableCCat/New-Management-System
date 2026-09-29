<script setup>
/**
 * 登录页（F-002）：手机号 + 密码 + 算术图形验证码；移动端用 Vant
 *
 * T24 改版（《V1.0 收尾需求》§3）：
 *   ① 顶部加 **Logo** —— 用 `web/public/Logo ZH.svg`（实测带透明通道：86.5% 全透明、四角 alpha=0，
 *      放深色渐变上不会出白框）。用**静态资源**而非 T20 那套「后台上传」—— 登录页不是"内容"，
 *      它就是品牌门面，不该由运营改；报名页那个 Logo 才是可换的（§九 Q1 定案）。
 *   ② **表单卡片垂直水平居中**
 *   ③ **全屏背景** —— 通过 `PublicLayout` 的 `bare` 开关（路由 `meta.bare`）去掉页头页脚与限宽。
 *      背景值集中在 `styles/index.scss` 的 `--login-bg-image` / `--login-bg-gradient`：
 *      **素材到位后只改那一行**就能换成真背景图，本文件不用动。
 *   ④ 「后端联通性检查卡」**本来就只在 dev 显示**（`v-if="isDev"`），prod 不渲染 —— 核对后无需改动。
 */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { ROUTE_PATH } from '@/constants/app'
import { useUserStore } from '@/stores/user'

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
    <div class="login__card">
      <!-- 品牌区：Logo + 名称（按社长反馈去掉「手机号 + 密码 + 图形验证码」副标题 —— 表单本身就是说明） -->
      <div class="login__brand">
        <img class="login__logo" src="/Logo%20ZH.svg" alt="天津中德开源鸿蒙社" />
        <h1 class="login__title">社团管理系统</h1>
      </div>

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
    </div>

    <!-- 后端联通性检查：**仅 dev** 显示（prod 不渲染）—— T24 核对过，无需改动 -->
    <HealthCheckCard v-if="isDev" />
  </div>
</template>

<style scoped>
/*
 * 全屏背景 + 卡片居中。
 * 背景值来自 styles/index.scss 的 token：--login-bg-image（素材到位后换这张图）/ --login-bg-gradient。
 * 两层顺序不能反：图在上、渐变在下 —— 这样图没配（none）时自然只剩渐变。
 */
.login {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  min-height: 100vh;
  padding: var(--space-6) var(--space-4);
  background-image: var(--login-bg-image), var(--login-bg-gradient);
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.login__card {
  width: 100%;
  max-width: 380px;
  padding: var(--space-6) 0 var(--space-4);
  background: var(--bg-surface);
  border-radius: var(--radius-surface);
  box-shadow: var(--shadow-popup);
}

.login__brand {
  padding: 0 var(--space-4) var(--space-4);
  text-align: center;
}

.login__logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
}

.login__title {
  margin: var(--space-2) 0 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

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
  color: var(--text-secondary);
}

.login__submit {
  margin: 16px;
}

.login__links {
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
  font-size: 13px;
}

@media (max-width: 480px) {
  .login {
    /* 手机上不要固定背景，避免部分浏览器滚动时的重绘抖动 */
    background-attachment: scroll;
  }

  .login__card {
    border-radius: var(--radius-popup);
  }
}
</style>
