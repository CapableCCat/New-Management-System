<script setup>
/**
 * 公告列表 / 详情（成员端，PRD F-008）
 *
 * 只读：没有发布/编辑/删除入口（PRD 权限矩阵里普通成员对「发布公告」是 —）。
 * 详情用弹窗承载而不是独立路由 —— 成员端手机为主，弹窗少一次路由跳转、
 * 也不会让底部导航的选中态在 /announcement/:id 上落空。
 *
 * 支持 `?open=<id>`：首页点公告摘要时带过来，进页面直接展开那条详情。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnnouncementDetail, getAnnouncementList } from '@/api/announcement'
import AnnouncementDetailDialog from '@/components/AnnouncementDetailDialog.vue'
import { markAnnouncementRead } from '@/utils/announcementRead'

const route = useRoute()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const detail = ref(null)

const isEmpty = computed(() => !loading.value && !list.value.length)

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword.trim()) {
      params.keyword = query.keyword.trim()
    }
    const data = await getAnnouncementList(params)
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

async function openDetail(row) {
  try {
    detail.value = await getAnnouncementDetail(row.id)
    detailVisible.value = true
  } catch {
    ElMessage.warning('读取公告失败')
  }
}

/** 首页带 `?open=<id>` 进来时直接展开详情（公告可能不在第一页，故按 id 单独取） */
async function openFromQuery() {
  const id = route.query.open
  if (!id) {
    return
  }
  try {
    detail.value = await getAnnouncementDetail(id)
    detailVisible.value = true
  } catch {
    // 公告可能已被删除，静默忽略即可
  }
}

watch(() => route.query.open, openFromQuery)

onMounted(async () => {
  // 打开公告页即视为已读（右上角铃铛红点据此消除，见 utils/announcementRead）
  markAnnouncementRead()
  await load()
  await openFromQuery()
})
</script>

<template>
  <div class="page announce-list">
    <div class="announce-list__head">
      <h2 class="page-title">公告</h2>
      <p class="page-desc">
        共 <b class="announce-list__num">{{ total }}</b> 条公告，置顶公告排在最前
      </p>
    </div>

    <div class="announce-list__filters">
      <el-input
        v-model="query.keyword"
        placeholder="搜索标题"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <div v-loading="loading" class="announce-list__items">
      <div
        v-for="row in list"
        :key="row.id"
        class="announce-list__item"
        :class="{ 'is-top': row.isTop === 1 }"
        @click="openDetail(row)"
      >
        <div class="announce-list__item-head">
          <el-tag v-if="row.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
          <span class="announce-list__title">{{ row.title }}</span>
        </div>
        <p class="announce-list__summary">{{ row.summary || '（点击查看详情）' }}</p>
        <div class="announce-list__meta">
          <span>{{ row.authorName || '开源鸿蒙社' }}</span>
          <span>{{ timeLabel(row.createdAt) }}</span>
        </div>
      </div>
      <p v-if="isEmpty" class="announce-list__empty">暂时还没有公告</p>
    </div>

    <div v-if="total > query.size" class="announce-list__pager">
      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>

    <AnnouncementDetailDialog v-model="detailVisible" :announcement="detail" />
  </div>
</template>

<style scoped>
.announce-list__num {
  color: var(--brand-primary);
}

.announce-list__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.announce-list__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 80px;
}

.announce-list__item {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-left: 3px solid transparent;
  border-radius: var(--brand-radius);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.announce-list__item:hover {
  border-color: var(--brand-primary);
  box-shadow: 0 2px 12px rgb(0 0 0 / 6%);
}

.announce-list__item.is-top {
  border-left-color: #f56c6c;
  background: #fffafa;
}

.announce-list__item-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.announce-list__title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  word-break: break-word;
}

.announce-list__summary {
  margin: 6px 0 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.announce-list__meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #909399;
}

.announce-list__empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}

.announce-list__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
