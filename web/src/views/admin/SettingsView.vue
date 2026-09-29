<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, removeClubLogo, updateConfig, uploadClubLogo } from '@/api/config'
import { getRecruitInfo } from '@/api/recruit'
import { cleanStorageOrphans, scanStorageOrphans } from '@/api/storage'
import { isSuperAdmin } from '@/constants/roles'
import { useUserStore } from '@/stores/user'

/**
 * 纳新设置（仅超管，T6 新增；T20/O3 加 Logo 上传）
 *
 * 把报名页要用到的系统配置搬到后台，避免动不动改 SQL：
 *   - 报名开关（recruit_open）
 *   - 审核时效文案（review_notice）
 *   - 社团简介（club_intro，报名页顶部的一句话；用空行分段也能正常显示）
 *   - 社团 Logo（club_logo）—— **走上传接口，不是文本框**：
 *     该键在库里存的是对象 key（`club/logo_xxx.png`），由后端专用端点写入，
 *     所以它不在 EDITABLE_KEYS 白名单里（否则文本框一改就指向不存在的对象）。
 * 另外把短信模板与系统访问地址一并列出，方便上线前核对。
 */
const loading = ref(false)
const savingKey = ref('')
const list = ref([])
const form = reactive({})

const userStore = useUserStore()

/**
 * 存储维护（T26）**仅超管可见**。
 *
 * ⚠️ 为什么在本页还要单独判一次：本页路由的能力是 `canManageConfig`，
 * **T27 会把它放宽给社长团** —— 而"清理对象存储"是会真删东西的运维动作，必须留在超管手里。
 * 所以这里显式用 `isSuperAdmin` 再拦一道，不依赖页面级权限。
 */
const showStorageTool = computed(() => isSuperAdmin(userStore.profile))

/** 存储维护状态 */
const orphanScanning = ref(false)
const orphanCleaning = ref(false)
const scanResult = ref(null)
const selectedKeys = ref([])

async function scanOrphans() {
  orphanScanning.value = true
  try {
    scanResult.value = await scanStorageOrphans()
    selectedKeys.value = []
    if (!scanResult.value?.orphans?.length) {
      ElMessage.success('扫描完成，没有发现孤儿文件')
    }
  } catch {
    // 提示由 axios 拦截器统一处理
  } finally {
    orphanScanning.value = false
  }
}

