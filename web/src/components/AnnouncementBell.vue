<script setup>
/**
 * 公告通知（右上角铃铛 + 红点）—— T19
 *
 * 口径（社长拍板 + 清单 §6 D112）：
 *   - **只要有比"上次查看时间"更新的公告就点红点**（不落库、不加表，只记 localStorage）
 *   - **打开「公告」页才算已读**（`AnnouncementView` 挂载时写时间）
 *   - 铃铛面板里另给一个「**一键已读**」按钮，方便不想点进去的人
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAnnouncementList } from '@/api/announcement'
import { ROUTE_PATH } from '@/constants/app'
import { hasUnreadAnnouncement, markAnnouncementRead } from '@/utils/announcementRead'

const route = useRoute()
const router = useRouter()

const visible = ref(false)
const unread = ref(false)
const recent = ref([])

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

/** 拉最近几条公告，用「最新一条的发布时间」判断是否有未读 */
async function load() {
  try {
    const data = await getAnnouncementList({ page: 1, size: 5 })
    recent.value = data.records || []
    unread.value = hasUnreadAnnouncement(recent.value[0] && recent.value[0].createdAt)
  } catch {
    // 拿不到公告不影响其它功能：不显示红点即可
    unread.value = false
  }
}

/** 一键已读：只标记，不跳页 */
function markAllRead() {
  markAnnouncementRead()
  unread.value = false
  ElMessage.success('已把公告标记为已读')
}

function openList() {
  visible.value = false
  router.push(ROUTE_PATH.ANNOUNCEMENT)
}

/** 点某条公告：直接去公告页并展开那条详情（公告页挂载时会写已读时间） */
function openOne(row) {
  visible.value = false
  router.push({ path: ROUTE_PATH.ANNOUNCEMENT, query: { open: row.id } })
}

/** 进到公告页就算已读（红点立刻消，不用等刷新） */
watch(
  () => route.path,
  (path) => {
    if (path === ROUTE_PATH.ANNOUNCEMENT) {
      unread.value = false
    }
  }
)

watch(visible, (open) => {
  if (open) {
    load()
  }
})

onMounted(load)
</script>

<template>
  <el-popover v-model:visible="visible" placement="bottom-end" :width="300" trigger="click">
    <template #reference>
      <span class="bell" :title="unread ? '有新的公告' : '公告'">
        <svg class="bell__icon" viewBox="0 0 24 24" aria-hidden="true">
          <path
            d="M12 3a5 5 0 0 0-5 5v3.2l-1.4 2.5A1 1 0 0 0 6.5 15h11a1 1 0 0 0 .9-1.3L17 11.2V8a5 5 0 0 0-5-5Z"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linejoin="round"
          />
          <path d="M10 17.5a2 2 0 0 0 4 0" fill="none" stroke="currentColor" stroke-width="1.6" />
        </svg>
        <i v-if="unread" class="bell__dot" />
      </span>
    </template>

    <div class="notice">
      <div class="notice__head">
        <span class="notice__title">公告</span>
        <el-button link type="primary" size="small" :disabled="!unread" @click="markAllRead">
          一键已读
        </el-button>
      </div>

      <div v-if="recent.length" class="notice__list">
        <div v-for="row in recent" :key="row.id" class="notice__item" @click="openOne(row)">
          <span class="notice__item-title">{{ row.title }}</span>
          <span class="notice__item-time">{{ timeLabel(row.createdAt) }}</span>
        </div>
      </div>
      <p v-else class="notice__empty">暂时还没有公告</p>

      <div class="notice__foot">
        <el-button link type="primary" size="small" @click="openList">查看全部</el-button>
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--brand-radius);
  color: #606266;
  cursor: pointer;
}

.bell:hover {
  background: #f5f7fa;
}

.bell__icon {
  width: 18px;
  height: 18px;
}

/* 红点：绝对定位在铃铛右上角 */
.bell__dot {
  position: absolute;
  top: 4px;
  right: 5px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
}

.notice__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.notice__title {
  font-size: 13px;
  font-weight: 500;
}

.notice__list {
  display: flex;
  flex-direction: column;
}

.notice__item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 7px 0;
  border-top: 1px solid #f2f3f5;
  font-size: 13px;
  cursor: pointer;
}

.notice__item:hover .notice__item-title {
  color: var(--brand-primary);
}

.notice__item-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.notice__item-time {
  flex: none;
  font-size: 12px;
  color: #909399;
}

.notice__empty {
  padding: 12px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.notice__foot {
  display: flex;
  justify-content: flex-end;
  padding-top: 6px;
  border-top: 1px solid #f2f3f5;
}
</style>
