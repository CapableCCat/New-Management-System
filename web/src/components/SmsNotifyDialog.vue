<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useIsMobile } from '@/composables/useIsMobile'
import { buildSmsLink, copyText, isMobileBrowser, joinPhones, renderTemplate } from '@/utils/sms'

/**
 * 短信通知弹窗（PRD F-004）—— 审核台「通过 / 拒绝 / 批量发送」共用。
 *
 * 设计要点：
 *   - 单个目标：直接给出一条精确渲染的短信，可当场编辑（只影响本次复制，不回写模板）
 *   - 多个目标：主区是**统一话术**（群发用），另提供「逐条短信」折叠区，
 *     每人一条精确内容（含姓名 / 初始密码 / 拒绝原因），可逐条复制
 *   - 移动端单目标提供 sms: 唤起；多目标不做唤起（各端对多收件人支持不一致）
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 通知对象：{ name, phone, status, password?, rejectReason? } */
  targets: { type: Array, default: () => [] },
  /** { systemUrl, passTemplate, rejectTemplate } */
  config: { type: Object, default: () => ({}) },
  title: { type: String, default: '短信通知' }
})

const emit = defineEmits(['update:modelValue'])

/** 移动端要把弹窗收窄，否则固定 620px 会在窄屏横向溢出、footer 按钮被切掉 */
const isMobile = useIsMobile()

const unifiedText = ref('')
const perTexts = ref([])

const list = computed(() => props.targets || [])
const isSingle = computed(() => list.value.length === 1)
const isMulti = computed(() => list.value.length > 1)
const phoneList = computed(() => joinPhones(list.value.map((item) => item.phone)))
const mixedStatus = computed(() => new Set(list.value.map((item) => item.status)).size > 1)
const canDeeplink = computed(() => isMobileBrowser() && isSingle.value)
const templateMissing = computed(() => {
  const status = list.value[0]?.status
  return status === 2 ? !props.config?.rejectTemplate : !props.config?.passTemplate
})

const STATUS_LABEL = { 0: '待审', 1: '通过', 2: '拒绝' }

/** 单个目标的精确变量 */
function varsOf(target) {
  const vars = { 姓名: target.name, 系统链接: props.config?.systemUrl || '' }
  if (target.status === 2) {
    vars['拒绝原因'] = target.rejectReason || '（未填写原因，可联系社长团了解）'
  } else {
    vars['初始密码'] = target.password || '（初始密码见创建时的密码清单）'
  }
  return vars
}

function templateOf(target) {
  return target.status === 2 ? props.config?.rejectTemplate : props.config?.passTemplate
}

/**
 * 群发用的通用话术：逐人变量换成通用措辞。
 *
 * <p>姓名留空 —— 模板本身通常写成「{姓名}同学你好」，填「同学」会叠成「同学同学你好」。
 */
function unifiedVars() {
  return {
    姓名: '',
    系统链接: props.config?.systemUrl || '',
    初始密码: '（初始密码见创建时记录的清单）',
    拒绝原因: '（详见审核记录）'
  }
}

function buildAll() {
  if (!list.value.length) {
    unifiedText.value = ''
    perTexts.value = []
    return
  }
  perTexts.value = list.value.map((target) => renderTemplate(templateOf(target), varsOf(target)))
  unifiedText.value = isSingle.value
    ? perTexts.value[0]
    : renderTemplate(templateOf(list.value[0]), unifiedVars())
}

watch(
  () => [props.modelValue, props.targets, props.config],
  () => {
    if (props.modelValue) {
      buildAll()
    }
  },
  { immediate: true, deep: true }
)

async function copy(label, text) {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  const ok = await copyText(text)
  if (ok) {
    ElMessage.success(label)
  } else {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

/** 唤起系统短信 App（仅移动端单目标） */
function openSmsApp() {
  const link = buildSmsLink(list.value.map((item) => item.phone), unifiedText.value)
  if (link) {
    window.location.href = link
  }
}

/** 逐条短信合并成一份文本（批量通过时逐条粘贴用） */
function perTextsJoined() {
  return perTexts.value
    .map((text, index) => `${list.value[index].name} ${list.value[index].phone}\n${text}`)
    .join('\n\n')
}

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="isMobile ? '92%' : '620px'"
    class="sms-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert v-if="templateMissing" type="warning" :closable="false" show-icon class="sms__alert">
      短信模板为空，请先由超管到「纳新设置 → 短信模板与链接」填写。
    </el-alert>
    <el-alert v-if="isMulti && mixedStatus" type="info" :closable="false" show-icon class="sms__alert">
      所选记录状态不一致，统一话术按第一条的状态取模板；需要精确内容请用下方「逐条短信」。
    </el-alert>

    <div class="sms__block">
      <div class="sms__label">
        收件人（{{ list.length }} 人）
        <el-button link type="primary" size="small" @click="copy('已复制手机号', phoneList)">
          复制手机号
        </el-button>
      </div>
      <div class="sms__phones">{{ phoneList || '—' }}</div>
    </div>

    <div class="sms__block">
      <div class="sms__label">
        {{ isMulti ? '统一话术（群发用，可编辑）' : '短信内容（可编辑）' }}
        <span class="sms__tip">编辑只影响本次复制，不会改回模板</span>
      </div>
      <el-input v-model="unifiedText" type="textarea" :autosize="{ minRows: 3, maxRows: 10 }" />
      <p v-if="isMulti" class="sms__note">
        群发话术不含每人的姓名 / 初始密码 / 拒绝原因；需要精确内容请展开下方「逐条短信」。
      </p>
    </div>

    <div v-if="isMulti" class="sms__block">
      <el-collapse>
        <el-collapse-item :title="`逐条短信（每人一条，含姓名等信息）`" name="each">
          <div v-for="(item, index) in list" :key="item.phone" class="sms__item">
            <div class="sms__item-head">
              <span class="sms__item-name">
                {{ item.name }} · {{ item.phone }}
                <el-tag :type="item.status === 2 ? 'danger' : 'success'" size="small">
                  {{ STATUS_LABEL[item.status] }}
                </el-tag>
              </span>
              <el-button
                link
                type="primary"
                size="small"
                @click="copy('已复制该条短信', perTexts[index])"
              >
                复制
              </el-button>
            </div>
            <el-input
              v-model="perTexts[index]"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
            />
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <template #footer>
      <el-button v-if="canDeeplink" type="success" plain @click="openSmsApp">唤起短信</el-button>
      <el-button v-if="isMulti" @click="copy('已复制逐条短信', perTextsJoined())">
        复制逐条短信
      </el-button>
      <el-button @click="copy('已复制话术+号码', `${unifiedText}\n\n收件人：${phoneList}`)">
        复制话术+号码
      </el-button>
      <el-button type="primary" @click="copy('已复制话术', unifiedText)">复制话术</el-button>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.sms__alert {
  margin-bottom: 10px;
}

.sms__block + .sms__block {
  margin-top: 14px;
}

.sms__label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.sms__tip {
  font-size: 12px;
  font-weight: 400;
  color: #c0c4cc;
}

.sms__note {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.sms__phones {
  padding: 8px 10px;
  border-radius: 4px;
  background: #f7f8fa;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
  word-break: break-all;
}

.sms__item + .sms__item {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.sms__item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.sms__item-name {
  font-size: 13px;
  color: #606266;
}

@media (max-width: 768px) {
  /* 窄屏下标签与提示分两行，避免「（可编辑）编辑只影响…」挤在一起换行断开 */
  .sms__label {
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }
}
</style>
