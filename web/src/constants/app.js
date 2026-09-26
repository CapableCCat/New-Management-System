/**
 * 应用级常量
 */

export const APP_NAME = '开源鸿蒙社'
export const APP_FULL_NAME = '天津中德开源鸿蒙社（TSGU-OSC）'

/** Sa-Token 的 Header 名，必须与后端 application.yml 的 sa-token.token-name 一致 */
export const TOKEN_HEADER = 'osc-token'

/** 本地存储 key */
export const STORAGE_KEY = {
  TOKEN: 'osc_token',
  PROFILE: 'osc_profile',
  LOCALE: 'osc_locale',
  /** 上次查看公告的时间（本地时区的 ISO 串，与后端 LocalDateTime 同格式；T19 红点用） */
  ANNOUNCEMENT_READ_AT: 'osc_announcement_read_at'
}

/** 路由 path */
export const ROUTE_PATH = {
  LOGIN: '/login',
  INIT: '/init',
  APPLY: '/apply',
  QUERY: '/query',
  CHANGE_PASSWORD: '/change-password',
  HOME: '/home',
  ANNOUNCEMENT: '/announcement',
  PROFILE: '/profile'
}
