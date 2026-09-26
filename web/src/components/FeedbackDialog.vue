<script setup>
/**
 * 反馈提交弹窗（PRD F-014）—— 三处入口共用（T16）
 *
 * 特点：
 *   - **免登录可提交**：报名结果态里新生还没有账号，所以后端 `/feedback/submit` 在白名单内；
 *     代价是**必须带一次性图形验证码**（这里负责取图与刷新）。
 *   - 文案前端自持：axios 拦截器只脱壳 `body.data`，读不到后端的 `message`（清单 §6 D115）。
 *   - UI 用 Element Plus（全站主库），宽度在小屏自适应，公开页里也不会溢出。
 */
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { submitFeedback } from '@/api/feedback'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 来源（字典 feedback_source：1 报名成功页 / 2 成员端） */
  source: { type: Number, required: true }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const MAX_CONTENT = 1000

const formRef = ref(null)
const saving = ref(false)
const captchaImage = ref('')

const form = reactive({
  content: '',
  contact: '',
  captchaKey: '',
  captchaCode: ''
})

const rules = {
  content: [{ required: true, message: '请填写反馈内容', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请填写验证码', trigger: 'blur' }]
}

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

function reset() {
  form.content = ''
  form.contact = ''
  form.captchaCode = ''
  formRef.value?.clearValidate()
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await submitFeedback({
      content: form.content.trim(),
      contact: form.contact.trim(),
      source: props.source,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    ElMessage.success('感谢反馈，我们会尽快查看')
    visible.value = false
    reset()
  } catch {
    // 验证码错误 / 内容不合法等：换一张验证码重来
    refreshCaptcha()
  } finally {
    saving.value = false
  }
}

// 每次打开都重置并取新验证码（验证码是一次性的，不能复用上次那张）
watch(visible, (open) => {
  if (open) {
    reset()
    refreshCaptcha()
  }
})
</script>

<template>
  <el-dialog
    v-model="visible"
    title="意见反馈"
    width="min(92vw, 460px)"
    :close-on-click-modal="false"
    append-to-body
  >
    <p class="feedback-tip">
      用着不顺手、想看什么功能、哪里写错了，都可以直接说 —— 不用登录，也不会耽误你报名。
    </p>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="反馈内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          :maxlength="MAX_CONTENT"
          show-word-limit
          placeholder="例如：报名页在手机上选专业时有点卡 / 希望公告能按部门筛选"
        />
      </el-form-item>

      <el-form-item label="联系方式（选填）">
        <el-input
          v-model="form.contact"
          maxlength="64"
          placeholder="手机号 / 微信 / 邮箱，方便我们回复你"
        />
      </el-form-item>

      <el-form-item label="验证码" prop="captchaCode">
        <div class="feedback-captcha">
          <el-input v-model="form.captchaCode" maxlength="8" placeholder="请输入计算结果" />
          <div class="feedback-captcha__box" title="看不清？点击换一张" @click="refreshCaptcha">
            <img v-if="captchaImage" class="feedback-captcha__img" :src="captchaImage" alt="验证码" />
            <span v-else class="feedback-captcha__ph">点击刷新</span>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSubmit">提交反馈</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.feedback-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #909399;
}

.feedback-captcha {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.feedback-captcha__box {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 34px;
  border: 1px solid #dcdfe6;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
  cursor: pointer;
  overflow: hidden;
}

.feedback-captcha__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.feedback-captcha__ph {
  font-size: 12px;
  color: #909399;
}
</style>
