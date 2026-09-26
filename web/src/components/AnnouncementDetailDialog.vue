<script setup>
/**
 * 公告详情弹窗（成员端列表 / 首页摘要 / 管理端预览共用）
 *
 * ⚠️ 全站唯一允许把富文本交给 `v-html` 的组件，且渲染前必须过 `sanitizeHtml`
 * （前端侧的第二道清洗，PRD F-008「前后端双重」）。
 */
import { computed } from 'vue'
import { useIsMobile } from '@/composables/useIsMobile'
import { sanitizeHtml } from '@/utils/sanitizeHtml'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  announcement: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const isMobile = useIsMobile()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const safeContent = computed(() => sanitizeHtml(props.announcement?.content || ''))

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="announcement?.title || '公告详情'"
    :width="isMobile ? '92%' : '680px'"
  >
    <div class="announce-detail__meta">
      <span>{{ announcement?.authorName || '开源鸿蒙社' }}</span>
      <span>发布于 {{ timeLabel(announcement?.createdAt) }}</span>
      <el-tag v-if="announcement?.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
    </div>

    <div v-if="safeContent" class="announce-detail__content" v-html="safeContent" />
    <p v-else class="announce-detail__empty">该公告暂无内容</p>
  </el-dialog>
</template>

<style scoped>
.announce-detail__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 10px;
  margin-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  color: #909399;
}

.announce-detail__content {
  font-size: 14px;
  line-height: 1.75;
  color: #303133;
  word-break: break-word;
}

/* 富文本内容的排版兜底（内容来自 v-html，必须用 :deep 才能命中） */
.announce-detail__content :deep(p) {
  margin: 0 0 10px;
}

.announce-detail__content :deep(h1),
.announce-detail__content :deep(h2),
.announce-detail__content :deep(h3),
.announce-detail__content :deep(h4),
.announce-detail__content :deep(h5),
.announce-detail__content :deep(h6) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.4;
}

.announce-detail__content :deep(img) {
  /* 窄屏零溢出的关键：富文本里的图片必须跟着容器缩 */
  max-width: 100%;
  height: auto;
}

.announce-detail__content :deep(a) {
  color: var(--brand-primary);
  text-decoration: none;
}

/* href 被安全策略剥掉的「残留链接」按普通文字渲染，避免看着能点其实点不动 */
.announce-detail__content :deep(a:not([href])) {
  color: inherit;
  text-decoration: none;
  cursor: default;
}

.announce-detail__content :deep(blockquote) {
  margin: 10px 0;
  padding: 6px 12px;
  border-left: 3px solid #dcdfe6;
  background: #f7f8fa;
  color: #606266;
}

.announce-detail__content :deep(pre) {
  padding: 10px;
  overflow-x: auto;
  border-radius: var(--brand-radius);
  background: #f7f8fa;
}

.announce-detail__content :deep(ul),
.announce-detail__content :deep(ol) {
  margin: 0 0 10px;
  padding-left: 22px;
}

.announce-detail__content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 10px;
}

.announce-detail__content :deep(td),
.announce-detail__content :deep(th) {
  padding: 6px 8px;
  border: 1px solid #ebeef5;
}

.announce-detail__empty {
  padding: 24px 0;
  text-align: center;
  color: #909399;
}
</style>
