<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateProfile, updateStudentId, uploadAvatar } from '@/api/user'
import { ROUTE_PATH } from '@/constants/app'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useIsMobile } from '@/composables/useIsMobile'

/**
 * 个人中心（PRD F-007）
 *
 * 边界（按社长确认的口径）：
 *   - 可自助修改：学院 / 专业 / 性别 / 生源地 / 个人简介 / 头像
 *   - **姓名、手机号不可自助修改**，只能联系管理员（干部走成员档案 T10）
 *   - 学号仅在为空时可自助补录；已有学号要改需找管理员
 *   - 修改密码复用 T4 的 /change-password 页（验旧密码 + 成功后强制登出）
 */
const router = useRouter()
const isMobile = useIsMobile()
const dictStore = useDictStore()
const userStore = useUserStore()

const MAX_AVATAR_SIZE = 2 * 1024 * 1024
const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png']

const loading = ref(false)
const me = ref(null)
const fileInput = ref(null)
const avatarUploading = ref(false)

const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  college: '',
  major: '',
  majorText: '',
  gender: 0,
  province: '',
  city: '',
  bio: ''
})

const studentVisible = ref(false)
const studentSaving = ref(false)
const studentIdInput = ref('')

const colleges = computed(() => dictStore.cache.college || [])
const majors = computed(() => dictStore.cache.major || [])
const isOtherMajor = computed(() => editForm.major === 'other')
const canFillStudentId = computed(() => !me.value?.studentId)

const majorLabel = computed(() => {
  const item = me.value
  if (!item?.major) {
    return '—'
  }
  if (item.major === 'other') {
    return item.majorText ? `其他（${item.majorText}）` : '其他'
  }
  return dictStore.labelOf('major', item.major)
})

const areaLabel = computed(() => {
  const { province, city } = me.value || {}
  return [province, city].filter(Boolean).join(' ') || '—'
})

const genderLabel = computed(() => ({ 0: '未填', 1: '男', 2: '女' })[me.value?.gender] || '未填')

function timeLabel(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '—'
}

function initial(name) {
  return (name || '?').slice(0, 1)
}

async function load() {
  loading.value = true
  try {
    const data = await getCurrentUser()
    me.value = data
    // 同步到登录态缓存，顶部/其它页面立刻用上新头像与资料
    userStore.setProfile(data)
  } finally {
    loading.value = false
  }
}

/* ---------------- 编辑资料 ---------------- */

function openEdit() {
  const item = me.value || {}
  Object.assign(editForm, {
    college: item.college || '',
    major: item.major || '',
    majorText: item.majorText || '',
    gender: item.gender ?? 0,
    province: item.province || '',
    city: item.city || '',
    bio: item.bio || ''
  })
  editVisible.value = true
}

async function submitEdit() {
  if (isOtherMajor.value && !editForm.majorText.trim()) {
    ElMessage.warning('选择「其他」专业时，请填写具体专业名称')
    return
  }
  editSaving.value = true
  try {
    const data = await updateProfile({
      college: editForm.college || null,
      major: editForm.major || null,
      majorText: isOtherMajor.value ? editForm.majorText.trim() : null,
      gender: editForm.gender,
      province: editForm.province.trim(),
      city: editForm.city.trim(),
      bio: editForm.bio.trim()
    })
    me.value = data
    userStore.setProfile(data)
    ElMessage.success('保存成功')
    editVisible.value = false
  } catch {
    // 提示由 axios 拦截器统一处理，弹窗保留便于修正
  } finally {
    editSaving.value = false
  }
}

/* ---------------- 学号补录 ---------------- */

function openStudentId() {
  studentIdInput.value = ''
  studentVisible.value = true
}

async function submitStudentId() {
  const value = studentIdInput.value.trim()
  if (!value) {
    ElMessage.warning('请填写学号')
    return
  }
  studentSaving.value = true
  try {
    const data = await updateStudentId(value)
    me.value = data
    userStore.setProfile(data)
    ElMessage.success('学号已保存')
    studentVisible.value = false
  } catch {
    // 冲突提示由拦截器弹出（该学号已被其他成员使用）
  } finally {
    studentSaving.value = false
  }
}