async function cleanOrphans() {
  const keys = selectedKeys.value
  if (!keys.length) {
    ElMessage.warning('请先勾选要清理的对象')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${keys.length} 个对象吗？此操作不可撤销（对象存储里的文件会被真删）。`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
  } catch {
    return
  }
  orphanCleaning.value = true
  try {
    const result = await cleanStorageOrphans(keys)
    ElMessage.success(
      `已清理 ${result.deleted} 个${result.skipped ? `，跳过 ${result.skipped} 个` : ''}${
        result.failed?.length ? `，失败 ${result.failed.length} 个` : ''
      }`
    )
    await scanOrphans()
  } catch {
    // 提示由拦截器处理
  } finally {
    orphanCleaning.value = false
  }
}

/** 字节数 → 可读大小 */
function sizeLabel(bytes) {
  if (!bytes) {
    return '0 B'
  }
  if (bytes < 1024) {
    return `${bytes} B`
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** Logo 相关状态 */
const logoUrl = ref('')
const logoUploading = ref(false)
const logoInput = ref(null)

/** 与后端 ImageValidator 一致：jpg/png、≤2MB（前端先拦一道，体验更好） */
const MAX_LOGO_SIZE = 2 * 1024 * 1024
const ALLOWED_LOGO_TYPES = ['image/jpeg', 'image/png']

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
    // 预览地址由后端拼（与报名页出参同一口径）：配置列表里的 club_logo 是**对象 key**，
    // 前端不重复实现前缀拼接，直接问公开的 /recruit/info 要拼好的地址
    const info = await getRecruitInfo()
    logoUrl.value = info?.logoUrl || ''
  } finally {
    loading.value = false
  }
}

/* ---------------- Logo 上传 / 清除 ---------------- */

function pickLogo() {
  logoInput.value?.click()
}

async function onLogoChange(event) {
  const file = event.target.files?.[0]
  // 复位 input，否则连续选同一个文件不会再触发 change
  event.target.value = ''
  if (!file) {
    return
  }
  if (!ALLOWED_LOGO_TYPES.includes(file.type)) {
    ElMessage.warning('Logo 仅支持 jpg / png 格式')
    return
  }
  if (file.size > MAX_LOGO_SIZE) {
    ElMessage.warning('Logo 大小不能超过 2MB')
    return
  }

  logoUploading.value = true
  try {
    const url = await uploadClubLogo(file)
    logoUrl.value = url || ''
    ElMessage.success('Logo 已更新')
    await load()
  } catch {
    // 提示由 axios 拦截器统一处理
  } finally {
    logoUploading.value = false
  }
}

async function clearLogo() {
  try {
    await ElMessageBox.confirm('确定清除社团 Logo 吗？报名页将不再显示 Logo。', '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  logoUploading.value = true
  try {
    await removeClubLogo()
    logoUrl.value = ''
    ElMessage.success('Logo 已清除')
    await load()
  } catch {
    // ignore
  } finally {
    logoUploading.value = false
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
      <h3 class="settings__title">社团 Logo</h3>
      <div class="settings__field">
        <div class="settings__label">报名页 Logo</div>
        <div class="settings__hint">
          显示在报名页顶部（PRD F-001 第 1 步）。支持 jpg / png，不超过 2MB；建议方形、底色透明或与页面同色。
          未上传时报名页不显示 Logo，其它功能不受影响。
        </div>
        <div class="settings__logo">
          <div class="settings__logo-preview" :class="{ 'is-empty': !logoUrl }">
            <img v-if="logoUrl" :src="logoUrl" alt="社团 Logo" />
            <span v-else>未设置</span>
          </div>
          <div class="settings__logo-actions">
            <el-button
              type="primary"
              size="small"
              :loading="logoUploading"
              @click="pickLogo"
            >
              {{ logoUrl ? '更换 Logo' : '上传 Logo' }}
            </el-button>
            <el-button
              v-if="logoUrl"
              size="small"
              :disabled="logoUploading"
              @click="clearLogo"
            >
              清除
            </el-button>
          </div>
        </div>
        <input
          ref="logoInput"
          class="settings__file"
          type="file"
          accept="image/jpeg,image/png"
          @change="onLogoChange"
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

    <!--
      存储维护（T26，**仅超管**）—— 对象存储孤儿文件清理。
      孤儿 = 在受管前缀（头像 / 公告配图 / 社团 Logo）下、但没有任何数据引用的对象。
      刻意做成「先扫描、再清理」两步：扫描只读不删，清理必须显式勾选。
    -->
    <section v-if="showStorageTool" class="settings__block">
      <h3 class="settings__title">存储维护</h3>
      <div class="settings__hint">
        换头像、删公告之后，旧图片会残留在对象存储里（没人引用了，但仍占空间）。这里可以扫出这些"孤儿"并清理。
        <b>扫描只读、不会删任何东西</b>；清理要你先勾选、再确认，且只删受管目录下的文件。
      </div>

      <div class="settings__actions settings__actions--start">
        <el-button :loading="orphanScanning" @click="scanOrphans">扫描孤儿文件</el-button>
        <el-button
          v-if="selectedKeys.length"
          type="danger"
          :loading="orphanCleaning"
          @click="cleanOrphans"
        >
          清理选中的 {{ selectedKeys.length }} 个
        </el-button>
      </div>

      <div v-if="scanResult" class="settings__storage-summary">
        桶 <code>{{ scanResult.bucket }}</code>：受管对象
        <b>{{ scanResult.totalObjects }}</b> 个，其中被引用
        <b>{{ scanResult.referencedObjects }}</b> 个，<b class="is-danger">孤儿
        {{ scanResult.orphans.length }}</b> 个（合计 {{ sizeLabel(scanResult.orphanBytes) }}）。
        <template v-if="scanResult.truncated">清单已截断，只显示前 500 条。</template>
        <template v-if="!scanResult.orphans.length">🎉 没有孤儿文件，无需清理。</template>
      </div>

      <el-table
        v-if="scanResult?.orphans?.length"
        :data="scanResult.orphans"
        size="small"
        border
        max-height="320"
        @selection-change="(rows) => (selectedKeys = rows.map((r) => r.key))"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column prop="kind" label="类型" width="90" />
        <el-table-column prop="key" label="对象 key" min-width="240" show-overflow-tooltip />
        <el-table-column label="大小" width="90">
          <template #default="{ row }">{{ sizeLabel(row.size) }}</template>
        </el-table-column>
        <el-table-column label="最后修改" width="170">
          <template #default="{ row }">
            {{ row.lastModified ? row.lastModified.replace('T', ' ').slice(0, 19) : '—' }}
          </template>
        </el-table-column>
      </el-table>
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

/* 存储维护：按钮左对齐（不是"标签 + 按钮"那种两端对齐） */
.settings__actions--start {
  justify-content: flex-start;
}

.settings__storage-summary {
  margin: 12px 0 8px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
}

.settings__storage-summary code {
  padding: 1px 4px;
  border-radius: 4px;
  background: var(--border-color-light);
}

.settings__storage-summary .is-danger {
  color: var(--el-color-danger);
}

/* Logo：预览框 + 操作按钮 */
.settings__logo {
  display: flex;
  align-items: center;
  gap: 14px;
}

.settings__logo-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fafafa;
  font-size: 12px;
  color: #c0c4cc;
}

.settings__logo-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.settings__logo-preview.is-empty {
  border-style: dashed;
}

.settings__logo-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings__file {
  display: none;
}

@media (max-width: 768px) {
  .settings__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
