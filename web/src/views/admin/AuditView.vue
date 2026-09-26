<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  approveApply,
  approveApplyBatch,
  getApplyList,
  getApplyStats,
  getSmsConfig,
  rejectApply
} from '@/api/recruit'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'
import { copyText } from '@/utils/sms'
import { downloadCsv as downloadCsvFile, today } from '@/utils/csv'
import SmsNotifyDialog from '@/components/SmsNotifyDialog.vue'

/**
 * 审核管理台（PRD F-003，纳新主链核心）+ 短信通知提效工具（PRD F-004）
 *
 * 要点：
 *   - 数据隔离由后端决定（部长只看"意向部门含本部门"的记录），前端只按角色控制可见性
 *   - 通过 = 建号（随机初始密码 + 首登强制改密），明文密码只在结果弹窗出现一次
 *   - 拒绝必须填原因（会展示给被拒者）
 *   - 多选对所有行开放：批量通过只吃勾选的「待审」，批量发送短信只吃勾选的「已处理」
 *   - 短信工具不代发，只拼话术 + 复制（移动端可 sms: 唤起）
 */
const isMobile = useIsMobile()
const dictStore = useDictStore()
const userStore = useUserStore()

/** 报名状态（注意：与 sys_dict 的「账号状态」不是一回事） */
const APPLY_STATUS = {
  0: { label: '待审', type: 'warning' },
  1: { label: '通过', type: 'success' },
  2: { label: '拒绝', type: 'danger' }
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const stats = reactive({ pending: 0, approved: 0, rejected: 0 })
const selectedRows = ref([])

const query = reactive({
  status: 0,
  college: '',
  department: '',
  keyword: '',
  page: 1,
  size: 20
})

/** 社长团 / 超管 = 全量范围；部长 = 本部门范围 */
const isFullScope = computed(() => userStore.isSuperAdminUser || userStore.canManageAllUsers)

const approveVisible = ref(false)
const approveMode = ref('single')
const approveSaving = ref(false)
const approveForm = reactive({ id: null, department: '', duty: 0 })

const resultVisible = ref(false)
const result = ref(null)

const rejectVisible = ref(false)
const rejectSaving = ref(false)
const rejectForm = reactive({ id: null, name: '', phone: '', reason: '' })

/* ---- 短信通知提效工具（F-004） ---- */
const smsVisible = ref(false)
const smsTitle = ref('短信通知')
const smsTargets = ref([])
const smsConfig = ref({ systemUrl: '', passTemplate: '', rejectTemplate: '' })

/** 勾选行按状态分桶：批量通过只吃待审，批量发送短信只吃已处理 */
const selectedPending = computed(() => selectedRows.value.filter((row) => row.status === 0))
const selectedDone = computed(() => selectedRows.value.filter((row) => row.status !== 0))

const colleges = computed(() => dictStore.cache.college || [])
const departments = computed(() => dictStore.cache.department || [])
const duties = [
  { value: 0, label: '成员' },
  { value: 1, label: '副部长' },
  { value: 2, label: '部长' },
  { value: 3, label: '社长' }
]

function statusLabel(status) {
  return APPLY_STATUS[status]?.label || '-'
}

function statusType(status) {
  return APPLY_STATUS[status]?.type || 'info'
}

function deptLabels(codes) {
  if (!codes || !codes.length) {
    return '—'
  }
  return codes.map((code) => dictStore.labelOf('department', code)).join('、')
}

function tagLabels(row) {
  const labels = (row.tags || []).map((code) => dictStore.labelOf('tag', code))
  if (row.tagText && (row.tags || []).includes('other')) {
    return [...labels.slice(0, -1), `其他(${row.tagText})`].join('、')
  }
  return labels.length ? labels.join('、') : '—'
}

function majorLabel(row) {
  if (!row.major) {
    return '—'
  }
  if (row.major === 'other') {
    return row.majorText ? `其他（${row.majorText}）` : '其他'
  }
  return dictStore.labelOf('major', row.major)
}

function genderLabel(gender) {
  return { 0: '未填', 1: '男', 2: '女' }[gender] || '未填'
}

function areaLabel(row) {
  if (!row.province && !row.city) {
    return '—'
  }
  return `${row.province || ''} ${row.city || ''}`.trim()
}

function timeLabel(value) {
  if (!value) {
    return '—'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (params.college === '') {
      delete params.college
    }
    if (params.department === '') {
      delete params.department
    }
    if (params.keyword === '') {
      delete params.keyword
    }
    const data = await getApplyList(params)
    list.value = data.records || []
    total.value = data.total || 0
    selectedRows.value = []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    Object.assign(stats, await getApplyStats())
  } catch {
    // 拦截器已提示
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, { status: 0, college: '', department: '', keyword: '', page: 1 })
  load()
}

function reloadAll() {
  load()
  loadStats()
  dictStore.clear()
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

/* ---------------- 通过 ---------------- */

function openApprove(row) {
  approveMode.value = 'single'
  approveForm.id = row.id
  // 默认：部门取意向部门第一个，职位取成员
  approveForm.department = (row.intentDepartments || [])[0] || ''
  approveForm.duty = 0
  approveVisible.value = true
}

function openApproveBatch() {
  if (!selectedPending.value.length) {
    ElMessage.warning('请先勾选待审记录')
    return
  }
  approveMode.value = 'batch'
  approveForm.id = null
  approveForm.department = ''
  approveForm.duty = 0
  approveVisible.value = true
}

async function submitApprove() {
  approveSaving.value = true
  try {
    let data
    if (approveMode.value === 'single') {
      data = await approveApply({
        id: approveForm.id,
        department: approveForm.department || null,
        duty: approveForm.duty
      })
    } else {
      data = await approveApplyBatch({
        ids: selectedPending.value.map((row) => row.id),
        duty: approveForm.duty
      })
    }
    approveVisible.value = false
    result.value = data
    resultVisible.value = true
    reloadAll()
  } catch {
    // 拦截器已提示，弹窗保留
  } finally {
    approveSaving.value = false
  }
}

/* ---------------- 拒绝 ---------------- */

function openReject(row) {
  rejectForm.id = row.id
  rejectForm.name = row.name
  rejectForm.phone = row.phone
  rejectForm.reason = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectSaving.value = true
  try {
    const reason = rejectForm.reason.trim()
    await rejectApply({ id: rejectForm.id, reason })
    ElMessage.success('已拒绝该报名')
    rejectVisible.value = false
    reloadAll()
    // 拒绝后接着弹短信通知，省得干部再去列表里找这条记录
    openSmsDialog(
      [
        {
          name: rejectForm.name,
          phone: rejectForm.phone,
          status: 2,
          rejectReason: reason
        }
      ],
      '通知被拒同学'
    )
  } catch {
    // 拦截器已提示
  } finally {
    rejectSaving.value = false
  }
}

/* ---------------- 短信通知提效工具（F-004） ---------------- */

/**
 * 打开短信话术弹窗。
 *
 * <p>打开前现拉一次配置，保证超管刚改的模板立即生效。
 */
async function openSmsDialog(targets, title) {
  if (!targets || !targets.length) {
    ElMessage.warning('没有可通知的对象')
    return
  }
  smsTargets.value = targets.map((item) => ({
    name: item.name,
    phone: item.phone,
    status: item.status,
    password: item.password || '',
    rejectReason: item.rejectReason || ''
  }))
  smsTitle.value = title
  try {
    smsConfig.value = await getSmsConfig()
  } catch {
    smsConfig.value = { systemUrl: '', passTemplate: '', rejectTemplate: '' }
  }
  smsVisible.value = true
}

/** 通过后：通知新生（含初始密码，密码只在此处可得） */
function notifyApproved() {
  const targets = (result.value?.credentials || []).map((item) => ({ ...item, status: 1 }))
  openSmsDialog(targets, '通知新生（含初始密码）')
}

/** 批量发送短信：只吃勾选的「已处理」记录（PRD F-004） */
function openBatchSms() {
  if (!selectedDone.value.length) {
    ElMessage.warning('请先勾选已处理（通过 / 拒绝）的记录')
    return
  }
  openSmsDialog(selectedDone.value, '批量发送短信')
}

/** 单条通知：仅「已拒绝」记录（通过记录的事后通知拿不到初始密码，改在创建结果弹窗里复制） */
function notifyRejected(row) {
  openSmsDialog([row], `通知 ${row.name}`)
}

/* ---------------- 密码清单 ---------------- */

function credentialsText() {
  const rows = (result.value?.credentials || []).map(
    (item) => `${item.name}\t${item.phone}\t${item.password}`
  )
  return ['姓名\t手机号\t初始密码', ...rows].join('\n')
}

async function copyCredentials() {
  const ok = await copyText(credentialsText())
  if (ok) {
    ElMessage.success('已复制到剪贴板')
  } else {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

function downloadCsv() {
  const rows = [['姓名', '手机号', '初始密码']]
  ;(result.value?.credentials || []).forEach((item) => {
    rows.push([item.name, item.phone, item.password])
  })
  downloadCsvFile(`初始密码清单_${today()}.csv`, rows)
}

onMounted(async () => {
  await dictStore.loadMany(['college', 'major', 'department', 'tag'])
  await Promise.all([load(), loadStats()])
})
</script>

<template>
  <div class="page audit">
    <div class="audit__head">
      <div>
        <h2 class="page-title">审核管理台</h2>
        <p class="page-desc">
          待审 <b class="audit__num">{{ stats.pending }}</b> ·
          通过 <b class="audit__num">{{ stats.approved }}</b> ·
          拒绝 <b class="audit__num">{{ stats.rejected }}</b>
          <span v-if="!isFullScope" class="audit__scope">（部长视角：仅本部门）</span>
        </p>
      </div>
      <div class="audit__head-actions">
        <el-button :disabled="!selectedPending.length" @click="openApproveBatch">
          批量通过{{ selectedPending.length ? `(${selectedPending.length})` : '' }}
        </el-button>
        <el-button :disabled="!selectedDone.length" @click="openBatchSms">
          批量发送短信{{ selectedDone.length ? `(${selectedDone.length})` : '' }}
        </el-button>
        <el-button @click="reloadAll">刷新</el-button>
      </div>
    </div>

    <div class="audit__filters">
      <el-radio-group v-model="query.status" @change="search">
        <el-radio-button :value="0">待审</el-radio-button>
        <el-radio-button :value="1">通过</el-radio-button>
        <el-radio-button :value="2">拒绝</el-radio-button>
        <el-radio-button :value="null">全部</el-radio-button>
      </el-radio-group>
      <el-select v-model="query.college" placeholder="学院" clearable style="width: 150px" @change="search">
        <el-option v-for="item in colleges" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>
      <el-select
        v-model="query.department"
        placeholder="意向部门"
        clearable
        :disabled="!isFullScope"
        style="width: 150px"
        @change="search"
      >
        <el-option v-for="item in departments" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="姓名 / 手机号"
        clearable
        style="width: 180px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 桌面：表格 -->
    <el-table
      v-if="!isMobile"
      v-loading="loading"
      :data="list"
      border
      stripe
      row-key="id"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column prop="name" label="姓名" width="90" fixed="left" />
      <el-table-column prop="phone" label="手机号" width="118" />
      <el-table-column label="学院" width="120">
        <template #default="{ row }">{{ dictStore.labelOf('college', row.college) }}</template>
      </el-table-column>
      <el-table-column label="专业" width="140">
        <template #default="{ row }">{{ majorLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="意向部门" width="130">
        <template #default="{ row }">{{ deptLabels(row.intentDepartments) }}</template>
      </el-table-column>
      <el-table-column label="兴趣标签" min-width="170">
        <template #default="{ row }">{{ tagLabels(row) }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="130">
        <template #default="{ row }">{{ timeLabel(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="primary" size="small" @click="openApprove(row)">通过</el-button>
            <el-button link type="danger" size="small" @click="openReject(row)">拒绝</el-button>
          </template>
          <template v-else>
            <el-button
              v-if="row.status === 2"
              link
              type="primary"
              size="small"
              @click="notifyRejected(row)"
            >
              通知
            </el-button>
            <span class="audit__done">{{ timeLabel(row.reviewedAt) }}</span>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="audit__cards">
      <div v-for="row in list" :key="row.id" class="audit__card">
        <div class="audit__card-head">
          <label class="audit__card-check" v-if="row.status === 0">
            <el-checkbox
              :model-value="selectedRows.some((item) => item.id === row.id)"
              @change="(checked) => (checked ? selectedRows.push(row) : (selectedRows = selectedRows.filter((item) => item.id !== row.id)))"
            />
          </label>
          <span class="audit__card-name">{{ row.name }}</span>
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </div>
        <div class="audit__card-line">{{ row.phone }}</div>
        <div class="audit__card-line">
          {{ dictStore.labelOf('college', row.college) }} · {{ majorLabel(row) }}
        </div>
        <div class="audit__card-line">意向：{{ deptLabels(row.intentDepartments) }}</div>
        <div class="audit__card-line">标签：{{ tagLabels(row) }}</div>
        <div class="audit__card-line audit__card-meta">
          {{ genderLabel(row.gender) }} · {{ areaLabel(row) }} · {{ timeLabel(row.createdAt) }}
        </div>
        <div v-if="row.status === 0" class="audit__card-actions">
          <el-button type="primary" size="small" @click="openApprove(row)">通过</el-button>
          <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
        </div>
        <div v-else class="audit__card-meta audit__card-done">
          <span>审核于 {{ timeLabel(row.reviewedAt) }}</span>
          <el-button
            v-if="row.status === 2"
            link
            type="primary"
            size="small"
            @click="notifyRejected(row)"
          >
            通知
          </el-button>
        </div>
      </div>
      <p v-if="!loading && !list.length" class="audit__empty">暂无符合条件的报名</p>
    </div>

    <div v-if="total > query.size" class="audit__pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>

    <!-- 通过弹窗 -->
    <el-dialog
      v-model="approveVisible"
      :title="approveMode === 'single' ? '通过报名并创建账号' : '批量通过'"
      :width="isMobile ? '92%' : '460px'"
    >
      <el-form label-width="96px">
        <el-form-item v-if="approveMode === 'single'" label="分配部门">
          <el-select v-model="approveForm.department" :disabled="!isFullScope" placeholder="默认取意向部门第一个">
            <el-option v-for="item in departments" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="分配部门">
          <span class="audit__hint">取各条意向部门的第一个；部长只能分到本部门</span>
        </el-form-item>
        <el-form-item label="职位">
          <el-select v-model="approveForm.duty">
            <el-option v-for="item in duties" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          将通过审核并创建账号（手机号为登录名，系统随机生成初始密码）；新生首次登录会被要求修改密码。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approveSaving" @click="submitApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- 结果（密码清单）弹窗 -->
    <el-dialog v-model="resultVisible" title="账号创建结果" :width="isMobile ? '92%' : '620px'">
      <template v-if="result">
        <el-alert v-if="result.failedCount" type="warning" :closable="false" show-icon class="audit__alert">
          成功 {{ result.successCount }} 条，失败 {{ result.failedCount }} 条
        </el-alert>
        <p class="audit__hint">
          初始密码只显示这一次，请立即通过短信告知本人；新生首次登录需修改密码。
          点「复制通知话术」会按模板生成短信，批量通过时可在弹窗里逐条复制。
        </p>
        <el-table :data="result.credentials" border size="small">
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column prop="password" label="初始密码" />
        </el-table>
        <div v-if="result.failures?.length" class="audit__failures">
          <p class="audit__hint">未成功：</p>
          <p v-for="(item, index) in result.failures" :key="index" class="audit__failure">· {{ item }}</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="copyCredentials">复制清单</el-button>
        <el-button @click="downloadCsv">下载 CSV</el-button>
        <el-button @click="notifyApproved">复制通知话术</el-button>
        <el-button type="primary" @click="resultVisible = false">我已记录</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectVisible" title="拒绝报名" :width="isMobile ? '92%' : '460px'">
      <el-form label-width="80px">
        <el-form-item label="申请人">
          <span>{{ rejectForm.name }}</span>
        </el-form-item>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="该原因会展示给被拒者，请使用可对外措辞"
          />
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon>
          被拒同学可以修正后重新提交报名，届时会重新进入待审列表。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSaving" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 短信通知弹窗（F-004）：通过结果 / 拒绝后 / 批量发送 共用 -->
    <SmsNotifyDialog
      v-model="smsVisible"
      :title="smsTitle"
      :targets="smsTargets"
      :config="smsConfig"
    />
  </div>
</template>

<style scoped>
.audit__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.audit__head-actions {
  display: flex;
  gap: 8px;
  flex: none;
}

.audit__num {
  color: var(--brand-primary);
}

.audit__scope {
  color: #909399;
}

.audit__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.audit__done {
  font-size: 12px;
  color: #909399;
}

.audit__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit__card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.audit__card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.audit__card-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
}

.audit__card-line {
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
}

.audit__card-meta {
  font-size: 12px;
  color: #909399;
}

.audit__card-done {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.audit__card-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.audit__empty {
  padding: 32px 0;
  text-align: center;
  color: #909399;
}

.audit__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.audit__alert {
  margin-bottom: 10px;
}

.audit__hint {
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.8;
  color: #909399;
}

.audit__failures {
  margin-top: 12px;
}

.audit__failure {
  margin: 2px 0;
  font-size: 12px;
  color: #f56c6c;
}

@media (max-width: 768px) {
  .audit__head {
    flex-direction: column;
  }

  .audit__head-actions {
    flex-wrap: wrap;
  }
}
</style>