/* ---------------- 头像 ---------------- */

function pickAvatar() {
  fileInput.value?.click()
}

async function onFileChange(event) {
  const file = event.target.files?.[0]
  // 复位 input，否则连续选同一个文件不会再触发 change
  event.target.value = ''
  if (!file) {
    return
  }
  if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
    ElMessage.warning('头像仅支持 jpg / png 格式')
    return
  }
  if (file.size > MAX_AVATAR_SIZE) {
    ElMessage.warning('头像大小不能超过 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const data = await uploadAvatar(file)
    me.value = data
    userStore.setProfile(data)
    ElMessage.success('头像已更新')
  } catch {
    // 拦截器已提示
  } finally {
    avatarUploading.value = false
  }
}

onMounted(async () => {
  await dictStore.loadMany(['college', 'major', 'department', 'duty', 'status'])
  await load()
})
</script>

<template>
  <div class="page profile" v-loading="loading">
    <!-- 头像与概要 -->
    <section class="profile__head">
      <div class="profile__avatar" :class="{ 'is-busy': avatarUploading }" @click="pickAvatar">
        <el-avatar :size="88" :src="me?.avatarUrl || undefined">{{ initial(me?.name) }}</el-avatar>
        <span class="profile__avatar-mask">{{ avatarUploading ? '上传中…' : '更换头像' }}</span>
      </div>
      <input
        ref="fileInput"
        class="profile__file"
        type="file"
        accept="image/jpeg,image/png"
        @change="onFileChange"
      />

      <div class="profile__head-main">
        <p class="profile__name">{{ me?.name || '—' }}</p>
        <p class="profile__tags">
          <el-tag size="small" type="info">
            {{ dictStore.labelOf('department', me?.department) || '未分配部门' }}
          </el-tag>
          <el-tag size="small">{{ dictStore.labelOf('duty', me?.duty) }}</el-tag>
        </p>
        <p class="profile__hint">支持 jpg / png，大小不超过 2MB</p>
      </div>

      <div class="profile__head-actions">
        <el-button type="primary" @click="openEdit">编辑资料</el-button>
        <el-button @click="router.push(ROUTE_PATH.CHANGE_PASSWORD)">修改密码</el-button>
      </div>
    </section>

    <!-- 三块资料卡 -->
    <div class="profile__grid">
      <section class="profile__block">
        <h3 class="profile__title">个人信息</h3>
        <div class="profile__row">
          <span class="profile__label">姓名</span>
          <span class="profile__value">{{ me?.name || '—' }}</span>
        </div>
        <div class="profile__row">
          <span class="profile__label">手机号</span>
          <span class="profile__value">{{ me?.phone || '—' }}</span>
        </div>
        <div class="profile__row">
          <span class="profile__label">学号</span>
          <span class="profile__value">
            <template v-if="me?.studentId">{{ me.studentId }}</template>
            <template v-else>
              <span class="profile__muted">未填写</span>
              <el-button v-if="canFillStudentId" link type="primary" @click="openStudentId">
                去补录
              </el-button>
            </template>
          </span>
        </div>
        <div class="profile__row">
          <span class="profile__label">性别</span>
          <span class="profile__value">{{ genderLabel }}</span>
        </div>
        <div class="profile__row">
          <span class="profile__label">生源地</span>
          <span class="profile__value">{{ areaLabel }}</span>
        </div>
      </section>

      <section class="profile__block">
        <h3 class="profile__title">专业信息</h3>
        <div class="profile__row">
          <span class="profile__label">学院</span>
          <span class="profile__value">
            {{ dictStore.labelOf('college', me?.college) || '—' }}
          </span>
        </div>
        <div class="profile__row">
          <span class="profile__label">专业</span>
          <span class="profile__value">{{ majorLabel }}</span>
        </div>
      </section>

      <section class="profile__block">
        <h3 class="profile__title">社团信息</h3>
        <div class="profile__row">
          <span class="profile__label">部门</span>
          <span class="profile__value">
            {{ dictStore.labelOf('department', me?.department) || '未分配' }}
          </span>
        </div>
        <div class="profile__row">
          <span class="profile__label">职位</span>
          <span class="profile__value">{{ dictStore.labelOf('duty', me?.duty) }}</span>
        </div>
        <div class="profile__row">
          <span class="profile__label">状态</span>
          <span class="profile__value">{{ dictStore.labelOf('status', me?.status) }}</span>
        </div>
        <div class="profile__row">
          <span class="profile__label">加入时间</span>
          <span class="profile__value">{{ timeLabel(me?.createdAt) }}</span>
        </div>
      </section>
    </div>

    <!-- 个人简介 -->
    <section class="profile__block profile__bio-block">
      <h3 class="profile__title">个人简介</h3>
      <p v-if="me?.bio" class="profile__bio">{{ me.bio }}</p>
      <p v-else class="profile__muted">还没有填写个人简介，点右上角「编辑资料」写一句吧</p>
    </section>

    <!-- 编辑资料弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="编辑资料"
      :width="isMobile ? '92%' : '480px'"
      :close-on-click-modal="false"
    >
      <el-form label-width="84px">
        <el-form-item label="姓名">
          <el-input :model-value="me?.name" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input :model-value="me?.phone" disabled />
        </el-form-item>
        <el-form-item label="学院">
          <el-select v-model="editForm.college" clearable placeholder="未填写">
            <el-option v-for="item in colleges" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="editForm.major" clearable placeholder="未填写">
            <el-option v-for="item in majors" :key="item.code" :label="item.label" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isOtherMajor" label="专业名称">
          <el-input v-model="editForm.majorText" maxlength="64" placeholder="请填写具体专业" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">不填</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生源地">
          <div class="profile__area">
            <el-input v-model="editForm.province" placeholder="省 / 直辖市" maxlength="32" />
            <el-input v-model="editForm.city" placeholder="市" maxlength="32" />
          </div>
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            maxlength="500"
            show-word-limit
            placeholder="介绍一下自己的方向与兴趣"
          />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          姓名与手机号不可自助修改；如需变更请联系社长团或管理员。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 学号补录弹窗 -->
    <el-dialog
      v-model="studentVisible"
      title="补录学号"
      :width="isMobile ? '92%' : '400px'"
      :close-on-click-modal="false"
    >
      <el-form label-width="60px">
        <el-form-item label="学号">
          <el-input v-model="studentIdInput" maxlength="32" placeholder="请输入本人学号" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          学号用于评优与学校系统对接，请如实填写；补录后如需修改请联系管理员。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="studentVisible = false">取消</el-button>
        <el-button type="primary" :loading="studentSaving" @click="submitStudentId">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile__head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.profile__avatar {
  position: relative;
  flex: none;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}

