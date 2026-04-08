<script setup>
import { computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'

use([
  CanvasRenderer,
  PieChart,
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
])

const props = defineProps({
  stats: { type: Object, default: null },
  loading: { type: Boolean, default: false },
})

const textColor = '#ecfdf5'
const muted = 'rgba(167, 243, 208, 0.72)'
const accent = '#4ade80'

function round1(v) {
  if (v == null || Number.isNaN(v)) return 0
  return Math.round(v * 10) / 10
}

function formatShortDate(ms) {
  if (ms == null) return ''
  const d = new Date(ms)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const hasData = computed(
  () => props.stats && props.stats.completedSessionCount > 0,
)

const pieOption = computed(() => {
  const bp = props.stats?.byPart
  if (!bp?.length) return {}
  return {
    backgroundColor: 'transparent',
    textStyle: { color: textColor },
    tooltip: { trigger: 'item', textStyle: { color: '#14532d' } },
    legend: {
      bottom: 0,
      textStyle: { color: muted, fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: 'rgba(10, 42, 32, 0.9)',
          borderWidth: 2,
        },
        label: { color: textColor, fontSize: 11 },
        data: bp.map((p) => ({
          name: p.partLabel,
          value: p.sessionCount,
        })),
        color: ['#22c55e', '#34d399', '#6ee7b7', '#86efac', '#a7f3d0'],
      },
    ],
  }
})

/** 各 Part 平均 Overall */
const bandBarOption = computed(() => {
  const bp = props.stats?.byPart
  if (!bp?.length) return {}
  const cats = bp.map((p) => p.partLabel)
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: { color: '#14532d' },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      data: cats,
      axisLine: { lineStyle: { color: muted } },
      axisLabel: { color: muted, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 9,
      splitLine: { lineStyle: { color: 'rgba(74, 222, 128, 0.12)' } },
      axisLabel: { color: muted },
    },
    series: [
      {
        name: '平均 Overall',
        type: 'bar',
        barMaxWidth: 48,
        data: bp.map((p) => round1(p.avgOverallBand)),
        itemStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#4ade80' },
              { offset: 1, color: '#15803d' },
            ],
          },
        },
      },
    ],
  }
})

/** 各 Part 五维均分（柱状分组） */
const rubricBarOption = computed(() => {
  const bp = props.stats?.byPart
  if (!bp?.length) return {}
  const cats = bp.map((p) => p.partLabel)
  const mk = (fn) => bp.map((p) => round1(fn(p)))
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: { color: '#14532d' },
    },
    legend: {
      top: 0,
      textStyle: { color: muted, fontSize: 11 },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 56, containLabel: true },
    xAxis: {
      type: 'category',
      data: cats,
      axisLine: { lineStyle: { color: muted } },
      axisLabel: { color: muted, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 9,
      splitLine: { lineStyle: { color: 'rgba(74, 222, 128, 0.12)' } },
      axisLabel: { color: muted },
    },
    series: [
      { name: 'P', type: 'bar', data: mk((p) => p.avgPronunciation) },
      { name: 'G', type: 'bar', data: mk((p) => p.avgGrammar) },
      { name: 'C', type: 'bar', data: mk((p) => p.avgCoherence) },
      { name: 'F', type: 'bar', data: mk((p) => p.avgFluency) },
      { name: 'I', type: 'bar', data: mk((p) => p.avgIdeas) },
    ],
  }
})

const lineOption = computed(() => {
  const pts = props.stats?.recentScores
  if (!pts?.length) return {}
  const dates = pts.map((p) => formatShortDate(p.startedAtMillis))
  const vals = pts.map((p) => round1(p.overallBand))
  const parts = pts.map((p) => p.partLabel)
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      textStyle: { color: '#14532d' },
      formatter(params) {
        const i = params[0]?.dataIndex
        const part = parts[i] ?? ''
        return `${params[0]?.axisValue}<br/>Overall: ${params[0]?.data}<br/>${part}`
      },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: muted } },
      axisLabel: { color: muted, fontSize: 10, rotate: 35 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 9,
      splitLine: { lineStyle: { color: 'rgba(74, 222, 128, 0.12)' } },
      axisLabel: { color: muted },
    },
    series: [
      {
        name: 'Overall',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 2, color: accent },
        itemStyle: { color: accent, borderColor: '#14532d', borderWidth: 1 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(74, 222, 128, 0.35)' },
              { offset: 1, color: 'rgba(74, 222, 128, 0.02)' },
            ],
          },
        },
        data: vals,
      },
    ],
  }
})

