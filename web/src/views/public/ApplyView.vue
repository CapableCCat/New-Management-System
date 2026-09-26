<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { areaList } from '@vant/area-data'
import { getCaptcha } from '@/api/auth'
import { submitApply } from '@/api/recruit'
import { useDictStore } from '@/stores/dict'
import { useRecruitStore } from '@/stores/recruit'

/**
 * 公开报名页（F-001）—— 纳新主链起点，移动优先
 *
 * 规则：
 *   - 只写 recruit_apply，不建账号（审核通过后由 T7 建号）
 *   - 手机号四种结果都由后端 `200 + nextAction` 下发（见清单 §6 D111）：
 *     新提交 / 被拒后重提 = 成功；仍在待审 / 已是成员 = **引导**（不是错误，不弹红色报错）
 *   - 兴趣标签按 5 大类分组展示，最多选 3 个；选「其他」出现自由填写框
 *   - 草稿存 localStorage，断网/误刷新不丢（PRD 要求保留已填内容）
 *   - 顶部只放**一句话介绍**（PRD F-001 第 1 步「一句话介绍」；文案存 `sys_config.club_intro`，后台可改）——
 *     2026-09-26 由长篇简介精简为一句；后台若改回多段，仍按空行分段渲染
 *   - 公开页不做导航栏：页与页平级互链，底部只给「已有账号？去登录」（清单 §6 D109）
 */
const router = useRouter()
const dictStore = useDictStore()
const recruitStore = useRecruitStore()

const DRAFT_KEY = 'osc_apply_draft'
const MAX_TAGS = 3

const loadingInfo = ref(true)
const saving = ref(false)
const submitted = ref(false)
const result = ref(null)
const info = reactive({ open: true, clubIntro: '', reviewNotice: '' })
const captchaImage = ref('')
const privacyAgreed = ref(false)
const pickerShow = ref(false)
const pickerKind = ref('college')
const pickerValue = ref([])
const areaShow = ref(false)

const form = reactive({
  name: '',
  phone: '',
  college: '',
  major: '',
  majorText: '',
  intentDepartments: [],
  tags: [],
  tagText: '',
  gender: 0,
  province: '',
  city: '',
  captchaKey: '',
  captchaCode: ''
})

const departments = computed(() => dictStore.cache.department || [])
const allTags = computed(() => dictStore.cache.tag || [])

/** 标签按 remark 里的分类名分组（分类名由 T5 的字典种子写入） */
const tagGroups = computed(() => {
  const groups = new Map()
  allTags.value.forEach((item) => {
    const name = item.remark || '其他'
    if (!groups.has(name)) {
      groups.set(name, [])
    }
    groups.get(name).push(item)
  })
  return [...groups.entries()].map(([name, items]) => ({ name, items }))
})

/** 简介按空行分段 */
const introParagraphs = computed(() =>
  (info.clubIntro || '')
    .split(/\n\s*\n/)
    .map((text) => text.trim())
    .filter(Boolean)
)

/** 结果态的下一步引导（后端 nextAction，见清单 §6 D111 / D109） */
const NEXT_LOGIN = 'LOGIN'

/**
 * 结果卡内容：**结果态不是独立页**，只是报名页的一个状态（清单 §6 D109）。
 *
 *   - 新提交 / 被拒后重提 / 仍在待审 → 去查询（+ 「已有账号？去登录」出口）
 *   - 已是正式成员 → 去登录（+ 去查询出口）
 */
const resultCard = computed(() => {
  const phone = result.value?.phone || form.phone
  const message = result.value?.message || ''

  if (result.value?.nextAction === NEXT_LOGIN) {
    return {
      icon: 'friends-o',
      color: '#409eff',
      title: '该手机号已是正式成员',
      lines: [message],
      primary: { label: '去登录', to: { path: '/login', query: { phone } } },
      secondary: { label: '去查询审核状态', to: { path: '/query', query: { phone } } }
    }
  }

  const titleByState = {
    SUBMITTED: '报名提交成功',
    RESUBMITTED: '已重新提交',
    ALREADY_PENDING: '你已提交过报名'
  }
  const alreadyPending = result.value?.state === 'ALREADY_PENDING'
  const lines = [`我们已收到你用手机号 ${phone} 提交的报名信息。`]
  if (message) {
    lines.push(message)
  }
  if (info.reviewNotice) {
    lines.push(info.reviewNotice)
  }
  return {
    icon: alreadyPending ? 'info-o' : 'passed',
    color: alreadyPending ? '#e6a23c' : '#67c23a',
    title: titleByState[result.value?.state] || '报名提交成功',
    lines,
    primary: { label: '去查询审核进度', to: { path: '/query', query: { phone } } },
    secondary: { label: '已有账号？去登录', to: { path: '/login', query: { phone } } }
  }
})

