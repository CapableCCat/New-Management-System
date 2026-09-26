<script setup>
/**
 * 反馈列表（PRD F-014，管理端）—— 社长团 / 超管，供复盘
 *
 * 权限：路由 meta.menu.capability = canViewFeedback，菜单与守卫同源（T19 / D119）；
 * 接口本身也由后端把守（三段路径不在白名单 + @SaCheckRole）。
 *
 * 来源文案取字典 `feedback_source`，不在前端写死 —— 字典改文案这里立即跟着变。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFeedbackList, handleFeedback } from '@/api/feedback'
import { useDictStore } from '@/stores/dict'
import { useIsMobile } from '@/composables/useIsMobile'

const dictStore = useDictStore()
const isMobile = useIsMobile()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  source: '',
  handled: '',
  page: 1,
  size: 20
})

const sourceOptions = computed(() => dictStore.cache.feedback_source || [])

const handledOptions = [
  { value: 0, label: '未处理' },
  { value: 1, label: '已处理' }
]

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

/** 空串 / null 一律不落进查询条件（避免筛出空结果） */
function hasValue(value) {
  return value !== '' && value !== null && value !== undefined
}

function sourceLabel(source) {
  return dictStore.labelOf('feedback_source', String(source)) || '未知来源'
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (hasValue(query.source)) {
      params.source = query.source
    }
    if (hasValue(query.handled)) {
      params.handled = query.handled
    }
    const data = await getFeedbackList(params)
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

function resetQuery() {
  query.source = ''
  query.handled = ''
  search()
}

async function toggleHandled(row) {
  const next = row.handled === 1 ? 0 : 1
  try {
    await handleFeedback(row.id, next)
    ElMessage.success(next === 1 ? '已标记为已处理' : '已标记为未处理')
    await load()
  } catch {
    // 提示由 axios 拦截器统一处理
  }
}

onMounted(async () => {
  await dictStore.loadMany(['feedback_source'])
  await load()
})
</script>

<template>
  <div class="page feedback">
    <div class="feedback__head">
      <h2 class="page-title">反馈列表</h2>
      <p class="page-desc">
        共 <b class="feedback__num">{{ total }}</b> 条，供上线期复盘使用（默认按提交时间新→旧）
      </p>
    </div>

    <div class="feedback__filters">
      <el-select v-model="query.source" placeholder="全部来源" clearable style="width: 150px">
        <el-option
          v-for="item in sourceOptions"
          :key="item.code"
          :label="item.label"
          :value="Number(item.code)"
        />
      </el-select>
      <el-select v-model="query.handled" placeholder="全部状态" clearable style="width: 150px">
        <el-option
          v-for="item in handledOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 桌面：表格 -->
    <el-table v-if="!isMobile" v-loading="loading" :data="list" border>
      <el-table-column prop="content" label="反馈内容" min-width="280" show-overflow-tooltip />
      <el-table-column prop="contact" label="联系方式" width="160">
        <template #default="{ row }">{{ row.contact || '—' }}</template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ sourceLabel(row.source) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ timeLabel(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.handled === 1 ? 'success' : 'warning'" size="small" effect="plain">
            {{ row.handled === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="toggleHandled(row)">
            {{ row.handled === 1 ? '标记未处理' : '标记已处理' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片流 -->
    <div v-else v-loading="loading" class="feedback__cards">
      <div v-for="row in list" :key="row.id" class="feedback__card">
        <p class="feedback__card-content">{{ row.content }}</p>
        <div class="feedback__card-meta">
          <el-tag size="small" effect="plain">{{ sourceLabel(row.source) }}</el-tag>
          <el-tag :type="row.handled === 1 ? 'success' : 'warning'" size="small" effect="plain">
            {{ row.handled === 1 ? '已处理' : '未处理' }}
          </el-tag>
          <span class="feedback__card-time">{{ timeLabel(row.createdAt) }}</span>
        </div>
        <p v-if="row.contact" class="feedback__card-contact">联系方式：{{ row.contact }}</p>
        <el-button link type="primary" size="small" @click="toggleHandled(row)">
          {{ row.handled === 1 ? '标记未处理' : '标记已处理' }}
        </el-button>
      </div>
      <p v-if="!loading && !list.length" class="feedback__empty">还没有收到反馈</p>
    </div>

    <div v-if="total > query.size" class="feedback__pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<style scoped>
.feedback__num {
  color: var(--brand-primary);
}

.feedback__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.feedback__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 80px;
}

.feedback__card {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.feedback__card-content {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.feedback__card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.feedback__card-time {
  font-size: 12px;
  color: #909399;
}

.feedback__card-contact {
  margin: 0 0 6px;
  font-size: 12px;
  color: #606266;
}

.feedback__empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}

.feedback__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
