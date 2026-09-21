<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { getRecruitInfo, queryApplyStatus } from '@/api/recruit'

/**
 * 审核状态查询页（F-005）—— 公开访问，移动优先
 *
 * 规则：
 *   - 凭手机号 + 一次性图形验证码查询（防脚本批量探测手机号）
 *   - 只展示状态与拒绝原因，不显示姓名 / 学院等其他信息
 *   - 不受报名开关影响：报名已结束也要能查进度
 *   - 手机号从 route.query.phone 预填（报名成功页的「去查询审核状态」带过来）
 */
const route = useRoute()
const router = useRouter()

/** 报名状态枚举（recruit_apply 自己的，与字典「账号状态」无关，见 D58） */
const STATUS = { PENDING: 0, APPROVED: 1, REJECTED: 2 }

const loading = ref(false)
const queried = ref(false)
const found = ref(false)
const result = ref(null)
const captchaImage = ref('')
const info = reactive({ reviewNotice: '' })
const form = reactive({ phone: '', captchaKey: '', captchaCode: '' })

/** 结果卡片文案：待审 / 已通过 / 已拒绝三种 */
const statusView = computed(() => {
  if (!found.value) {
    return null
  }
  if (result.value?.status === STATUS.APPROVED) {
    return {
      icon: 'passed',
      color: '#67c23a',
      title: '恭喜！你的报名已通过审核',
      text: '请前往登录页，用报名手机号和初始密码激活账号。'
    }
  }
  if (result.value?.status === STATUS.REJECTED) {
    return {
      icon: 'warning-o',
      color: '#f56c6c',
      title: '很抱歉，你的报名未通过审核',
      text: `原因：${result.value?.rejectReason || '管理员未填写原因，可联系社长团了解'}`
    }
  }
  return {
    icon: 'clock-o',
    color: '#e6a23c',
    title: '你的报名正在审核中，请耐心等待',
    text: info.reviewNotice || '审核完成后可再次查询结果。'
  }
})

/** 刷新验证码（一次性，每次查询后必须换一张） */
async function refreshCaptcha() {
  form.captchaCode = ''
  try {
    const data = await getCaptcha()
    form.captchaKey = data.captchaKey
    captchaImage.value = data.captchaImage
  } catch {
    // 提示由 axios 拦截器统一处理
  }
}

async function onQuery() {
  const phone = form.phone.trim()
  if (!phone) {
    ElMessage.warning('请填写手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (!form.captchaCode.trim()) {
    ElMessage.warning('请填写验证码')
    return
  }

  loading.value = true
  try {
    const data = await queryApplyStatus({
      phone,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    result.value = data
    found.value = Boolean(data)
    queried.value = true
  } catch {
    // 验证码一次性：失败也要换一张（finally 里统一刷新）
  } finally {
    loading.value = false
    refreshCaptcha()
  }
}

/** 回到查询表单再查一次 */
function reset() {
  queried.value = false
  found.value = false
  result.value = null
}

onMounted(async () => {
  const phone = route.query.phone
  if (typeof phone === 'string') {
    form.phone = phone
  }
  try {
    const data = await getRecruitInfo()
    info.reviewNotice = data.reviewNotice || ''
  } catch {
    // 拿不到审核时效文案不影响查询
  }
  refreshCaptcha()
})
</script>

<template>
  <div class="query">
    <!-- 查询表单 -->
    <template v-if="!queried">
      <section class="query__hero">
        <p class="query__hero-title">查询审核状态</p>
        <p class="query__hero-desc">输入报名时使用的手机号，即可查看纳新审核进度。</p>
      </section>

      <van-form @submit="onQuery">
        <van-cell-group inset>
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            type="tel"
            maxlength="11"
            placeholder="报名时填写的手机号"
            :rules="[
              { required: true, message: '请填写手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ]"
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
              <div class="query__captcha-box" title="看不清？点击刷新" @click="refreshCaptcha">
                <img
                  v-if="captchaImage"
                  class="query__captcha"
                  :src="captchaImage"
                  alt="图形验证码"
                />
                <span v-else class="query__captcha-loading">加载中…</span>
              </div>
            </template>
          </van-field>
        </van-cell-group>

        <div class="query__submit">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            查询
          </van-button>
        </div>
        <p class="query__tip">查询需填写图形验证码，用于防止他人批量试探手机号。</p>
      </van-form>
    </template>

    <!-- 查询结果 -->
    <div v-else class="query__result">
      <van-icon
        :name="found ? statusView.icon : 'search'"
        class="query__result-icon"
        :style="{ color: found ? statusView.color : '#909399' }"
      />
      <p class="query__result-title">
        {{ found ? statusView.title : '未找到该手机号的报名记录' }}
      </p>
      <p class="query__result-text">
        {{ found ? statusView.text : '请确认手机号是否与报名时填写的一致。' }}
      </p>

      <div class="query__result-actions">
        <van-button
          v-if="found && result.status === STATUS.APPROVED"
          round
          block
          type="primary"
          @click="router.push('/login')"
        >
          去登录激活账号
        </van-button>
        <van-button v-else-if="!found" round block type="primary" @click="router.push('/apply')">
          去报名
        </van-button>
        <van-button round block plain @click="reset">再查一次</van-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.query {
  padding: 12px 0 24px;
}

.query__hero {
  margin: 4px 16px 16px;
}

.query__hero-title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.query__hero-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #909399;
}

.query__captcha-box {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 96px;
  overflow: hidden;
  cursor: pointer;
}

.query__captcha {
  height: 34px;
  max-width: 96px;
  border-radius: 4px;
}

.query__captcha-loading {
  font-size: 12px;
  color: #909399;
}

.query__submit {
  margin: 20px 16px 0;
}

.query__tip {
  margin: 12px 16px 0;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.query__result {
  padding: 32px 20px;
  text-align: center;
}

.query__result-icon {
  font-size: 56px;
}

.query__result-title {
  margin: 12px 0 8px;
  font-size: 17px;
  font-weight: 500;
  color: #303133;
}

.query__result-text {
  margin: 0 0 24px;
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
}

.query__result-actions {
  padding: 0 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