const isOtherMajor = computed(() => form.major === 'other')
const isOtherTag = computed(() => form.tags.includes('other'))
const collegeText = computed(() => (form.college ? dictStore.labelOf('college', form.college) : ''))
const majorText = computed(() => {
  if (!form.major) {
    return ''
  }
  if (isOtherMajor.value) {
    return form.majorText ? `其他（${form.majorText}）` : '其他'
  }
  return dictStore.labelOf('major', form.major)
})
const pickerTitle = computed(() => (pickerKind.value === 'college' ? '选择学院' : '选择专业'))
const pickerColumns = computed(() =>
  (pickerKind.value === 'college' ? dictStore.cache.college || [] : dictStore.cache.major || []).map(
    (item) => ({ text: item.label, value: item.code })
  )
)

function openPicker(kind) {
  pickerKind.value = kind
  const current = kind === 'college' ? form.college : form.major
  pickerValue.value = current ? [current] : [pickerColumns.value[0]?.value]
  pickerShow.value = true
}

function onPickerConfirm({ selectedValues }) {
  const code = selectedValues?.[0]
  if (pickerKind.value === 'college') {
    form.college = code
  } else {
    form.major = code
    if (code !== 'other') {
      form.majorText = ''
    }
  }
  pickerShow.value = false
}

function onAreaConfirm({ selectedOptions }) {
  form.province = selectedOptions?.[0]?.name || ''
  form.city = selectedOptions?.[1]?.name || ''
  areaShow.value = false
}

function toggleDepartment(code) {
  const index = form.intentDepartments.indexOf(code)
  if (index >= 0) {
    form.intentDepartments.splice(index, 1)
  } else {
    form.intentDepartments.push(code)
  }
}

function toggleTag(code) {
  const index = form.tags.indexOf(code)
  if (index >= 0) {
    form.tags.splice(index, 1)
    if (code === 'other') {
      form.tagText = ''
    }
    return
  }
  if (form.tags.length >= MAX_TAGS) {
    ElMessage.warning(`兴趣标签最多选 ${MAX_TAGS} 个`)
    return
  }
  form.tags.push(code)
}

/** 刷新验证码（一次性，提交失败必须换一张） */
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

function saveDraft() {
  if (submitted.value) {
    return
  }
  try {
    const draft = { ...form }
    // 验证码是一次性的，不进草稿
    delete draft.captchaKey
    delete draft.captchaCode
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
  } catch {
    // 忽略：隐私模式下 localStorage 可能不可用
  }
}

function restoreDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) {
      return
    }
    const draft = JSON.parse(raw)
    Object.keys(draft).forEach((key) => {
      if (key in form) {
        form[key] = draft[key]
      }
    })
  } catch {
    // 草稿损坏时忽略
  }
}

function clearDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY)
  } catch {
    // ignore
  }
}

async function onSubmit() {
  if (!form.college) {
    ElMessage.warning('请选择学院')
    return
  }
  if (!form.major) {
    ElMessage.warning('请选择专业')
    return
  }
  if (isOtherMajor.value && !form.majorText.trim()) {
    ElMessage.warning('请填写具体专业名称')
    return
  }
  if (!form.intentDepartments.length) {
    ElMessage.warning('请至少选择一个意向部门')
    return
  }
  if (!privacyAgreed.value) {
    ElMessage.warning('请先阅读并同意信息使用说明')
    return
  }

  saving.value = true
  try {
    const data = await submitApply({
      name: form.name.trim(),
      phone: form.phone.trim(),
      college: form.college,
      major: form.major,
      majorText: isOtherMajor.value ? form.majorText.trim() : '',
      intentDepartments: [...form.intentDepartments],
      tags: [...form.tags],
      tagText: isOtherTag.value ? form.tagText.trim() : '',
      gender: form.gender,
      province: form.province,
      city: form.city,
      captchaKey: form.captchaKey,
      captchaCode: form.captchaCode
    })
    result.value = data
    submitted.value = true
    clearDraft()
  } catch {
    // 验证码一次性：失败必须换一张
    refreshCaptcha()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  // 配置走 store 缓存（路由守卫判定落地页时可能已经取过，这里不重复请求）
  const data = await recruitStore.loadInfo()
  if (data) {
    Object.assign(info, data)
  } else {
    info.open = false
  }
  loadingInfo.value = false

  if (info.open) {
    restoreDraft()
    await dictStore.loadMany(['college', 'major', 'department', 'tag'])
    refreshCaptcha()
  }
})

