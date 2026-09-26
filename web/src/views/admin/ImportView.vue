<script setup>
/**
 * Excel 批量导入（PRD F-009）
 *
 * 流程：下载模板 → 填好上传 → 逐行校验并建号 → 结果页给出「成功清单（含初始密码）」与「错误行清单」。
 *
 * 权限：PRD 权限矩阵里「Excel 导入」只给**社长团 / 超管**（部长没有）——
 * 路由 meta.leaderGroup + 后端 @SaCheckRole 双重把关。
 *
 * 两个有意为之的设计：
 *   1. **一步到位**，不做"先预览再确认"：手机号唯一 + 重复行跳过，
 *      同一份文件重跑不会重复建号（天然幂等），误操作代价可控。
 *   2. **初始密码不落库**（同 T7 口径）：只在本次结果里显示一次，可复制 / 下载 CSV。
 */
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadImportTemplate, importMembers } from '@/api/member'
import { copyText } from '@/utils/sms'
import { downloadCsv, today } from '@/utils/csv'
import { readBlobMessage, saveBlob } from '@/utils/download'
import { useIsMobile } from '@/composables/useIsMobile'

const isMobile = useIsMobile()

const MAX_FILE_SIZE = 10 * 1024 * 1024

const uploadRef = ref(null)
const fileList = ref([])
const selectedFile = ref(null)

const importing = ref(false)
const downloading = ref(false)
const result = ref(null)

const hasResult = computed(() => !!result.value)
const accounts = computed(() => result.value?.accounts || [])
const errors = computed(() => result.value?.errors || [])

/** 结果摘要（共 N 行 / 成功 M / 跳过 K） */
const summary = computed(() => {
  if (!result.value) {
    return ''
  }
  const { totalRows, successCount, failCount } = result.value
  return `共 ${totalRows} 行：成功创建 ${successCount} 个账号，跳过 ${failCount} 行`
})

function onFileChange(uploadFile) {
  selectedFile.value = uploadFile?.raw || null
  fileList.value = uploadFile ? [uploadFile] : []
}

function onFileRemove() {
  selectedFile.value = null
  fileList.value = []
}

/** 只允许留一个文件：再选就替换掉旧的 */
function onExceed(files) {
  uploadRef.value?.clearFiles()
  const next = files && files[0]
  if (next) {
    uploadRef.value?.handleStart(next)
  }
}

function assertFile(file) {
  const name = (file.name || '').toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    ElMessage.warning('只支持 .xlsx / .xls 格式的 Excel 文件')
    return false
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.warning('文件不能超过 10MB，请拆分后分批导入')
    return false
  }
  return true
}

async function handleImport() {
  const file = selectedFile.value
  if (!file) {
    ElMessage.warning('请先选择填好的 Excel 文件')
    return
  }
  if (!assertFile(file)) {
    return
  }

  importing.value = true
  try {
    const data = await importMembers(file)
    result.value = data
    if (data.failCount === 0) {
      ElMessage.success(`导入完成，成功创建 ${data.successCount} 个账号`)
    } else {
      ElMessage.warning(`导入完成：成功 ${data.successCount} 个，跳过 ${data.failCount} 行，请查看下方错误清单`)
    }
    // 导入完把已选文件清掉，避免手一抖再点一次（虽然重跑也只是跳过，但没必要）
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    fileList.value = []
  } catch {
    // 提示由 axios 拦截器统一处理
  } finally {
    importing.value = false
  }
}

async function handleDownloadTemplate() {
  downloading.value = true
  try {
    const blob = await downloadImportTemplate()
    const message = await readBlobMessage(blob)
    if (message) {
      ElMessage.error(message)
      return
    }
    saveBlob(blob, '成员导入模板.xlsx')
  } catch {
    // 提示由拦截器处理
  } finally {
    downloading.value = false
  }
}

function credentialsText() {
  const rows = accounts.value.map((item) => `${item.name}\t${item.phone}\t${item.password}`)
  return ['姓名\t手机号\t初始密码', ...rows].join('\n')
}

