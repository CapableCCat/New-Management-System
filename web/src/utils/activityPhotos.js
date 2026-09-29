/**
 * 活动照片探测（PRD F-001 第 1 步：报名页「活动照片轮播」）
 *
 * 社长要求：素材暂时没有 → **占位符占位**；等他搜集好放进 `web/public/activities/` 就能用。
 * 因此这里按**固定命名探测**，而不是维护一份文件清单 —— 免得每加一张照片都要改代码。
 *
 * 约定（`web/public/activities/` 目录里已放 README 说明）：
 *   文件名 `01` ~ `05` + 扩展名 `.jpg` / `.jpeg` / `.png`；编号即轮播顺序。
 *
 * 代价：每张候选图会发一次请求（未命中即 404，很轻），且**一次页面加载内只探测一次**
 *      （结果缓存在模块作用域，SPA 内路由来回切不会重复探测）。
 *
 * 设计取舍：为什么不扫目录 / 不用 import.meta.glob？
 *   `public/` 下的文件**不参与构建**（原样拷贝），构建期拿不到「目录里有什么」——
 *   要自动感知就只能放到 `src/assets/` 并 glob，但那与「运行时资产放 public」的约定冲突，
 *   且社长已明确会往 `public/activities/` 放图。故采用运行时探测。
 */

/** 槽位编号 = 轮播顺序 */
const SLOT_NAMES = ['01', '02', '03', '04', '05']

/** 支持的扩展名（按优先级；同一张图只认第一个命中的） */
const EXTENSIONS = ['jpg', 'jpeg', 'png']

/** 占位块数量（没有真实素材时显示几张占位） */
export const PLACEHOLDER_COUNT = 3

/** 探测单张图能否加载 */
function probe(url) {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => resolve(url)
    img.onerror = () => resolve(null)
    img.src = url
  })
}

let cache = null

/**
 * 加载活动照片地址列表（按槽位编号排序）。
 *
 * @returns {Promise<string[]>} 命中到的图片地址；一张都没有时返回 `[]`
 */
export function loadActivityPhotos() {
  if (cache) {
    return cache
  }
  cache = (async () => {
    const candidates = []
    for (const name of SLOT_NAMES) {
      for (const ext of EXTENSIONS) {
        candidates.push({ name, url: `/activities/${name}.${ext}` })
      }
    }
    // 并行探测（一次往返），再按槽位取第一个命中的扩展名
    const probed = await Promise.all(
      candidates.map(async (item) => ({ ...item, ok: await probe(item.url) }))
    )
    const bySlot = new Map()
    for (const item of probed) {
      if (item.ok && !bySlot.has(item.name)) {
        bySlot.set(item.name, item.url)
      }
    }
    return SLOT_NAMES.filter((name) => bySlot.has(name)).map((name) => bySlot.get(name))
  })()
  return cache
}
