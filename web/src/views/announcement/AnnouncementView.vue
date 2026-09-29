<script setup>
/**
 * 公告（PRD F-008）—— **T22 合并版**
 *
 * 原来有「公告」（成员只读）和「公告管理」（干部可写）两个菜单，功能重复。
 * T22 合并为**一个菜单项 + 一个页面**，进入后按权限显示：
 *   - 成员：只读列表 + 详情
 *   - 部长及以上：多出「发布公告」「编辑」「删除」（PRD 第五章权限矩阵「发布公告」）
 *
 * 为什么发布权给到部长：社团规模小、公告本身就是公共信息；
 * 限制「只能改自己发的」反而会出现「发公告的人毕业了，没人能改」的死角（§6 D82）。
 *
 * 本页同时承接两件事（原在成员版里，合并后不能丢）：
 *   1. `?open=<id>` 深链 —— 右上角铃铛点某条公告时直接展开详情（公告可能在第二页，故按 id 单独取）
 *   2. 打开即视为已读 —— 铃铛红点据此消除（`utils/announcementRead`）
 *
 * 排序恒为「置顶优先 + 发布时间倒序」，由后端决定；本页不提供排序交互。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncementDetail,
  getAnnouncementList,
  updateAnnouncement
} from '@/api/announcement'
import { canManageAnnouncement } from '@/constants/roles'
import { hasVisibleContent, sanitizeHtml } from '@/utils/sanitizeHtml'
import { markAnnouncementRead } from '@/utils/announcementRead'
import { useIsMobile } from '@/composables/useIsMobile'
import { useUserStore } from '@/stores/user'
import RichTextEditor from '@/components/RichTextEditor.vue'
import AnnouncementDetailDialog from '@/components/AnnouncementDetailDialog.vue'

const route = useRoute()
const userStore = useUserStore()
const isMobile = useIsMobile()

/** 能否发布/编辑/删除（与菜单、守卫共用同一份能力判定） */
const canManage = computed(() => canManageAnnouncement(userStore.profile))

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

