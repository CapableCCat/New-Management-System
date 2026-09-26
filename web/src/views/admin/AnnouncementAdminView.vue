<script setup>
/**
 * 公告管理（PRD F-008）
 *
 * 权限要点（PRD 第五章权限矩阵「发布公告」）：超管 / 社长团 / 部长均可发布、编辑、删除任意公告。
 * 为什么不做「只能改自己发的」：社团规模小、公告本身就是公共信息，
 * 加上这条限制反而会出现「发公告的人毕业了，没人能改」的死角（§6 D82）。
 *
 * 排序恒为「置顶优先 + 发布时间倒序」，由后端决定；本页不提供排序交互。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncementDetail,
  getAnnouncementList,
  updateAnnouncement
} from '@/api/announcement'
import { hasVisibleContent, sanitizeHtml } from '@/utils/sanitizeHtml'
import { useIsMobile } from '@/composables/useIsMobile'
import RichTextEditor from '@/components/RichTextEditor.vue'
import AnnouncementDetailDialog from '@/components/AnnouncementDetailDialog.vue'

const isMobile = useIsMobile()

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
    await ElMessageBox.confirm(
      `确定删除公告「${row.title}」吗？删除后成员端将不再可见。`,
      '提示',
      {
        type: 'warning',
        // 显式给按钮文案：Element Plus 的内置文案默认是英文（全局未配 zh-cn locale），
        // 不写死在页面上的话，删除确认框会出现「OK / Cancel」（见 §6 D85）
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
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

onMounted(load)
</script>

<template>
  <div class="page announce-admin">
    <div class="announce-admin__head">
      <div>
        <h2 class="page-title">公告管理</h2>
        <p class="page-desc">
          共 <b class="announce-admin__num">{{ total }}</b> 条公告，置顶公告始终排在列表最前
        </p>
      </div>
      <div class="announce-admin__head-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">发布公告</el-button>
      </div>
    </div>

    <div class="announce-admin__filters">
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
          <span v-else class="announce-admin__dim">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="摘要" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="announce-admin__summary">{{ row.summary || '（无文字内容）' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发布人" width="110">
        <template #default="{ row }">{{ row.authorName || '—' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" width="140">
        <template #default="{ row }">{{ timeLabel(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="announce-admin__cards">
      <div v-for="row in list" :key="row.id" class="announce-admin__card">
        <div class="announce-admin__card-head">
          <span class="announce-admin__card-title" @click="openDetail(row)">{{ row.title }}</span>
          <el-tag v-if="row.isTop === 1" type="danger" size="small">置顶</el-tag>
        </div>
        <p class="announce-admin__card-summary">{{ row.summary || '（无文字内容）' }}</p>
        <div class="announce-admin__card-meta">
          {{ row.authorName || '—' }} · {{ timeLabel(row.createdAt) }}
        </div>
        <div class="announce-admin__card-actions">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </div>
      </div>
      <p v-if="!loading && !list.length" class="announce-admin__empty">暂无公告，点「发布公告」写第一条吧</p>
    </div>

    <div v-if="total > query.size" class="announce-admin__pager">
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

    <!-- 发布 / 编辑弹窗 -->
    <el-dialog
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
          <span class="announce-admin__tip">置顶后始终排在成员端列表最前</span>
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
.announce-admin__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.announce-admin__head-actions {
  display: flex;
  gap: 8px;
  flex: none;
}

.announce-admin__num {
  color: var(--brand-primary);
}

.announce-admin__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 12px 0 16px;
  padding: 12px;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.announce-admin__summary {
  color: #606266;
}

.announce-admin__dim {
  color: #c0c4cc;
}

.announce-admin__tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.announce-admin__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.announce-admin__card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.announce-admin__card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.announce-admin__card-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.announce-admin__card-summary {
  margin: 6px 0 4px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.announce-admin__card-meta {
  font-size: 12px;
  color: #909399;
}

.announce-admin__card-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.announce-admin__empty {
  padding: 32px 0;
  text-align: center;
  color: #909399;
}

.announce-admin__pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

@media (max-width: 768px) {
  .announce-admin__head {
    flex-direction: column;
  }

  .announce-admin__head-actions {
    flex-wrap: wrap;
  }
}
</style>
