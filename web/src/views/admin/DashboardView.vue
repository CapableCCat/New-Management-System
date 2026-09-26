<script setup>
/**
 * 基础看板（PRD F-010）
 *
 * 两块内容、两套数据源，**刻意分开摆放**（PRD 明确"两处数据源不混用"）：
 *   ① 成员现状 —— `user` 表仅「账号状态正常」的正式成员 → 回答"现在有多少人"
 *   ② 招新复盘 —— `recruit_apply` 表全量（含被拒者）→ 回答"这次纳新收成如何"
 * 把它们的数字混在一排卡片里，最容易被误读成同一个口径。
 *
 * 空态：某一项没有数据时给空态提示，**不画空白图表**（PRD 验收要求）。
 *
 * 地图合规：用本地矢量行政区划数据（含台湾省 / 香港特别行政区 / 澳门特别行政区 + 九段线），
 * 不接任何在线底图或境外地图服务，因此不需要 key、断网也能用（详见 utils/echarts.js）。
 */
import { computed, onMounted, ref } from 'vue'
import { getMemberStats, getRecruitStats } from '@/api/dashboard'
import { useIsMobile } from '@/composables/useIsMobile'
import BaseChart from '@/components/BaseChart.vue'
import { CHINA_MAP_NAME, ensureChinaMap } from '@/utils/echarts'

const isMobile = useIsMobile()

const loading = ref(false)
const member = ref(null)
const recruit = ref(null)
const mapReady = ref(false)

const PALETTE = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#9c27b0', '#00bcd4', '#ff9800']
const CHART_HEIGHT = computed(() => (isMobile.value ? '240px' : '300px'))
/**
 * 地图高度按**卡片宽度**来配：这份区划数据的外接框（含南海诸岛，最南到 3°N 附近）
 * 大约是 1.25:1，半宽卡片配 360px 最贴合，地图能铺满；矮了会被挤成小小一块、左右全是空白。
 */
const MAP_HEIGHT = computed(() => (isMobile.value ? '280px' : '360px'))

const has = (list) => Array.isArray(list) && list.length > 0

async function load() {
  loading.value = true
  try {
    const [memberData, recruitData] = await Promise.all([getMemberStats(), getRecruitStats()])
    member.value = memberData
    recruit.value = recruitData
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), ensureChinaMap().then((ok) => (mapReady.value = ok))])
})

/* ---------------- 成员现状 ---------------- */

const colleges = computed(() => member.value?.colleges || [])
const majors = computed(() => member.value?.majors || [])
const genders = computed(() => member.value?.genders || [])
const provinces = computed(() => member.value?.provinces || [])

/** 饼图（学院 / 性别共用） */
function pieOption(list) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { type: 'scroll', bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8 },
    color: PALETTE,
    series: [
      {
        type: 'pie',
        radius: isMobile.value ? ['38%', '62%'] : ['42%', '66%'],
        center: ['50%', '46%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b} {c}' },
        data: list.map((item) => ({ name: item.name, value: item.value }))
      }
    ]
  }
}

const collegeOption = computed(() => pieOption(colleges.value))
const genderOption = computed(() => pieOption(genders.value))

/** 专业分布：项目多，用柱状图（后端已按人数降序返回） */
const majorOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}：{c} 人' },
  grid: { left: 8, right: 16, top: 24, bottom: 46, containLabel: true },
  xAxis: {
    type: 'category',
    data: majors.value.map((item) => item.name),
    axisLabel: { interval: 0, rotate: 35, fontSize: 11, color: '#606266' },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#909399' }
  },
  series: [
    {
      type: 'bar',
      barMaxWidth: 26,
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      data: majors.value.map((item) => item.value)
    }
  ]
}))

/** 地区分布：省级热力（数据已是标准省名，含港澳台） */
const mapOption = computed(() => {
  const values = provinces.value.map((item) => item.value)
  const max = values.length ? Math.max(...values) : 1
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params) =>
        params.value === undefined || params.value === null || Number.isNaN(params.value)
          ? `${params.name}：暂无数据`
          : `${params.name}：${params.value} 人`
    },
    visualMap: {
      type: 'continuous',
      min: 0,
      max,
      left: 12,
      bottom: 12,
      itemWidth: 12,
      itemHeight: 78,
      text: ['多', '少'],
      calculable: true,
      inRange: { color: ['#e8f3ff', '#9ecbff', '#409eff', '#1c5fa8'] },
      textStyle: { color: '#606266', fontSize: 11 }
    },
    series: [
      {
        type: 'map',
        map: CHINA_MAP_NAME,
        roam: false,
        zoom: 1.05,
        label: { show: false },
        itemStyle: { areaColor: '#f7f8fa', borderColor: '#dcdfe6' },
        emphasis: {
          label: { show: true, fontSize: 11, color: '#303133' },
          itemStyle: { areaColor: '#ffe58f' }
        },
        data: provinces.value.map((item) => ({ name: item.name, value: item.value }))
      }
    ]
  }
})

/* ---------------- 招新复盘 ---------------- */

const trend = computed(() => recruit.value?.trend || [])

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: (params) => `${params[0].axisValue}：${params[0].data} 人` },
  grid: { left: 8, right: 20, top: 24, bottom: 36, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => item.date),
    axisLabel: { color: '#606266', fontSize: 11 },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#f0f2f5' } },
    axisLabel: { color: '#909399' }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      symbolSize: 6,
      itemStyle: { color: '#409eff' },
      lineStyle: { width: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64,158,255,0.28)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ]
        }
      },
      data: trend.value.map((item) => item.count)
    }
  ]
}))
</script>