const editVisible = ref(false)
const editSaving = ref(false)
const editId = ref(null)
const editForm = reactive({
  title: '',
  content: '',
  isTop: false
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

function reset() {
  query.keyword = ''
  query.page = 1
  load()
}

function openCreate() {
  editId.value = null
  Object.assign(editForm, { title: '', content: '', isTop: false })
  editVisible.value = true
}

async function openEdit(row) {
  try {
    const data = await getAnnouncementDetail(row.id)
    editId.value = data.id
    Object.assign(editForm, {
      title: data.title || '',
      // 详情接口返回的是**已拼好地址**的正文，直接交给编辑器即可
      content: data.content || '',
      isTop: data.isTop === 1
    })
    editVisible.value = true
  } catch {
    ElMessage.warning('读取公告失败')
  }
}

async function openDetail(row) {
  try {
    detail.value = await getAnnouncementDetail(row.id)
    detailVisible.value = true
  } catch {
    ElMessage.warning('读取公告失败')
  }
}

/** 从铃铛带 `?open=<id>` 进来时直接展开详情（公告可能不在第一页，故按 id 单独取） */
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

async function submit() {
  const title = editForm.title.trim()
  if (!title) {
    ElMessage.warning('请填写公告标题')
    return
  }
  // 提交前的前端清洗（后端还会再清洗一次，两道都要有）
  const content = sanitizeHtml(editForm.content)
  if (!hasVisibleContent(content)) {
    ElMessage.warning('请填写公告内容')
    return
  }

  const payload = { title, content, isTop: editForm.isTop }
  editSaving.value = true
  try {
    if (editId.value) {
      await updateAnnouncement(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('发布成功')
    }
    editVisible.value = false
    query.page = 1
    await load()
  } catch {
    // 提示由 axios 拦截器统一处理，弹窗保留便于修正
  } finally {
    editSaving.value = false
  }
}

async function remove(row) {
  try {
    // 按钮文案交给组件库的 locale（中文环境显示「确定 / 取消」，英文环境显示「OK / Cancel」），
    // 不再写死中文 —— 全局已配 ElConfigProvider，见 stores/locale.js
    await ElMessageBox.confirm(
      `确定删除公告「${row.title}」吗？删除后成员端将不再可见。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteAnnouncement(row.id)
    ElMessage.success('已删除')
    // 删掉当前页最后一条时回退一页，避免停在空页
    if (list.value.length === 1 && query.page > 1) {
      query.page -= 1
    }
    await load()
  } catch {
    // 提示由拦截器处理
  }
}

onMounted(async () => {
  // 打开公告页即视为已读（右上角铃铛红点据此消除，见 utils/announcementRead）
  markAnnouncementRead()
  await load()
  await openFromQuery()
})
</script>

<template>
  <div class="page announce">
    <div class="announce__head">
      <div>
        <h2 class="page-title">公告</h2>
        <p class="page-desc">
          共 <b class="announce__num">{{ total }}</b> 条公告，置顶公告始终排在列表最前
        </p>
      </div>
      <div class="announce__head-actions">
        <el-button @click="load">刷新</el-button>
        <!-- 只有能发布的角色才看到「发布公告」 -->
        <el-button v-if="canManage" type="primary" @click="openCreate">发布公告</el-button>
      </div>
    </div>

    <div class="announce__filters">
      <el-input
        v-model="query.keyword"
        placeholder="标题关键字"
        clearable
        style="width: 220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 桌面：表格 -->
    <el-table v-if="!isMobile" v-loading="loading" :data="list" border stripe>
      <el-table-column label="置顶" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isTop === 1" type="danger" size="small">置顶</el-tag>
          <span v-else class="announce__dim">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="摘要" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="announce__summary">{{ row.summary || '（无文字内容）' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发布人" width="110">
        <template #default="{ row }">{{ row.authorName || '—' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" width="140">
        <template #default="{ row }">{{ timeLabel(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" :width="canManage ? 170 : 80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <template v-if="canManage">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="announce__cards">
      <div v-for="row in list" :key="row.id" class="announce__card">
        <div class="announce__card-head">
          <span class="announce__card-title" @click="openDetail(row)">{{ row.title }}</span>
          <el-tag v-if="row.isTop === 1" type="danger" size="small">置顶</el-tag>
        </div>
        <p class="announce__card-summary">{{ row.summary || '（无文字内容）' }}</p>
        <div class="announce__card-meta">
          {{ row.authorName || '—' }} · {{ timeLabel(row.createdAt) }}
        </div>
        <div class="announce__card-actions">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <template v-if="canManage">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
          </template>
        </div>
      </div>
      <p v-if="isEmpty" class="announce__empty">
        {{ canManage ? '暂无公告，点「发布公告」写第一条吧' : '暂无公告' }}
      </p>
    </div>

    <div v-if="total > query.size" class="announce__pager">
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

    <!-- 发布 / 编辑弹窗（仅能发布的角色会打开它） -->
    <el-dialog
      v-if="canManage"
      v-model="editVisible"
      :title="editId ? '编辑公告' : '发布公告'"
      :width="isMobile ? '96%' : '760px'"
      :close-on-click-modal="false"
    >
      <el-form label-width="72px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="128" show-word-limit placeholder="一句话说清公告主题" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="editForm.isTop" />
          <span class="announce__tip">置顶后始终排在成员端列表最前</span>
        </el-form-item>
        <el-form-item label="内容">
          <RichTextEditor v-model="editForm.content" :height="isMobile ? '260px' : '340px'" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          支持标题、加粗、列表、引用、链接、表格；图片请用工具栏的「上传图片」（jpg / png、≤2MB）。
          站外图片与脚本会被安全策略过滤，正文最终以服务端清洗结果为准。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submit">
          {{ editId ? '保存' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.announce__head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  justify-content: space-between;
}

.announce__num {
  color: var(--brand-primary);
}

.announce__head-actions {
  display: flex;
  gap: 8px;
}

.announce__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.announce__dim {
  color: var(--text-placeholder);
}

.announce__summary {
  color: var(--text-regular);
}

.announce__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.announce__card {
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-popup);
  background: var(--bg-surface);
}

.announce__card-head {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.announce__card-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--brand-primary);
}

.announce__card-summary {
  margin: 6px 0 4px;
  font-size: 13px;
  color: var(--text-regular);
}

.announce__card-meta {
  font-size: 12px;
  color: var(--text-secondary);
}

.announce__card-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.announce__empty {
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.announce__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.announce__tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