.profile__avatar-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 2px 0;
  background: rgb(0 0 0 / 55%);
  font-size: 11px;
  color: #fff;
  text-align: center;
}

.profile__avatar.is-busy {
  cursor: progress;
}

.profile__file {
  display: none;
}

.profile__head-main {
  flex: 1;
  min-width: 0;
}

.profile__name {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.profile__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0 0 6px;
}

.profile__hint {
  margin: 0;
  font-size: 12px;
  color: #c0c4cc;
}

.profile__head-actions {
  display: flex;
  flex: none;
  gap: 8px;
}

.profile__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.profile__block {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.profile__bio-block {
  margin-top: 12px;
}

.profile__title {
  margin: 0 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--brand-primary);
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.profile__row {
  display: flex;
  gap: 8px;
  font-size: 13px;
  line-height: 2;
}

.profile__label {
  flex: none;
  width: 68px;
  text-align: right;
  color: #909399;
}

.profile__value {
  flex: 1;
  min-width: 0;
  color: #606266;
  word-break: break-all;
}

.profile__muted {
  font-size: 13px;
  color: #c0c4cc;
}

.profile__bio {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: #606266;
  white-space: pre-wrap;
}

.profile__area {
  display: flex;
  gap: 8px;
  width: 100%;
}

@media (max-width: 768px) {
  .profile__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile__head-actions {
    width: 100%;
  }

  .profile__head-actions .el-button {
    flex: 1;
  }
}
</style>
