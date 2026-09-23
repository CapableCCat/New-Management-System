<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMemberDetail, getMemberList, updateMember } from '@/api/member'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'
import { canManageAll, canManageDepartment } from '@/constants/roles'
import MemberDetailDrawer from '@/components/MemberDetailDrawer.vue'

/**
 * 成员档案管理（PRD F-006）
 *
 * 权限要点（后端为准，这里只控制可见性）：
 *   - 行范围：部长只看本部门；社长团 / 超管看全部
 *   - 列范围：手机号、学号对成员不可见 —— 本页进入者都是干部，故正常展示
 *   - 编辑：部长可改本部门成员的常规字段，但**部门锁定本部门、职位不可改、不能编辑自己**（防提权）
 *   - `role`（是否超管）不开放编辑
 *   - 手机号变更 = 登录名变更，需唯一性校验；学号冲突提示「该学号已被使用」
 */
const isMobile = useIsMobile()
const dictStore = useDictStore()
const userStore = useUserStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const drawerVisible = ref(false)
const current = ref(null)

const editVisible = ref(false)
const editSaving = ref(false)
const editId = ref(null)
const editForm = reactive({
  name: '',
  phone: '',
  studentId: '',
  college: '',
  major: '',
  majorText: '',
  gender: 0,
  province: '',
  city: '',
  department: null,
  duty: 0,
  status: 0
})

const query = reactive({
  keyword: '',
  college: '',
  department: '',
  duty: '',
  status: '',
  page: 1,
  size: 20
})

/** 全社管理资格（社长团 / 超管）：决定部门与职位能否改、部门筛选能否用 */
const canFullEdit = computed(() => canManageAll(userStore.profile))
/** 部门管理资格（部长及以上）：决定能不能编辑 */
const canEditAny = computed(() => canManageDepartment(userStore.profile))

const colleges = computed(() => dictStore.cache.college || [])
const majors = computed(() => dictStore.cache.major || [])
const departmentOptions = computed(() =>
  (dictStore.cache.department || []).map((item) => ({ value: Number(item.code), label: item.label }))
)
const dutyOptions = computed(() =>
  (dictStore.cache.duty || []).map((item) => ({ value: Number(item.code), label: item.label }))
)
const statusOptions = computed(() =>
  (dictStore.cache.status || []).map((item) => ({ value: Number(item.code), label: item.label }))
)

const isOtherMajor = computed(() => editForm.major === 'other')

function deptLabel(code) {
  return dictStore.labelOf('department', code) || '未分配'
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

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function initial(name) {
  return (name || '?').slice(0, 1)
}

/** 能否编辑该行：部长不能编辑自己（否则可自我提权），后端同样会拦 */
function canEdit(row) {
  if (!canEditAny.value) {
    return false
  }
  if (!canFullEdit.value && row.id === userStore.profile?.id) {
    return false
  }
  return true
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword.trim()) {
      params.keyword = query.keyword.trim()
    }
    if (query.college) {
      params.college = query.college
    }
    if (query.department !== '') {
      params.department = query.department
    }
    if (query.duty !== '') {
      params.duty = query.duty
    }
    if (query.status !== '') {
      params.status = query.status
    }
    const data = await getMemberList(params)
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  Object.assign(query, {
    keyword: '',
    college: '',
    department: '',
    duty: '',
    status: '',
    page: 1
  })
  load()
}

async function openDetail(row) {
  try {
    current.value = await getMemberDetail(row.id)
    drawerVisible.value = true
  } catch {
    ElMessage.warning('读取成员档案失败')
  }
}

async function openEdit(row) {
  try {
    const data = await getMemberDetail(row.id)
    editId.value = data.id
    Object.assign(editForm, {
      name: data.name || '',
      phone: data.phone || '',
      studentId: data.studentId || '',
      college: data.college || '',
      major: data.major || '',
      majorText: data.majorText || '',
      gender: data.gender ?? 0,
      province: data.province || '',
      city: data.city || '',
      department: data.department ?? null,
      duty: data.duty ?? 0,
      status: data.status ?? 0
    })
    editVisible.value = true
  } catch {
    ElMessage.warning('读取成员档案失败')
  }
}