async function copyCredentials() {
  const ok = await copyText(credentialsText())
  if (ok) {
    ElMessage.success('已复制到剪贴板')
  } else {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

function downloadCredentials() {
  const rows = [['姓名', '手机号', '初始密码']]
  accounts.value.forEach((item) => rows.push([item.name, item.phone, item.password]))
  downloadCsv(`成员初始密码清单_${today()}.csv`, rows)
}

function downloadErrors() {
  const rows = [['行号', '原因']]
  errors.value.forEach((item) => rows.push([item.row, item.reason]))
  downloadCsv(`成员导入错误清单_${today()}.csv`, rows)
}

function reset() {
  result.value = null
}
</script>

<template>
  <div class="page member-import">
    <div class="member-import__head">
      <h2 class="page-title">Excel 导入</h2>
      <p class="page-desc">
        把飞书导出的成员名单按模板整理后导入，系统会逐行校验并创建账号（初始密码随机生成，首次登录强制修改）。
      </p>
    </div>

    <!-- 步骤一：模板 -->
    <el-card class="member-import__card" shadow="never">
      <div class="member-import__step">
        <span class="member-import__step-no">1</span>
        <div class="member-import__step-body">
          <p class="member-import__step-title">下载并填写模板</p>
          <p class="member-import__step-desc">
            列头固定 7 列：姓名、手机号、学号、学院、专业、部门、职位（详见模板里的「填写说明」sheet）。
            <b>学号可以留空</b>；学院 / 部门 / 职位要写字典里的名称，专业写不出来的会按「其他 + 原文」保存。
          </p>
        </div>
        <el-button :loading="downloading" @click="handleDownloadTemplate">下载模板</el-button>
      </div>
    </el-card>

    <!-- 步骤二：上传 -->
    <el-card class="member-import__card" shadow="never">
      <div class="member-import__step">
        <span class="member-import__step-no">2</span>
        <div class="member-import__step-body">
          <p class="member-import__step-title">上传填好的文件并导入</p>
          <p class="member-import__step-desc">
            支持 .xlsx / .xls，不超过 10MB；单次最多 5000 行。手机号已存在的行会被跳过（不会重复建号），
            可以放心重跑同一份文件。
          </p>
        </div>
      </div>

      <el-upload
        ref="uploadRef"
        v-model:file-list="fileList"
        class="member-import__upload"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        :on-exceed="onExceed"
      >
        <div class="member-import__upload-text">
          把填好的 Excel 拖到这里，或<em>点击选择文件</em>
        </div>
      </el-upload>

      <div class="member-import__actions">
        <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="handleImport">
          开始导入
        </el-button>
        <span v-if="selectedFile" class="member-import__actions-tip">已选：{{ selectedFile.name }}</span>
      </div>
    </el-card>

    <!-- 结果 -->
    <el-card v-if="hasResult" class="member-import__card" shadow="never">
      <template #header>
        <div class="member-import__result-head">
          <span>导入结果</span>
          <el-button link type="primary" size="small" @click="reset">清空结果</el-button>
        </div>
      </template>

      <el-alert
        :type="errors.length ? 'warning' : 'success'"
        :closable="false"
        show-icon
        :title="summary"
        class="member-import__summary"
      />

      <!-- 成功清单：含初始密码，只此一次 -->
      <div v-if="accounts.length" class="member-import__block">
        <div class="member-import__block-head">
          <span class="member-import__block-title">创建成功的账号（{{ accounts.length }}）</span>
          <div class="member-import__block-actions">
            <el-button size="small" @click="copyCredentials">复制清单</el-button>
            <el-button size="small" @click="downloadCredentials">下载 CSV</el-button>
          </div>
        </div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="初始密码只在这里显示这一次，请立即下载或复制后逐个告知本人；系统不保存明文密码。"
          class="member-import__note"
        />
        <el-table v-if="!isMobile" :data="accounts" border stripe size="small">
          <el-table-column prop="name" label="姓名" width="140" />
          <el-table-column prop="phone" label="手机号（登录账号）" width="170" />
          <el-table-column prop="password" label="初始密码" />
        </el-table>
        <div v-else class="member-import__cards">
          <div v-for="(item, index) in accounts" :key="index" class="member-import__card-item">
            <span class="member-import__card-name">{{ item.name }}</span>
            <span class="member-import__card-line">{{ item.phone }}</span>
            <span class="member-import__card-line">初始密码：{{ item.password }}</span>
          </div>
        </div>
      </div>

      <!-- 错误清单 -->
      <div v-if="errors.length" class="member-import__block">
        <div class="member-import__block-head">
          <span class="member-import__block-title">被跳过的行（{{ errors.length }}）</span>
          <div class="member-import__block-actions">
            <el-button size="small" @click="downloadErrors">下载 CSV</el-button>
          </div>
        </div>
        <el-table v-if="!isMobile" :data="errors" border stripe size="small">
          <el-table-column prop="row" label="行号" width="90" />
          <el-table-column prop="reason" label="原因" />
        </el-table>
        <div v-else class="member-import__cards">
          <div v-for="(item, index) in errors" :key="index" class="member-import__card-item">
            <span class="member-import__card-line">第 {{ item.row }} 行</span>
            <span class="member-import__card-name">{{ item.reason }}</span>
          </div>
        </div>
        <p class="member-import__hint">
          改完这几行后可直接重新导入整份文件：已建号的手机号会被识别为「已存在」而跳过，不会产生重复账号。
        </p>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.member-import__card {
  margin-top: 12px;
  border-radius: var(--brand-radius);
}

.member-import__step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.member-import__step-no {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--brand-primary);
  color: #fff;
  font-size: 13px;
  line-height: 22px;
  text-align: center;
}

.member-import__step-body {
  flex: 1;
  min-width: 0;
}

.member-import__step-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.member-import__step-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

.member-import__upload {
  margin-top: 12px;
}

.member-import__upload-text {
  font-size: 13px;
  color: #606266;
}

.member-import__upload-text em {
  color: var(--brand-primary);
  font-style: normal;
}

.member-import__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.member-import__actions-tip {
  font-size: 12px;
  color: #909399;
  word-break: break-all;
}

.member-import__result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.member-import__summary {
  margin-bottom: 12px;
}

.member-import__note {
  margin-bottom: 10px;
}

.member-import__block + .member-import__block {
  margin-top: 18px;
}

.member-import__block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.member-import__block-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.member-import__block-actions {
  display: flex;
  gap: 8px;
  flex: none;
}

.member-import__cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-import__card-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.member-import__card-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.member-import__card-line {
  font-size: 13px;
  color: #606266;
}

.member-import__hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

@media (max-width: 768px) {
  .member-import__step {
    flex-wrap: wrap;
  }

  .member-import__step-body {
    order: 2;
    flex-basis: 100%;
  }
}
</style>
