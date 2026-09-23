<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMemberDetail, getMemberList } from '@/api/member'
import { useDictStore } from '@/stores/dict'
import { useIsMobile } from '@/composables/useIsMobile'
import MemberDetailDrawer from '@/components/MemberDetailDrawer.vue'

/**
 * 成员列表 / 成员展板（成员端，PRD F-006）
 *
 * 只读，且**只展示基础列**（姓名 / 部门 / 职位 / 学院 / 专业 / 个人简介 / 头像）：
 * 手机号与学号对普通成员不可见，后端已把它们从响应里抹掉（PRD 第五章敏感列可见性），
 * 所以这里连列都不渲染。
 */
const isMobile = useIsMobile()
const dictStore = useDictStore()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const drawerVisible = ref(false)
const current = ref(null)

const query = reactive({
  keyword: '',
  college: '',
  department: '',
  page: 1,
  size: 20
})

const colleges = computed(() => dictStore.cache.college || [])
const departments = computed(() => dictStore.cache.department || [])

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

function initial(name) {
  return (name || '?').slice(0, 1)
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
  Object.assign(query, { keyword: '', college: '', department: '', page: 1 })
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

onMounted(async () => {
  await dictStore.loadMany(['college', 'major', 'department', 'duty', 'status'])
  await load()
})
</script>

<template>
  <div class="page members">
    <div class="members__head">
      <h2 class="page-title">成员列表</h2>
      <p class="page-desc">
        社内成员的公开档案（共 <b class="members__num">{{ total }}</b> 人）。手机号与学号仅干部可见。
      </p>
    </div>

    <div class="members__filters">
      <el-input
        v-model="query.keyword"
        placeholder="姓名"
        clearable
        style="width: 160px"
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
        style="width: 140px"
        @change="search"
      >
        <el-option v-for="item in departments" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 桌面：表格（基础列） -->
    <el-table v-if="!isMobile" v-loading="loading" :data="list" border stripe>
      <el-table-column label="姓名" width="150">
        <template #default="{ row }">
          <div class="members__name-cell">
            <el-avatar :size="26" :src="row.avatarUrl || undefined">{{ initial(row.name) }}</el-avatar>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="部门" width="110">
        <template #default="{ row }">{{ deptLabel(row.department) }}</template>
      </el-table-column>
      <el-table-column label="职位" width="100">
        <template #default="{ row }">{{ dictStore.labelOf('duty', row.duty) }}</template>
      </el-table-column>
      <el-table-column label="学院" width="150">
        <template #default="{ row }">{{ dictStore.labelOf('college', row.college) || '—' }}</template>
      </el-table-column>
      <el-table-column label="专业" width="170">
        <template #default="{ row }">{{ majorLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="个人简介" min-width="220">
        <template #default="{ row }">
          <span class="members__bio">{{ row.bio || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="members__cards">
      <div v-for="row in list" :key="row.id" class="members__card" @click="openDetail(row)">
        <el-avatar :size="40" :src="row.avatarUrl || undefined">{{ initial(row.name) }}</el-avatar>
        <div class="members__card-main">
          <div class="members__card-head">
            <span class="members__card-name">{{ row.name }}</span>
            <span class="members__card-tag">
              {{ deptLabel(row.department) }} · {{ dictStore.labelOf('duty', row.duty) }}
            </span>
          </div>
          <p class="members__card-line">
            {{ dictStore.labelOf('college', row.college) || '—' }} · {{ majorLabel(row) }}
          </p>
          <p v-if="row.bio" class="members__card-bio">{{ row.bio }}</p>
        </div>
      </div>
      <p v-if="!loading && !list.length" class="members__empty">暂无成员数据</p>
    </div>

    <div v-if="total > query.size" class="members__pager">
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
  </div>
</template>

<style scoped>
.members__num {
  color: var(--brand-primary);
}

.members__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.members__name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.members__bio {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  color: #606266;
}

.members__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.members__card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
  cursor: pointer;
}

.members__card-main {
  min-width: 0;
  flex: 1;
}

.members__card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.members__card-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.members__card-tag {
  flex: none;
  font-size: 12px;
  color: #909399;
}

.members__card-line {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

.members__card-bio {
  margin: 4px 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.members__empty {
  padding: 32px 0;
  text-align: center;
  color: #909399;
}

.members__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
