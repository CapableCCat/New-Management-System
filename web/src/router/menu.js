import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 菜单**从路由表生成**（《开发任务点清单》§6 D108 —— T19 的单一出处）
 *
 * 每条内部路由在 `meta.menu` 里声明自己：
 *   `{ label, order, capability }`
 *     - `label`      菜单文案（缺省用 `meta.title`）
 *     - `order`      排序（小的在前；决定移动端 tabbar 取哪几项）
 *     - `capability` 具名能力函数（`constants/roles.js`），缺省 = 登录即可见
 *
 * 于是：
 *   ✅ 加一个页面 = **只改路由表一处**（菜单与守卫同时生效）
 *   ✅ 「菜单看得见」与「守卫放得进去」用的是**同一个 capability**，不可能再漂
 *   ✅ 公开路由不写 `meta.menu`，自然不出现在内部导航里
 */
export function useRouteMenu() {
  const router = useRouter()
  const userStore = useUserStore()

  /** 全部可见菜单项（已按 order 排序） */
  const items = computed(() =>
    router
      .getRoutes()
      .filter((record) => record.meta && record.meta.menu)
      .filter((record) => {
        const capability = record.meta.menu.capability
        return typeof capability !== 'function' || capability(userStore.profile)
      })
      .sort((a, b) => (a.meta.menu.order ?? 999) - (b.meta.menu.order ?? 999))
      .map((record) => ({
        path: record.path,
        label: record.meta.menu.label || record.meta.title || record.path
      }))
  )

  /** 移动端底部 tabbar：取 order 最小的前几项（高频页），其余进「更多」抽屉 */
  const tabbarItems = computed(() => items.value.slice(0, 3))

  return { items, tabbarItems }
}
