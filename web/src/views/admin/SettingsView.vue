<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, updateConfig } from '@/api/config'

/**
 * 纳新设置（仅超管，T6 新增）
 *
 * 把报名页要用到的系统配置搬到后台，避免动不动改 SQL：
 *   - 报名开关（recruit_open）
 *   - 审核时效文案（review_notice）
 *   - 社团简介（club_intro，报名页顶部的一句话；用空行分段也能正常显示）
 * 另外把短信模板与系统访问地址一并列出，方便上线前核对。
 */
const loading = ref(false)
const savingKey = ref('')
const list = ref([])
const form = reactive({})

const RECRUIT_FIELDS = [
  {
    key: 'recruit_open',
    label: '报名开关',
    type: 'switch',
    hint: '关闭后报名页只显示「本轮纳新报名已结束」；已提交的报名与审核不受影响'
  },
  {
    key: 'review_notice',
    label: '审核时效文案',
    type: 'textarea',
    rows: 3,
    hint: '显示在报名成功页，例如「我们会在 3 个工作日内完成审核，请留意查询页的状态更新」'
  },
  {
    key: 'club_intro',
    label: '社团简介',
    type: 'textarea',
    rows: 2,
    hint: '显示在报名页最顶部；建议一句话（报名页只占一行，写长了会占屏）。若确实要写长文案，用空行分段也能正常显示'
  }
]

const OTHER_FIELDS = [
  {
    key: 'system_url',
    label: '系统访问地址',
    type: 'text',
    hint: '短信模板里 {系统链接} 的取值；上线前改为公网 HTTPS 地址'
  },
  { key: 'sms_template_pass', label: '审核通过短信模板', type: 'textarea', rows: 3 },
  { key: 'sms_template_reject', label: '审核拒绝短信模板', type: 'textarea', rows: 3 }
]

const recruitOpen = computed(() => form.recruit_open === '1')

function remarkOf(key) {
  return list.value.find((item) => item.key === key)?.remark || ''
}

async function load() {
  loading.value = true
  try {
    const data = await getConfigList()
    list.value = data || []
    list.value.forEach((item) => {
      form[item.key] = item.value
    })
  } finally {
    loading.value = false
  }
}

async function save(key) {
  savingKey.value = key
  try {
    await updateConfig(key, String(form[key] ?? ''))
    ElMessage.success('保存成功')
    await load()
  } catch {
    // 提示由 axios 拦截器统一处理；重新拉取以回滚界面
    await load()
  } finally {
    savingKey.value = ''
  }
}

async function changeOpen(next) {
  const action = next ? '开启' : '关闭'
  try {
    await ElMessageBox.confirm(
      `确定${action}报名入口吗？${next ? '' : '关闭后新生将无法提交报名。'}`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  savingKey.value = 'recruit_open'
  try {
    await updateConfig('recruit_open', next ? '1' : '0')
    ElMessage.success(`已${action}报名入口`)
  } catch {
    // ignore
  } finally {
    savingKey.value = ''
    await load()
  }
}

onMounted(load)
</script>

<template>
  <div class="page settings" v-loading="loading">
    <h2 class="page-title">纳新设置</h2>
    <p class="page-desc">
      报名页相关的配置都在这里改，保存后立即生效，无需重启服务。
    </p>

    <section class="settings__block">
      <h3 class="settings__title">报名入口</h3>
      <div class="settings__row">
        <div class="settings__row-main">
          <div class="settings__label">报名开关</div>
          <div class="settings__hint">
            {{ RECRUIT_FIELDS[0].hint }}
          </div>
        </div>
        <el-switch
          :model-value="recruitOpen"
          :loading="savingKey === 'recruit_open'"
          active-text="开放中"
          inactive-text="已关闭"
          inline-prompt
          @change="changeOpen"
        />
      </div>
    </section>

    <section class="settings__block">
      <h3 class="settings__title">报名页文案</h3>
      <div v-for="field in RECRUIT_FIELDS.filter((item) => item.type !== 'switch')" :key="field.key" class="settings__field">
        <div class="settings__label">{{ field.label }}</div>
        <div class="settings__hint">{{ field.hint }}</div>
        <el-input
          v-model="form[field.key]"
          type="textarea"
          :rows="field.rows || 3"
          :autosize="{ minRows: field.rows || 3, maxRows: 20 }"
        />
        <div class="settings__actions">
          <span class="settings__remark">{{ remarkOf(field.key) }}</span>
          <el-button
            type="primary"
            size="small"
            :loading="savingKey === field.key"
            @click="save(field.key)"
          >
            保存
          </el-button>
        </div>
      </div>
    </section>

    <section class="settings__block">
      <h3 class="settings__title">短信模板与链接</h3>
      <div v-for="field in OTHER_FIELDS" :key="field.key" class="settings__field">
        <div class="settings__label">{{ field.label }}</div>
        <div v-if="field.hint" class="settings__hint">{{ field.hint }}</div>
        <el-input
          v-model="form[field.key]"
          :type="field.type === 'textarea' ? 'textarea' : 'text'"
          :rows="field.rows || 3"
          :autosize="{ minRows: field.rows || 3, maxRows: 12 }"
        />
        <div class="settings__actions">
          <span class="settings__remark">{{ remarkOf(field.key) }}</span>
          <el-button
            type="primary"
            size="small"
            :loading="savingKey === field.key"
            @click="save(field.key)"
          >
            保存
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.settings__block {
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.settings__title {
  margin: 0 0 12px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-primary);
  font-size: 14px;
  font-weight: 500;
}

.settings__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.settings__row-main {
  flex: 1;
  min-width: 0;
}

.settings__field + .settings__field {
  margin-top: 16px;
}

.settings__label {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.settings__hint {
  margin: 4px 0 8px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.settings__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.settings__remark {
  font-size: 12px;
  color: #c0c4cc;
}

@media (max-width: 768px) {
  .settings__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