async function submitEdit() {
  if (!editForm.name.trim()) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(editForm.phone.trim())) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (isOtherMajor.value && !editForm.majorText.trim()) {
    ElMessage.warning('选择「其他」专业时，请填写具体专业名称')
    return
  }

  const payload = {
    id: editId.value,
    name: editForm.name.trim(),
    phone: editForm.phone.trim(),
    studentId: editForm.studentId.trim(),
    college: editForm.college || null,
    major: editForm.major || null,
    majorText: isOtherMajor.value ? editForm.majorText.trim() : null,
    gender: editForm.gender,
    province: editForm.province.trim(),
    city: editForm.city.trim(),
    department: editForm.department,
    duty: editForm.duty,
    status: editForm.status
  }

  // 与后端一致：部长提交的部门/职位会被忽略，这里先按本部门回填，避免界面误导
  if (!canFullEdit.value) {
    const row = list.value.find((item) => item.id === editId.value)
    payload.department = row?.department ?? null
    payload.duty = row?.duty ?? 0
  }

  editSaving.value = true
  try {
    await updateMember(payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    await load()
  } catch {
    // 提示由 axios 拦截器统一处理，弹窗保留便于修正
  } finally {
    editSaving.value = false
  }
}

/** 冻结是影响面较大的操作，先二次确认 */
async function onStatusChange(next) {
  if (next === 1) {
    try {
      await ElMessageBox.confirm(
        `确定冻结「${editForm.name}」吗？该账号将立即无法访问系统（登录会被拒绝）。`,
        '提示',
        { type: 'warning' }
      )
    } catch {
      editForm.status = 0
    }
  }
}

onMounted(async () => {
  await dictStore.loadMany(['college', 'major', 'department', 'duty', 'status'])
  await load()
})
</script>