// 草稿自动暂存（captcha 字段在 saveDraft 里被排除）
watch(form, saveDraft, { deep: true })
</script>

<template>
  <div class="apply">
    <van-empty v-if="loadingInfo" description="加载中…" />
    <template v-else-if="!info.open">
      <van-empty image="error" description="本轮纳新报名已结束" />
      <div class="apply__links">
        <router-link to="/login">已有账号？去登录</router-link>
        <router-link to="/query">查询审核状态</router-link>
      </div>
    </template>

    <!-- 提交结果（**不是独立页**，只是报名页的一个状态；两个出口：去查询 / 已有账号去登录） -->
    <div v-else-if="submitted" class="apply__success">
      <van-icon
        :name="resultCard.icon"
        class="apply__success-icon"
        :style="{ color: resultCard.color }"
      />
      <p class="apply__success-title">{{ resultCard.title }}</p>
      <p v-for="(line, index) in resultCard.lines" :key="index" class="apply__success-text">
        {{ line }}
      </p>
      <div class="apply__success-actions">
        <van-button round block type="primary" @click="router.push(resultCard.primary.to)">
          {{ resultCard.primary.label }}
        </van-button>
        <van-button round block plain @click="router.push(resultCard.secondary.to)">
          {{ resultCard.secondary.label }}
        </van-button>
      </div>
    </div>

    <!-- 报名表单 -->
    <template v-else>
      <section v-if="introParagraphs.length" class="apply__intro">
        <p v-for="(text, index) in introParagraphs" :key="index" class="apply__intro-p">
          {{ text }}
        </p>
      </section>

      <van-form @submit="onSubmit">
        <van-cell-group inset title="基本信息">
          <van-field
            v-model="form.name"
            name="name"
            label="姓名"
            maxlength="32"
            placeholder="请输入真实姓名"
            :rules="[{ required: true, message: '请填写姓名' }]"
          />
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            type="tel"
            maxlength="11"
            placeholder="用于接收审核结果"
            :rules="[
              { required: true, message: '请填写手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ]"
          />
          <van-field
            :model-value="collegeText"
            label="学院"
            readonly
            is-link
            placeholder="请选择学院"
            @click="openPicker('college')"
          />
          <van-field
            :model-value="majorText"
            label="专业"
            readonly
            is-link
            placeholder="请选择专业"
            @click="openPicker('major')"
          />
          <van-field
            v-if="isOtherMajor"
            v-model="form.majorText"
            label="专业名称"
            maxlength="64"
            placeholder="请填写你的具体专业"
            :rules="[{ required: true, message: '请填写具体专业名称' }]"
          />
          <van-field name="gender" label="性别">
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio :name="1">男</van-radio>
                <van-radio :name="2">女</van-radio>
                <van-radio :name="0">不填</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field
            :model-value="form.province ? `${form.province} ${form.city}` : ''"
            label="生源地"
            readonly
            is-link
            placeholder="选填，点击选择"
            @click="areaShow = true"
          />
        </van-cell-group>

        <van-cell-group inset title="意向部门（至少选 1 个）">
          <div class="apply__chips">
            <div
              v-for="item in departments"
              :key="item.code"
              class="apply__chip"
              :class="{ 'is-active': form.intentDepartments.includes(item.code) }"
              @click="toggleDepartment(item.code)"
            >
              {{ item.label }}
            </div>
          </div>
        </van-cell-group>

        <van-cell-group inset :title="`兴趣标签（选填，最多 ${MAX_TAGS} 个）`">
          <div v-for="group in tagGroups" :key="group.name" class="apply__group">
            <p class="apply__group-name">{{ group.name }}</p>
            <div class="apply__chips">
              <div
                v-for="item in group.items"
                :key="item.code"
                class="apply__chip"
                :class="{ 'is-active': form.tags.includes(item.code) }"
                @click="toggleTag(item.code)"
              >
                {{ item.label }}
              </div>
            </div>
          </div>
          <van-field
            v-if="isOtherTag"
            v-model="form.tagText"
            label="补充标签"
            maxlength="64"
            placeholder="想写上自己的兴趣方向"
          />
        </van-cell-group>

        <van-cell-group inset title="验证与提交">
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
              <div class="apply__captcha-box" title="看不清？点击刷新" @click="refreshCaptcha">
                <img
                  v-if="captchaImage"
                  class="apply__captcha"
                  :src="captchaImage"
                  alt="图形验证码"
                />
                <span v-else class="apply__captcha-loading">加载中…</span>
              </div>
            </template>
          </van-field>
          <div class="apply__privacy">
            <van-checkbox v-model="privacyAgreed" shape="square">
              我同意将以上信息用于开源鸿蒙社纳新审核与后续社团联络，不作其他用途
            </van-checkbox>
          </div>
        </van-cell-group>

        <div class="apply__submit">
          <van-button round block type="primary" native-type="submit" :loading="saving">
            提交报名
          </van-button>
        </div>
      </van-form>

      <div class="apply__links">
        <router-link to="/login">已有账号？去登录</router-link>
        <router-link to="/query">查询审核状态</router-link>
      </div>
    </template>

    <van-popup v-model:show="pickerShow" position="bottom" round>
      <van-picker
        v-model="pickerValue"
        :title="pickerTitle"
        :columns="pickerColumns"
        @confirm="onPickerConfirm"
        @cancel="pickerShow = false"
      />
    </van-popup>

    <van-popup v-model:show="areaShow" position="bottom" round>
      <van-area
        :area-list="areaList"
        title="选择生源地"
        :columns-num="2"
        @confirm="onAreaConfirm"
        @cancel="areaShow = false"
      />
    </van-popup>
  </div>
</template>

<style scoped>
.apply {
  padding: 12px 0 24px;
}

/* 顶部一句话介绍：轻量一行，不占屏（长篇简介已精简；后台改回多段也能正常分段渲染） */
.apply__intro {
  margin: 4px 12px 14px;
  padding: 10px 14px;
  border-left: 3px solid var(--brand-primary);
  border-radius: 6px;
  background: color-mix(in srgb, var(--brand-primary) 6%, #fff);
}

.apply__intro-p {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #4b5563;
}

.apply__intro-p:last-child {
  margin-bottom: 0;
}

.apply__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
}

.apply__chip {
  padding: 6px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 16px;
  font-size: 13px;
  color: #606266;
  background: #fff;
}

.apply__chip.is-active {
  border-color: var(--brand-primary);
  background: color-mix(in srgb, var(--brand-primary) 12%, #fff);
  color: var(--brand-primary);
}

.apply__group {
  border-top: 1px solid #f2f3f5;
}

.apply__group:first-of-type {
  border-top: none;
}

.apply__group-name {
  margin: 10px 16px -4px;
  font-size: 12px;
  color: #909399;
}

.apply__captcha-box {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 96px;
  overflow: hidden;
  cursor: pointer;
}

.apply__captcha {
  height: 34px;
  max-width: 96px;
  border-radius: 4px;
}

.apply__captcha-loading {
  font-size: 12px;
  color: #909399;
}

.apply__privacy {
  padding: 12px 16px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.apply__submit {
  margin: 20px 16px 0;
}

.apply__success {
  padding: 40px 20px;
  text-align: center;
}

.apply__success-icon {
  font-size: 56px;
  color: #67c23a;
}

.apply__success-title {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 500;
}

.apply__success-text {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
}

/* 公开页平级互链（清单 §6 D109）：各页只给出口，不做全局导航栏 */
.apply__links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin: 16px 16px 0;
  font-size: 13px;
}

.apply__links a {
  color: var(--brand-primary);
  text-decoration: none;
}

.apply__success-actions {
  margin-top: 20px;
  padding: 0 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