<template>
  <div v-loading="loading" class="page dashboard">
    <div class="dashboard__head">
      <div>
        <h2 class="page-title">基础看板</h2>
        <p class="page-desc">成员现状与招新复盘，两块数据口径不同，请分开看。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <!-- ① 成员现状 -->
    <section class="dashboard__block">
      <div class="dashboard__block-head">
        <h3 class="dashboard__block-title">成员现状</h3>
        <span class="dashboard__source">
          数据源：正式成员（账号状态为「正常」），共 {{ member?.total ?? 0 }} 人
        </span>
      </div>

      <div class="dashboard__metric-row">
        <div class="dashboard__metric dashboard__metric--primary">
          <span class="dashboard__metric-value">{{ member?.total ?? 0 }}</span>
          <span class="dashboard__metric-label">正式成员总数</span>
        </div>
      </div>

      <div class="dashboard__grid">
        <el-card class="dashboard__card" shadow="never">
          <template #header><span class="dashboard__card-title">学院分布</span></template>
          <BaseChart v-if="has(colleges)" :option="collegeOption" :height="CHART_HEIGHT" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>

        <el-card class="dashboard__card" shadow="never">
          <template #header><span class="dashboard__card-title">专业分布</span></template>
          <BaseChart v-if="has(majors)" :option="majorOption" :height="CHART_HEIGHT" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>

        <el-card class="dashboard__card" shadow="never">
          <template #header><span class="dashboard__card-title">性别分布</span></template>
          <BaseChart v-if="has(genders)" :option="genderOption" :height="CHART_HEIGHT" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>

        <el-card class="dashboard__card" shadow="never">
          <template #header>
            <div class="dashboard__card-head">
              <span class="dashboard__card-title">地区分布（生源地）</span>
              <span v-if="member?.provinceUnspecified" class="dashboard__card-hint">
                另有 {{ member.provinceUnspecified }} 人未识别，未计入
              </span>
            </div>
          </template>
          <BaseChart v-if="has(provinces) && mapReady" :option="mapOption" :height="MAP_HEIGHT" />
          <el-empty
            v-else
            :description="mapReady ? '暂无数据' : '地图数据加载中或不可用'"
            :image-size="60"
          />
        </el-card>
      </div>
    </section>

    <!-- ② 招新复盘 -->
    <section class="dashboard__block">
      <div class="dashboard__block-head">
        <h3 class="dashboard__block-title">招新复盘</h3>
        <span class="dashboard__source">
          数据源：全部报名记录（含未通过者），共 {{ recruit?.total ?? 0 }} 条
        </span>
      </div>

      <div class="dashboard__metric-row">
        <div class="dashboard__metric">
          <span class="dashboard__metric-value">{{ recruit?.total ?? 0 }}</span>
          <span class="dashboard__metric-label">报名总数</span>
        </div>
        <div class="dashboard__metric">
          <span class="dashboard__metric-value dashboard__metric-value--pending">{{ recruit?.pending ?? 0 }}</span>
          <span class="dashboard__metric-label">待审</span>
        </div>
        <div class="dashboard__metric">
          <span class="dashboard__metric-value dashboard__metric-value--ok">{{ recruit?.approved ?? 0 }}</span>
          <span class="dashboard__metric-label">已通过</span>
        </div>
        <div class="dashboard__metric">
          <span class="dashboard__metric-value dashboard__metric-value--no">{{ recruit?.rejected ?? 0 }}</span>
          <span class="dashboard__metric-label">已拒绝</span>
        </div>
      </div>

      <div class="dashboard__grid">
        <el-card class="dashboard__card dashboard__card--wide" shadow="never">
          <template #header><span class="dashboard__card-title">报名按日趋势</span></template>
          <BaseChart v-if="has(trend)" :option="trendOption" :height="CHART_HEIGHT" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.dashboard__block {
  margin-top: 20px;
}

.dashboard__block-head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.dashboard__block-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.dashboard__source {
  font-size: 12px;
  color: #909399;
}

.dashboard__metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.dashboard__metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 108px;
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: var(--brand-radius);
  background: #fff;
}

.dashboard__metric--primary {
  min-width: 148px;
  border-left: 3px solid var(--brand-primary);
}

.dashboard__metric-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--brand-primary);
}

.dashboard__metric-value--pending {
  color: #e6a23c;
}

.dashboard__metric-value--ok {
  color: #67c23a;
}

.dashboard__metric-value--no {
  color: #f56c6c;
}

.dashboard__metric-label {
  font-size: 12px;
  color: #909399;
}

.dashboard__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard__card {
  border-radius: var(--brand-radius);
}

.dashboard__card--wide {
  grid-column: 1 / -1;
}

.dashboard__card-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.dashboard__card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.dashboard__card-hint {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .dashboard__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .dashboard__head {
    flex-direction: column;
  }

  .dashboard__metric-row {
    gap: 8px;
  }

  .dashboard__metric {
    flex: 1 1 44%;
    min-width: 0;
    padding: 10px 12px;
  }

  .dashboard__metric-value {
    font-size: 20px;
  }
}
</style>
