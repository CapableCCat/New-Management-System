import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getRecruitInfo } from '@/api/recruit'

/**
 * 公开端报名配置缓存（会话级）。
 *
 * 用途有二：
 *   1. 根路径 `/` 的落地页按 `recruit_open` 动态决定（《开发任务点清单》§6 D110）——
 *      报名开关打开进报名页（纳新期新生扫码一步到位），关闭进登录页；
 *   2. 报名页 / 查询页复用同一份配置（社团简介、审核时效文案），避免同一个接口请求两次。
 *
 * 取不到配置时**不缓存失败结果**（返回 null），下次导航会重试；
 * 调用方看到 null 一律按更保守的分支处理（落地页回退登录页）。
 */
export const useRecruitStore = defineStore('recruit', () => {
  /** /recruit/info 的完整返回（{ open, clubIntro, reviewNotice }），null 表示还没取到 */
  const info = ref(null)
  let pending = null

  /** 取报名配置（带会话内缓存；force=true 强制刷新） */
  function loadInfo({ force = false } = {}) {
    if (!force && info.value) {
      return Promise.resolve(info.value)
    }
    if (pending) {
      return pending
    }
    pending = getRecruitInfo()
      .then((data) => {
        info.value = data || null
        return info.value
      })
      .catch(() => {
        // 失败不写缓存：下次导航重试
        return null
      })
      .finally(() => {
        pending = null
      })
    return pending
  }

  /** 报名开关；取不到时返回 null（调用方按保守分支处理） */
  async function loadOpen() {
    const data = await loadInfo()
    return data ? !!data.open : null
  }

  /** 退出登录 / 配置改变后清缓存 */
  function clear() {
    info.value = null
    pending = null
  }

  return { info, loadInfo, loadOpen, clear }
})
