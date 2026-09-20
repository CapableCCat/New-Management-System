<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDict, getDictAdminList, getDictTypes, moveDict, updateDict } from '@/api/dict'
import { useDictStore } from '@/stores/dict'
import { useIsMobile } from '@/composables/useIsMobile'

/**
 * 字典管理（F-011，仅超管）
 *
 * 规则：
 *   - 编码由 PRD 固定，入库后不可修改（新增时填一次）
 *   - 条目只停用不删除，且不允许把某类型的启用项停空
 *   - 保存后清掉前端字典缓存，其它页面下次进入即拿到新文案
 */
const isMobile = useIsMobile()
const dictStore = useDictStore()

const types = ref([])
const activeType = ref('')
const list = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const dialogMode = ref('create')
const saving = ref(false)
const form = reactive({ id: null, code: '', label: '', sort: null, remark: '' })

const activeTypeMeta = computed(
  () => types.value.find((item) => item.code === activeType.value) || {}
)

async function loadTypes() {
  types.value = await getDictTypes()
  if (!activeType.value && types.value.length) {
    activeType.value = types.value[0].code
  }
}

async function loadList() {
  if (!activeType.value) {
    return
  }
  loading.value = true
  try {
    list.value = await getDictAdminList(activeType.value)
  } finally {
    loading.value = false
  }
}

function switchType() {
  loadList()
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: null, code: '', label: '', sort: null, remark: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    code: row.code,
    label: row.label,
    sort: row.sort,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function submit() {
  if (dialogMode.value === 'create' && !form.code.trim()) {
    ElMessage.warning('请填写编码')
    return
  }
  if (!form.label.trim()) {
    ElMessage.warning('请填写文案')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createDict({
        type: activeType.value,
        code: form.code.trim(),
        label: form.label.trim(),
        sort: form.sort,
        remark: form.remark
      })
      ElMessage.success('新增成功')
    } else {
      await updateDict(form.id, {
        label: form.label.trim(),
        sort: form.sort,
        remark: form.remark
      })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    await afterChange()
  } catch {
    // 提示由 axios 拦截器统一处理，弹窗保持打开方便改
  } finally {
    saving.value = false
  }
}

async function changeEnabled(row) {
  const next = row.enabled === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}「${row.label}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateDict(row.id, {
      label: row.label,
      sort: row.sort,
      remark: row.remark,
      enabled: next
    })
    ElMessage.success(`${action}成功`)
    await afterChange()
  } catch {
    // 拦截器已提示
  }
}

async function move(row, direction) {
  try {
    await moveDict(row.id, direction)
    await loadList()
  } catch {
    // 拦截器已提示
  }
}

/** 变更后：刷新本页 + 清公开字典缓存（保证其它页面立即拿到新文案） */
async function afterChange() {
  await loadList()
  dictStore.clear(activeType.value)
}

onMounted(async () => {
  await loadTypes()
  await loadList()
})
</script>

<template>
  <div class="page dict">
    <div class="dict__head">
      <div>
        <h2 class="page-title">字典管理</h2>
        <p class="page-desc">编码由 PRD 固定，只能改文案 / 排序 / 启停；条目只停用不删除。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增条目</el-button>
    </div>

    <div class="dict__types">
      <el-radio-group v-if="!isMobile" v-model="activeType" @change="switchType">
        <el-radio-button v-for="item in types" :key="item.code" :value="item.code">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
      <el-select v-else v-model="activeType" style="width: 100%" @change="switchType">
        <el-option v-for="item in types" :key="item.code" :label="item.label" :value="item.code" />
      </el-select>
    </div>

    <!-- 桌面：表格 -->
    <el-table v-if="!isMobile" v-loading="loading" :data="list" border stripe>
      <el-table-column prop="sort" label="排序" width="72" />
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="label" label="文案" min-width="150" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <span :class="row.enabled === 1 ? 'dict__on' : 'dict__off'">
            {{ row.enabled === 1 ? '启用' : '停用' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row, $index }">
          <el-button link size="small" :disabled="$index === 0" @click="move(row, 'up')">
            上移
          </el-button>
          <el-button
            link
            size="small"
            :disabled="$index === list.length - 1"
            @click="move(row, 'down')"
          >
            下移
          </el-button>
          <el-button link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link size="small" @click="changeEnabled(row)">
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 移动：卡片 -->
    <div v-else class="dict__cards">
      <div v-for="(row, $index) in list" :key="row.id" class="dict__card">
        <div class="dict__card-head">
          <span class="dict__card-label">{{ row.label }}</span>
          <span :class="row.enabled === 1 ? 'dict__on' : 'dict__off'">
            {{ row.enabled === 1 ? '启用' : '停用' }}
          </span>
        </div>
        <div class="dict__card-meta">
          编码 {{ row.code }} · 排序 {{ row.sort }}
          <template v-if="row.remark"> · {{ row.remark }}</template>
        </div>
        <div class="dict__card-actions">
          <el-button link size="small" :disabled="$index === 0" @click="move(row, 'up')">
            上移
          </el-button>
          <el-button
            link
            size="small"
            :disabled="$index === list.length - 1"
            @click="move(row, 'down')"
          >
            下移
          </el-button>
          <el-button link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link size="small" @click="changeEnabled(row)">
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
        </div>
      </div>
      <p v-if="!loading && !list.length" class="dict__empty">该类型暂无条目</p>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增字典条目' : '编辑字典条目'"
      :width="isMobile ? '92%' : '480px'"
    >
      <el-form :model="form" label-width="72px">
        <el-form-item label="类型">
          <el-input :model-value="activeTypeMeta.label" disabled />
        </el-form-item>
        <el-form-item label="编码">
          <el-input
            v-model="form.code"
            :disabled="dialogMode === 'edit'"
            placeholder="英文或数字，入库后不可修改"
          />
        </el-form-item>
        <el-form-item label="文案">
          <el-input v-model="form.label" maxlength="64" placeholder="展示给用户看的名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
          <span class="dict__hint">留空则自动排到末尾</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dict__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.dict__types {
  margin: 8px 0 14px;
}

.dict__on {
  color: #67c23a;
}

.dict__off {
  color: #909399;
}

.dict__cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dict__card {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.dict__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dict__card-label {
  font-size: 15px;
  font-weight: 500;
}

.dict__card-meta {
  margin: 4px 0 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}

.dict__card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.dict__empty {
  padding: 24px 0;
  text-align: center;
  color: #909399;
}

.dict__hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
