import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getDict } from '@/api/dict'

/**
 * 字典缓存（供全站引用：报名页、查询页、成员档案、审核台…）
 *
 * 说明：字典是"读多写少 + 要求改了立即生效"，所以只在**前端会话内**缓存，
 * 后端不做缓存；字典管理页保存后会调用 clear() 让其它页面重新拉取。
 */
export const useDictStore = defineStore('dict', () => {
  /** type -> DictVO[] */
  const cache = ref({})
  const pending = new Map()

  /** 取某类型启用项（默认走缓存，force=true 强制刷新） */
  async function load(type, { force = false } = {}) {
    if (!type) {
      return []
    }
    if (!force && cache.value[type]) {
      return cache.value[type]
    }
    if (pending.has(type)) {
      return pending.get(type)
    }
    const task = getDict(type)
      .then((list) => {
        cache.value[type] = list || []
        return cache.value[type]
      })
      .finally(() => {
        pending.delete(type)
      })
    pending.set(type, task)
    return task
  }

  /** 批量取（报名页一次要学院/专业/部门/标签） */
  async function loadMany(types) {
    const result = {}
    await Promise.all(
      types.map(async (type) => {
        result[type] = await load(type)
      })
    )
    return result
  }

  /** code → 文案；找不到时回退显示 code 本身 */
  function labelOf(type, code) {
    if (code === null || code === undefined || code === '') {
      return ''
    }
    const list = cache.value[type] || []
    const hit = list.find((item) => String(item.code) === String(code))
    return hit ? hit.label : String(code)
  }

  /** 按 type 清缓存（不传则全清） */
  function clear(type) {
    if (type) {
      delete cache.value[type]
    } else {
      cache.value = {}
    }
  }

  return { cache, load, loadMany, labelOf, clear }
})
