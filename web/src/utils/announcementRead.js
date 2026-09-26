import { STORAGE_KEY } from '@/constants/app'

/**
 * 公告「已读」标记（清单 §6 D112 / T19）
 *
 * V1.0 不为一个红点引入未读状态模型：**不落库、不加表**，
 * 只用 localStorage 记「上次查看公告的时间」，与最新公告的发布时间比大小。
 *
 * ⚠️ 时间格式必须与后端的 `LocalDateTime` 一致（本地时区、无时区后缀、秒级）。
 * 若用 `toISOString()`（UTC + 毫秒 + `Z`），会跟后端时间差 8 小时，
 * 导致「刚看过又亮红点」——所以这里自己拼本地串。
 */

/** 本地时区的 `yyyy-MM-ddTHH:mm:ss`（对齐后端 LocalDateTime 的序列化格式） */
export function nowLocalIso() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

/** 上次查看公告的时间（空串 = 从没看过） */
export function getAnnouncementReadAt() {
  try {
    return localStorage.getItem(STORAGE_KEY.ANNOUNCEMENT_READ_AT) || ''
  } catch {
    return ''
  }
}

/** 标记公告为已读（打开公告页、或点「一键已读」时调用） */
export function markAnnouncementRead(at = nowLocalIso()) {
  try {
    localStorage.setItem(STORAGE_KEY.ANNOUNCEMENT_READ_AT, at)
  } catch {
    // 隐私模式下 localStorage 可能不可用：忽略，只是红点会重现
  }
  return at
}

/** 给定「最新公告的发布时间」，判断是否有未读 */
export function hasUnreadAnnouncement(latestCreatedAt) {
  if (!latestCreatedAt) {
    return false
  }
  const readAt = getAnnouncementReadAt()
  return !readAt || String(latestCreatedAt) > readAt
}
