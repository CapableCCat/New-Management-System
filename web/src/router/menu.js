import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 菜单**从路由表生成**（《开发任务点清单》§6 D108 —— T19 的单一出处；T22 起支持两级分组）
 *
 * 每条内部路由在 `meta.menu` 里声明自己：
 *   `{ label, order, capability, group, icon }`
 *     - `label`      菜单文案（缺省用 `meta.title`）
 *     - `order`      **组内**排序（小的在前）
 *     - `capability` 具名能力函数（`constants/roles.js`），缺省 = 登录即可见
 *     - `group`      所属分组 key（见 `MENU_GROUPS`；缺省归入「其他」）
 *     - `icon`       Element Plus 图标组件名（`@element-plus/icons-vue` 里的具名导出）
 *
 * 于是：
 *   ✅ 加一个页面 = **只改路由表一处**（菜单与守卫同时生效）
 *   ✅ 「菜单看得见」与「守卫放得进去」用的是**同一个 capability**，不可能再漂
 *   ✅ 公开路由不写 `meta.menu`，自然不出现在内部导航里
 *
 * ⚠️ `icon` 存的是**组件名字符串**而不是组件本身 —— 路由表是纯数据，不该 import 组件；
 *    由 `AppShell` 统一从图标库取，取不到时降级为不显示图标（不报错）。
 */

/** 分组定义（`order` 决定组的先后；组内各项按 `meta.menu.order` 排） */
export const MENU_GROUPS = [
  { key: 'mine', label: '我的', order: 10 },
  { key: 'recruit', label: '纳新管理', order: 20 },
  { key: 'content', label: '内容管理', order: 30 },
  { key: 'insight', label: '数据洞察', order: 40 },
  { key: 'system', label: '系统管理', order: 50 }
]

/** 未声明 group 的项落到这里（不静默丢弃，方便发现漏配） */
const FALLBACK_GROUP = { key: 'other', label: '其他', order: 999 }

/** 移动端底部 tabbar 取前几项（高频页；其余进「更多」抽屉） */
const TABBAR_SIZE = 3

export function useRouteMenu() {
  const router = useRouter()
  const userStore = useUserStore()

  /** 全部可见菜单项（**扁平**，按「组 order → 组内 order」排序） */
  const items = computed(() =>
    router
      .getRoutes()
      .filter((record) => record.meta && record.meta.menu)
      .filter((record) => {
        const capability = record.meta.menu.capability
        return typeof capability !== 'function' || capability(userStore.profile)
      })
      .map((record) => {
        const meta = record.meta.menu
        const group = MENU_GROUPS.find((g) => g.key === meta.group) || FALLBACK_GROUP
        return {
          path: record.path,
          label: meta.label || record.meta.title || record.path,
          icon: meta.icon || '',
          order: meta.order ?? 999,
          groupKey: group.key,
          groupLabel: group.label,
          groupOrder: group.order
        }
      })
      .sort((a, b) =>
        a.groupOrder !== b.groupOrder ? a.groupOrder - b.groupOrder : a.order - b.order
      )
  )

  /** 分组结构（**空组不渲染**）—— 桌面左侧菜单与移动端抽屉共用这一份 */
  const groups = computed(() => {
    const map = new Map()
    for (const item of items.value) {
      if (!map.has(item.groupKey)) {
        map.set(item.groupKey, {
          key: item.groupKey,
          label: item.groupLabel,
          order: item.groupOrder,
          items: []
        })
      }
      map.get(item.groupKey).items.push({
        path: item.path,
        label: item.label,
        icon: item.icon
      })
    }
    return [...map.values()].sort((a, b) => a.order - b.order)
  })

  /** 移动端底部 tabbar：取扁平列表的前 N 项（即「组序 → 组内序」最靠前的几项） */
  const tabbarItems = computed(() =>
    items.value.slice(0, TABBAR_SIZE).map(({ path, label, icon }) => ({ path, label, icon }))
  )

  return { groups, items, tabbarItems }
}
