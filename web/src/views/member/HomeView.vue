<script setup>
/**
 * 成员端首页（PRD F-008 公告摘要 + §8.2 页面地图）
 *
 * 本轮只做「公告摘要」这一块（路由 meta 里首页归属 T12）：
 * 取置顶 + 最新的若干条，点击跳到公告页并直接展开详情（`/announcement?open=<id>`）。
 * 反馈入口属于 T16，快捷入口等后续任务点补齐。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAnnouncementList } from '@/api/announcement'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { canEnterAdmin } from '@/constants/roles'

const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const loading = ref(false)
const announcements = ref([])

const profile = computed(() => userStore.profile || {})
const canEnterAdminPage = computed(() => canEnterAdmin(profile.value))

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function deptLabel() {
  return dictStore.labelOf('department', profile.value.department) || '未分配'
}

function dutyLabel() {
  return dictStore.labelOf('duty', profile.value.duty) || '成员'
}

async function load() {
  loading.value = true
  try {
    // 置顶优先 + 发布时间倒序由后端保证，这里取前 5 条即可
    const data = await getAnnouncementList({ page: 1, size: 5 })
    announcements.value = data.records || []
  } finally {
    loading.value = false
  }
}

function openAnnouncement(row) {
  router.push({ path: '/announcement', query: { open: row.id } })
}

onMounted(async () => {
  await dictStore.loadMany(['department', 'duty'])
  await load()
})
</script>

<template>
  <div class="page home">
    <div class="home__welcome">
      <div class="home__welcome-main">
        <h2 class="page-title">你好，{{ profile.name || '同学' }}</h2>
        <p class="page-desc">
          {{ deptLabel() }} · {{ dutyLabel() }}
          <span v-if="!profile.department && !profile.duty">（入社信息待社长团分配）</span>
        </p>
      </div>
      <el-button v-if="canEnterAdminPage" @click="router.push('/admin/audit')">进入管理端</el-button>
    </div>

    <div class="home__section">
      <div class="home__section-head">
        <h3 class="home__section-title">公告</h3>
        <el-button link type="primary" size="small" @click="router.push('/announcement')">
          查看全部
        </el-button>
      </div>

      <div v-loading="loading" class="home__announcements">
        <div
          v-for="row in announcements"
          :key="row.id"
          class="home__announcement"
          :class="{ 'is-top': row.isTop === 1 }"
          @click="openAnnouncement(row)"
        >
          <div class="home__announcement-head">
            <el-tag v-if="row.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
            <span class="home__announcement-title">{{ row.title }}</span>
          </div>
          <p class="home__announcement-summary">{{ row.summary || '（点击查看详情）' }}</p>
          <div class="home__announcement-meta">
            <span>{{ row.authorName || '开源鸿蒙社' }}</span>
            <span>{{ timeLabel(row.createdAt) }}</span>
          </div>
        </div>
        <p v-if="!loading && !announcements.length" class="home__empty">
          暂时还没有公告，先去看看
          <el-button link type="primary" size="small" @click="router.push('/members')">成员列表</el-button>
          吧
        </p>
      </div>
    </div>

    <p class="home__todo">反馈入口与更多快捷入口将在后续任务点补充（T16）。</p>
  </div>
</template>

<style scoped>
.home__welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.home__welcome-main h2 {
  margin: 0;
}

.home__section {
  margin-top: 20px;
}

.home__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.home__section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.home__announcements {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 60px;
}

.home__announcement {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-left: 3px solid transparent;
  border-radius: var(--brand-radius);
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.home__announcement:hover {
  border-color: var(--brand-primary);
  box-shadow: 0 2px 12px rgb(0 0 0 / 6%);
}

.home__announcement.is-top {
  border-left-color: #f56c6c;
  background: #fffafa;
}

.home__announcement-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.home__announcement-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  word-break: break-word;
}

.home__announcement-summary {
  margin: 6px 0 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.home__announcement-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #909399;
}

.home__empty {
  padding: 28px 0;
  text-align: center;
  color: #909399;
}

.home__todo {
  margin-top: 20px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
