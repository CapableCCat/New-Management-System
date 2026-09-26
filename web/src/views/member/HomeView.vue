<script setup>
/**
 * 工作台（内部场景落地页）—— T19
 *
 * 首页**不是内容门面**（清单 §6 D107）：原来那张公告摘要卡**撤掉**（公告走「公告」菜单 +
 * 右上角铃铛红点），这里改为**按角色的待办与入口**。
 *
 * 「谁能看到什么」全部走 `constants/roles.js` 的具名能力函数 —— 与路由菜单、守卫同源。
 * 成员没有管理资格时，这里连 `/recruit/admin/stats` 都不会去请求（否则会拿到 40300）。
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getApplyStats } from '@/api/recruit'
import { getFeedbackList } from '@/api/feedback'
import {
  canExportData,
  canImportMembers,
  canManageAll,
  canReviewRecruit,
  canViewFeedback
} from '@/constants/roles'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import FeedbackDialog from '@/components/FeedbackDialog.vue'

const router = useRouter()
const dictStore = useDictStore()
const userStore = useUserStore()

const profile = computed(() => userStore.profile || {})
const loadingStats = ref(false)
/** 待审报名数：null = 不适用或没拿到 */
const pendingCount = ref(null)
/** 未处理反馈数（仅社长团 / 超管去取） */
const unhandledFeedback = ref(null)
const loadingFeedback = ref(false)
const feedbackVisible = ref(false)

const showReview = computed(() => canReviewRecruit(profile.value))
const showImport = computed(() => canImportMembers(profile.value))
const showExport = computed(() => canExportData(profile.value))
const showFeedback = computed(() => canViewFeedback(profile.value))
const fullScope = computed(() => canManageAll(profile.value))

/** 资料完整度：列出还没填的项（学号 / 生源地 / 个人简介） */
const missingFields = computed(() => {
  const p = profile.value
  const missing = []
  if (!p.studentId) {
    missing.push('学号')
  }
  if (!p.province) {
    missing.push('生源地')
  }
  if (!p.bio) {
    missing.push('个人简介')
  }
  return missing
})

const deptLabel = computed(
  () => dictStore.labelOf('department', profile.value.department) || '未分配'
)

const dutyLabel = computed(() => dictStore.labelOf('duty', profile.value.duty) || '成员')

function go(path) {
  router.push(path)
}

onMounted(async () => {
  await dictStore.loadMany(['department', 'duty'])
  if (showReview.value) {
    loadingStats.value = true
    try {
      const data = await getApplyStats()
      pendingCount.value = data ? data.pending : null
    } catch {
      pendingCount.value = null
    } finally {
      loadingStats.value = false
    }
  }
  if (showFeedback.value) {
    loadingFeedback.value = true
    try {
      // 只要 total：拿一页一条即可，不必把内容都拉回来
      const data = await getFeedbackList({ handled: 0, page: 1, size: 1 })
      unhandledFeedback.value = data ? data.total : null
    } catch {
      unhandledFeedback.value = null
    } finally {
      loadingFeedback.value = false
    }
  }
})
</script>

<template>
  <div class="page workbench">
    <section class="workbench__welcome">
      <div class="workbench__welcome-main">
        <h2 class="page-title">你好，{{ profile.name || '同学' }}</h2>
        <p class="page-desc">
          {{ deptLabel }} · {{ dutyLabel }}
          <span v-if="!profile.department && !profile.duty">（入社信息待社长团分配）</span>
        </p>
      </div>
      <el-button v-if="showReview" type="primary" @click="go('/admin/audit')">去审核台</el-button>
    </section>

    <section class="workbench__section">
      <h3 class="workbench__section-title">待办与入口</h3>

      <div class="workbench__grid">
        <!-- 待审报名（部长起）：数字按可见范围统计（部长=本部门） -->
        <div
          v-if="showReview"
          class="workbench__card is-action"
          @click="go('/admin/audit')"
        >
          <div class="workbench__card-label">待审报名</div>
          <div class="workbench__card-value">
            {{ loadingStats || pendingCount === null ? '—' : pendingCount }}
          </div>
          <div class="workbench__card-hint">
            {{ fullScope ? '全社范围' : '本部门范围' }} · 点击去审核
          </div>
        </div>

        <!-- 资料完整度：全员 -->
        <div class="workbench__card is-action" @click="go('/profile')">
          <div class="workbench__card-label">资料完整度</div>
          <div class="workbench__card-value">
            {{ missingFields.length ? `缺 ${missingFields.length} 项` : '已完善' }}
          </div>
          <div class="workbench__card-hint">
            {{
              missingFields.length
                ? `待补：${missingFields.join(' / ')}`
                : '资料齐全，可在个人中心随时修改'
            }}
          </div>
        </div>

        <!-- Excel 导入（社长团 / 超管） -->
        <div v-if="showImport" class="workbench__card is-action" @click="go('/admin/import')">
          <div class="workbench__card-label">Excel 导入</div>
          <div class="workbench__card-value">批量建号</div>
          <div class="workbench__card-hint">下载模板 → 上传 → 一次性密码清单</div>
        </div>

        <!-- 数据导出（社长团 / 超管）：导出按钮在成员档案页与审核台 -->
        <div v-if="showExport" class="workbench__card is-action" @click="go('/admin/members')">
          <div class="workbench__card-label">数据导出</div>
          <div class="workbench__card-value">成员名册 / 报名数据</div>
          <div class="workbench__card-hint">在成员档案页与审核台按当前筛选导出</div>
        </div>

        <!-- 意见反馈（T16）：全员可提交；公开页（报名结果态）另有免登录入口 -->
        <div class="workbench__card is-action" @click="feedbackVisible = true">
          <div class="workbench__card-label">意见反馈</div>
          <div class="workbench__card-value">说点什么</div>
          <div class="workbench__card-hint">用着不顺手、想看什么功能，都可以直接提</div>
        </div>

        <!-- 反馈待查看（社长团 / 超管）：复盘的输入 -->
        <div
          v-if="showFeedback"
          class="workbench__card is-action"
          @click="go('/admin/feedback')"
        >
          <div class="workbench__card-label">反馈待查看</div>
          <div class="workbench__card-value">
            {{ loadingFeedback || unhandledFeedback === null ? '—' : unhandledFeedback }}
          </div>
          <div class="workbench__card-hint">未处理的反馈条数 · 点击去看</div>
        </div>
      </div>

      <p v-if="!showReview && !showImport && !showExport" class="workbench__calm">
        暂时没有待办事项，去
        <el-button link type="primary" size="small" @click="go('/announcement')">公告</el-button>
        或
        <el-button link type="primary" size="small" @click="go('/members')">成员列表</el-button>
        逛逛吧。
      </p>
    </section>

    <!-- 来源 2 = 成员端（字典 feedback_source） -->
    <FeedbackDialog v-model="feedbackVisible" :source="2" />
  </div>
</template>

<style scoped>
.workbench__welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.workbench__welcome-main :deep(.page-title) {
  margin: 0;
}

.workbench__section {
  margin-top: 20px;
}

.workbench__section-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.workbench__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.workbench__card {
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.workbench__card.is-action {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.workbench__card.is-action:hover {
  border-color: var(--brand-primary);
  box-shadow: 0 2px 12px rgb(0 0 0 / 6%);
}

.workbench__card-label {
  font-size: 13px;
  color: #909399;
}

.workbench__card-value {
  margin: 6px 0 4px;
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  word-break: break-word;
}

.workbench__card-hint {
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.workbench__calm {
  margin: 14px 0 0;
  font-size: 13px;
  color: #909399;
}
</style>