const showCharts = computed(() => hasData.value && !props.loading)
</script>

<template>
  <el-card v-if="loading" class="dash-card" shadow="hover">
    <el-skeleton animated :rows="8" />
  </el-card>

  <el-card v-else-if="stats && !hasData" class="dash-card empty" shadow="hover">
    <template #header>
      <span class="dash-title">练习概览</span>
    </template>
    <el-empty description="暂无已完成并评分的练习，结束一轮并生成评价后将显示统计" />
  </el-card>

  <div v-else-if="showCharts" class="dashboard">
    <el-card class="dash-card" shadow="hover">
      <template #header>
        <span class="dash-title">练习概览</span>
      </template>
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="24" :sm="8">
          <div class="stat-box">
            <div class="stat-label">已完成会话</div>
            <div class="stat-value">{{ stats.completedSessionCount }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-box">
            <div class="stat-label">练习过的话题数（去重）</div>
            <div class="stat-value">{{ stats.distinctTopicCount }}</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-box">
            <div class="stat-label">Overall 总平均分</div>
            <div class="stat-value">
              {{ stats.overallAvgBand != null ? round1(stats.overallAvgBand) : '—' }}
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="dash-card chart-wrap" shadow="hover">
          <template #header>
            <span class="chart-head">各 Part 练习次数占比</span>
          </template>
          <v-chart class="chart" :option="pieOption" autoresize />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="dash-card chart-wrap" shadow="hover">
          <template #header>
            <span class="chart-head">各 Part 平均 Overall 分</span>
          </template>
          <v-chart class="chart" :option="bandBarOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card class="dash-card chart-wrap" shadow="hover">
          <template #header>
            <span class="chart-head">各 Part 五项评分均分（P/G/C/F/I）</span>
          </template>
          <v-chart class="chart chart-tall" :option="rubricBarOption" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="24">
        <el-card class="dash-card chart-wrap" shadow="hover">
          <template #header>
            <span class="chart-head">近期 Overall 走势（最近 48 次已完成）</span>
          </template>
          <v-chart
            v-if="stats.recentScores?.length"
            class="chart chart-tall"
            :option="lineOption"
            autoresize
          />
          <p v-else class="muted small">暂无带有效 Overall 分数的记录</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  margin-bottom: 0.5rem;
}

.dash-card {
  margin-bottom: 1rem;
  border-radius: 14px;
}

.dash-title {
  font-weight: 600;
  font-size: 1.05rem;
  color: #ecfdf5;
}

.chart-head {
  font-size: 0.95rem;
  font-weight: 600;
  color: rgba(209, 250, 229, 0.95);
}

.stat-row {
  margin-top: 0;
}

.stat-box {
  padding: 0.5rem 0.75rem;
  border-radius: 10px;
  background: rgba(6, 40, 28, 0.35);
  border: 1px solid rgba(74, 222, 128, 0.18);
}

.stat-label {
  font-size: 0.82rem;
  color: rgba(167, 243, 208, 0.75);
  margin-bottom: 0.35rem;
}

.stat-value {
  font-size: 1.65rem;
  font-weight: 700;
  color: #ecfdf5;
  letter-spacing: 0.02em;
}

.chart-row {
  margin-bottom: 0;
}

.chart-wrap :deep(.el-card__body) {
  padding-top: 0.5rem;
}

.chart {
  height: 260px;
  width: 100%;
}

.chart-tall {
  height: 300px;
}

.empty {
  margin-bottom: 1rem;
}

.muted.small {
  margin: 0;
  font-size: 0.88rem;
  color: rgba(167, 243, 208, 0.65);
}
</style>
