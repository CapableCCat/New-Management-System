<script setup>
import { computed } from 'vue'
import { useDictStore } from '@/stores/dict'
import { useIsMobile } from '@/composables/useIsMobile'

/**
 * 成员档案详情抽屉（F-006）—— 成员端与管理端共用。
 *
 * 敏感列（手机号 / 学号）在「成员」视角下后端会置空（PRD 第五章），
 * 这里用 v-if 直接不渲染这些行，避免出现空字段。
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 成员档案（UserVO）；手机号/学号可能为 null */
  member: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue'])

const isMobile = useIsMobile()
const dictStore = useDictStore()

const canSeePhone = computed(() => !!props.member?.phone)
const canSeeStudentId = computed(() => !!props.member?.studentId)

const genderLabel = computed(() => {
  const map = { 0: '未填', 1: '男', 2: '女' }
  return map[props.member?.gender] || '未填'
})

const areaLabel = computed(() => {
  const { province, city } = props.member || {}
  return [province, city].filter(Boolean).join(' ') || '—'
})

const majorLabel = computed(() => {
  const item = props.member
  if (!item?.major) {
    return '—'
  }
  if (item.major === 'other') {
    return item.majorText ? `其他（${item.majorText}）` : '其他'
  }
  return dictStore.labelOf('major', item.major)
})

const timeLabel = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '—')

const initial = computed(() => (props.member?.name || '?').slice(0, 1))

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :size="isMobile ? '88%' : '420px'"
    title="成员档案"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="member" class="member-detail">
      <div class="member-detail__head">
        <el-avatar :size="56" :src="member.avatarUrl || undefined">
          {{ initial }}
        </el-avatar>
        <div class="member-detail__head-main">
          <p class="member-detail__name">{{ member.name }}</p>
          <p class="member-detail__tags">
            <el-tag size="small" type="info">
              {{ dictStore.labelOf('department', member.department) || '未分配部门' }}
            </el-tag>
            <el-tag size="small">
              {{ dictStore.labelOf('duty', member.duty) }}
            </el-tag>
            <el-tag v-if="member.status === 1" size="small" type="danger">已冻结</el-tag>
          </p>
        </div>
      </div>

      <section class="member-detail__block">
        <h3 class="member-detail__title">个人信息</h3>
        <div class="member-detail__row">
          <span class="member-detail__label">姓名</span>
          <span class="member-detail__value">{{ member.name }}</span>
        </div>
        <div v-if="canSeePhone" class="member-detail__row">
          <span class="member-detail__label">手机号</span>
          <span class="member-detail__value">{{ member.phone }}</span>
        </div>
        <div v-if="canSeeStudentId" class="member-detail__row">
          <span class="member-detail__label">学号</span>
          <span class="member-detail__value">{{ member.studentId }}</span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">性别</span>
          <span class="member-detail__value">{{ genderLabel }}</span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">生源地</span>
          <span class="member-detail__value">{{ areaLabel }}</span>
        </div>
      </section>

      <section class="member-detail__block">
        <h3 class="member-detail__title">专业信息</h3>
        <div class="member-detail__row">
          <span class="member-detail__label">学院</span>
          <span class="member-detail__value">
            {{ dictStore.labelOf('college', member.college) || '—' }}
          </span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">专业</span>
          <span class="member-detail__value">{{ majorLabel }}</span>
        </div>
      </section>

      <section class="member-detail__block">
        <h3 class="member-detail__title">社团信息</h3>
        <div class="member-detail__row">
          <span class="member-detail__label">部门</span>
          <span class="member-detail__value">
            {{ dictStore.labelOf('department', member.department) || '未分配' }}
          </span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">职位</span>
          <span class="member-detail__value">{{ dictStore.labelOf('duty', member.duty) }}</span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">状态</span>
          <span class="member-detail__value">
            {{ dictStore.labelOf('status', member.status) }}
          </span>
        </div>
        <div class="member-detail__row">
          <span class="member-detail__label">加入时间</span>
          <span class="member-detail__value">{{ timeLabel(member.createdAt) }}</span>
        </div>
      </section>

      <section class="member-detail__block">
        <h3 class="member-detail__title">个人简介</h3>
        <p class="member-detail__bio">{{ member.bio || '这位同学还没有填写个人简介' }}</p>
      </section>
    </div>

    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.member-detail__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.member-detail__head-main {
  min-width: 0;
  flex: 1;
}

.member-detail__name {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.member-detail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
}

.member-detail__block {
  padding: 12px 0;
  border-top: 1px solid #f2f3f5;
}

.member-detail__title {
  margin: 0 0 8px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-primary);
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.member-detail__row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 2;
}

.member-detail__label {
  flex: none;
  width: 76px;
  text-align: right;
  color: #909399;
}

.member-detail__value {
  flex: 1;
  min-width: 0;
  color: #606266;
  word-break: break-all;
}

.member-detail__bio {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
  white-space: pre-wrap;
}
</style>
