<script setup>
/**
 * 看板（占位）—— T15 实现
 * 注意双口径：成员现状查 user（仅正常），招新复盘查 recruit_apply（全量含被拒）
 */
</script>

<template>
  <div class="page">
    <PlaceholderCard
      title="看板"
      task="T15"
      desc="成员现状 + 招新复盘，两处数据源不混用（PRD F-010）"
      :points="[
        '成员现状（user 表，仅 status=正常）：总人数 / 学院 / 专业 / 性别 / 地区分布',
        '招新复盘（recruit_apply 表，全量）：报名 / 通过 / 拒绝数，按日趋势',
        '中国地图使用标准地图数据（含港澳台），无数据时展示空态'
      ]"
    />
  </div>
</template>
