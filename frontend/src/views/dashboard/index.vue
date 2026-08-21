<template>
  <div>
    <!-- 时间范围 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetRange">近30天</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="mt-16">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-desc">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="stat-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="mt-16">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>工单趋势（创建 / 解决）</template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>SLA 达标情况</template>
          <div ref="slaRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, type ReportDashboard } from '@/api/report'

const range = ref<[string, string]>()
const data = ref<ReportDashboard>()

const trendRef = ref<HTMLElement>()
const slaRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let slaChart: echarts.ECharts | null = null

const statCards = computed(() => {
  const d = data.value
  return [
    {
      label: '工单总数',
      value: d?.ticketSummary.total ?? 0,
      sub: `区间内新建 ${d?.ticketSummary.created ?? 0}`,
      color: '#409eff'
    },
    {
      label: 'SLA 达标率',
      value: d ? `${d.slaCompliance.complianceRate}%` : '0%',
      sub: d ? `达标 ${d.slaCompliance.total - d.slaCompliance.breached}/${d.slaCompliance.total}` : '暂无数据',
      color: '#67c23a'
    },
    {
      label: '平均修复时长 MTTR',
      value: d ? `${d.ticketSummary.avgMttrHours}h` : '0h',
      sub: `已解决 ${d?.ticketSummary.resolved ?? 0}`,
      color: '#e6a23c'
    },
    {
      label: '自动化成功率',
      value: d ? `${d.autoExec.successRate}%` : '0%',
      sub: `成功 ${d?.autoExec.success ?? 0} / ${d?.autoExec.total ?? 0}`,
      color: '#f56c6c'
    }
  ]
})

async function load() {
  const [s, e] = range.value || []
  data.value = await getDashboard(s, e)
  renderCharts()
}

function resetRange() {
  range.value = undefined
  load()
}

function renderCharts() {
  const d = data.value
  if (!d) return
  const dates = d.ticketTrend.map((t) => t.date)
  const created = d.ticketTrend.map((t) => t.created)
  const resolved = d.ticketTrend.map((t) => t.resolved)

  if (trendChart) {
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['创建', '解决'] },
      grid: { left: 40, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '创建', type: 'line', smooth: true, data: created, areaStyle: { opacity: 0.15 } },
        { name: '解决', type: 'line', smooth: true, data: resolved, areaStyle: { opacity: 0.15 } }
      ]
    })
  }

  if (slaChart) {
    const ok = d.slaCompliance.total - d.slaCompliance.breached
    slaChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '45%'],
          data: [
            { name: '达标', value: ok, itemStyle: { color: '#67c23a' } },
            { name: '未达标', value: d.slaCompliance.breached, itemStyle: { color: '#f56c6c' } }
          ],
          label: { formatter: '{b}: {c} ({d}%)' }
        }
      ]
    })
  }
}

function initCharts() {
  trendChart = echarts.init(trendRef.value!)
  slaChart = echarts.init(slaRef.value!)
}

onMounted(() => {
  initCharts()
  load()
})

onBeforeUnmount(() => {
  trendChart?.dispose()
  slaChart?.dispose()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 0;
}

.mt-16 {
  margin-top: 16px;
}

.stat-card {
  text-align: center;
}

.stat-desc {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  margin: 8px 0;
}

.stat-sub {
  color: #c0c4cc;
  font-size: 12px;
}

.chart {
  height: 360px;
}
</style>