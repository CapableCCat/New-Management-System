import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Locale } from 'vant'
import elementEn from 'element-plus/es/locale/lang/en'
import elementZhCn from 'element-plus/es/locale/lang/zh-cn'
import vantEnUS from 'vant/es/locale/lang/en-US'
import vantZhCN from 'vant/es/locale/lang/zh-CN'
import { STORAGE_KEY } from '@/constants/app'

/**
 * 界面语言（**只管组件库内置文案**：确认框按钮、分页、上传、空态、日期选择器……）
 *
 * 为什么要这个：Element Plus / Vant 的内置文案默认是**英文**，而系统面向中文社团 ——
 * 不配 locale 的话，删除确认框会出现「OK / Cancel」，在中文界面里很突兀。
 *
 * 范围说明（有意为之）：本项目**页面自己写的文案仍是中文**，V1.0 不做整站翻译。
 * 做成可切换是为了：① 中文环境下不再冒出英文按钮；② 演示/答辩时能一键给个双语界面。
 *
 * 实现分工：Element Plus 走 `ElConfigProvider` 的响应式 locale（见 App.vue）；
 * Vant 只有命令式的 `Locale.use()`，故在切换时手动同步一次。
 */

const LOCALES = {
  'zh-cn': { label: '中文', element: elementZhCn, vant: vantZhCN, short: '中' },
  en: { label: 'English', element: elementEn, vant: vantEnUS, short: 'EN' }
}

/** 供界面做语言选择用（当前只有两种） */
export const LOCALE_OPTIONS = Object.entries(LOCALES).map(([value, item]) => ({
  value,
  label: item.label
}))

function readCached() {
  const saved = localStorage.getItem(STORAGE_KEY.LOCALE)
  return saved && LOCALES[saved] ? saved : 'zh-cn'
}

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref(readCached())

  /** 交给 <el-config-provider :locale> 的对象 */
  const elementLocale = computed(() => LOCALES[locale.value].element)
  const isChinese = computed(() => locale.value === 'zh-cn')
  /** 切换按钮上显示的「另一种语言」 */
  const switchLabel = computed(() => (isChinese.value ? 'EN' : '中'))

  function setLocale(next) {
    if (!LOCALES[next]) {
      return
    }
    locale.value = next
    localStorage.setItem(STORAGE_KEY.LOCALE, next)
    Locale.use(next, LOCALES[next].vant)
  }

  function toggle() {
    setLocale(isChinese.value ? 'en' : 'zh-cn')
  }

  // 初始化：把缓存的语言同步给 Vant（Element Plus 由 config-provider 自己响应）
  Locale.use(locale.value, LOCALES[locale.value].vant)

  return { locale, elementLocale, isChinese, switchLabel, setLocale, toggle }
})