<template>
  <div class="page member-admin">
    <div class="member-admin__head">
      <div>
        <h2 class="page-title">成员档案</h2>
        <p class="page-desc">
          正式成员共 <b class="member-admin__num">{{ total }}</b> 人
          <span v-if="!canFullEdit" class="member-admin__scope">（部长视角：仅本部门）</span>
        </p>
      </div>
      <div class="member-admin__head-actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="member-admin__filters">
      <el-input
        v-model="query.keyword"
        placeholder="姓名 / 手机号 / 学号"
        clearable
        style="width: 190px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="query.college" placeholder="学院" clearable style="width: 160px" @change="search">
        <el-option v-for="item in colleges" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>
      <el-select
        v-model="query.department"
        placeholder="部门"
        clearable
        :disabled="!canFullEdit"
        style="width: 140px"
        @change="search"
      >
        <el-option
          v-for="item in departmentOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="query.duty" placeholder="职位" clearable style="width: 120px" @change="search">
        <el-option v-for="item in dutyOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 桌面：表格 -->
    <el-table v-if="!isMobile" v-loading="loading" :data="list" border stripe>
      <el-table-column label="姓名" width="140">
        <template #default="{ row }">
          <div class="member-admin__name-cell">
            <el-avatar :size="26" :src="row.avatarUrl || undefined">{{ initial(row.name) }}</el-avatar>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column label="学号" width="120">
        <template #default="{ row }">{{ row.studentId || '—' }}</template>
      </el-table-column>
      <el-table-column label="学院" width="140">
        <template #default="{ row }">{{ dictStore.labelOf('college', row.college) || '—' }}</template>
      </el-table-column>
      <el-table-column label="专业" width="160">
        <template #default="{ row }">{{ majorLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="部门" width="100">
        <template #default="{ row }">{{ deptLabel(row.department) }}</template>
      </el-table-column>
      <el-table-column label="职位" width="90">
        <template #default="{ row }">{{ dictStore.labelOf('duty', row.duty) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
            {{ dictStore.labelOf('status', row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="加入时间" width="130">
        <template #default="{ row }">{{ timeLabel(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button
            link
            type="primary"
            size="small"
            :disabled="!canEdit(row)"
            :title="canEdit(row) ? '' : '部长不能编辑自己的档案'"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="member-admin__cards">
      <div v-for="row in list" :key="row.id" class="member-admin__card">
        <div class="member-admin__card-head">
          <el-avatar :size="34" :src="row.avatarUrl || undefined">{{ initial(row.name) }}</el-avatar>
          <span class="member-admin__card-name">{{ row.name }}</span>
          <el-tag :type="row.status === 1 ? 'danger' : 'success'" size="small">
            {{ dictStore.labelOf('status', row.status) }}
          </el-tag>
        </div>
        <div class="member-admin__card-line">{{ row.phone }} · {{ row.studentId || '无学号' }}</div>
        <div class="member-admin__card-line">
          {{ dictStore.labelOf('college', row.college) || '—' }} · {{ majorLabel(row) }}
        </div>
        <div class="member-admin__card-line">
          {{ deptLabel(row.department) }} · {{ dictStore.labelOf('duty', row.duty) }}
        </div>
        <div class="member-admin__card-meta">加入于 {{ timeLabel(row.createdAt) }}</div>
        <div class="member-admin__card-actions">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button
            size="small"
            type="primary"
            :disabled="!canEdit(row)"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
        </div>
      </div>
      <p v-if="!loading && !list.length" class="member-admin__empty">暂无符合条件的成员</p>
    </div>

    <div v-if="total > query.size" class="member-admin__pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>

    <MemberDetailDrawer v-model="drawerVisible" :member="current" />

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑成员档案"
      :width="isMobile ? '92%' : '520px'"
      :close-on-click-modal="false"
    >
      <el-form label-width="84px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="学号">
          <el-input v-model="editForm.studentId" maxlength="32" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="editForm.college" clearable placeholder="未填写">
            <el-option v-for="item in colleges" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="editForm.major" clearable placeholder="未填写">
            <el-option v-for="item in majors" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isOtherMajor" label="专业名称">
          <el-input v-model="editForm.majorText" maxlength="64" placeholder="请填写具体专业" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">不填</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生源地">
          <div class="member-admin__area">
            <el-input v-model="editForm.province" placeholder="省 / 直辖市" maxlength="32" />
            <el-input v-model="editForm.city" placeholder="市" maxlength="32" />
          </div>
        </el-form-item>
        <el-form-item label="部门">
          <el-select
            v-model="editForm.department"
            clearable
            :disabled="!canFullEdit"
            :placeholder="canFullEdit ? '未分配' : '部长不可修改部门'"
          >
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="职位">
          <el-select v-model="editForm.duty" :disabled="!canFullEdit">
            <el-option v-for="item in dutyOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 160px" @change="onStatusChange">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          手机号即登录账号，变更后请提醒本人用新号码登录；被冻结的账号会立即无法访问系统。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.member-admin__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.member-admin__head-actions {
  display: flex;
  gap: 8px;
  flex: none;
}

.member-admin__num {
  color: var(--brand-primary);
}

.member-admin__scope {
  color: #909399;
}

.member-admin__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.member-admin__name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-admin__area {
  display: flex;
  gap: 8px;
  width: 100%;
}

.member-admin__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.member-admin__card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.member-admin__card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.member-admin__card-name {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.member-admin__card-line {
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
}

.member-admin__card-meta {
  font-size: 12px;
  color: #909399;
}

.member-admin__card-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.member-admin__empty {
  padding: 32px 0;
  text-align: center;
  color: #909399;
}

.member-admin__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .member-admin__head {
    flex-direction: column;
  }

  .member-admin__head-actions {
    flex-wrap: wrap;
  }
}
</style>
