<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num">{{ data?.total ?? '-' }}</div>
          <div class="stat-label">工单总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num warning">{{ data?.pendingApproval ?? '-' }}</div>
          <div class="stat-label">待审批</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num primary">{{ data?.inProgress ?? '-' }}</div>
          <div class="stat-label">处理中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num success">{{ data?.resolved ?? '-' }}</div>
          <div class="stat-label">已解决</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num">{{ data?.avgMttrHours ?? '-' }}</div>
          <div class="stat-label">平均解决时长（小时）</div>
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num">{{ data?.slaComplianceRate ?? '-' }}<span class="unit">%</span></div>
          <div class="stat-label">SLA 达标率</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>工单类型分布</template>
          <div ref="typeChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>工单优先级分布</template>
          <div ref="priorityChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, type DashboardData } from '@/api/ticket'

const data = ref<DashboardData | null>(null)
const typeChartRef = ref<HTMLDivElement>()
const priorityChartRef = ref<HTMLDivElement>()
let typeChart: echarts.ECharts | null = null
let priorityChart: echarts.ECharts | null = null

function renderTypeChart() {
  if (!typeChartRef.value || !data.value) return
  typeChart = typeChart || echarts.init(typeChartRef.value)
  typeChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: '62%',
        center: ['50%', '45%'],
        data: Object.entries(data.value.byType).map(([name, value]) => ({ name, value })),
        label: { formatter: '{b}: {c}' }
      }
    ]
  })
}

function renderPriorityChart() {
  if (!priorityChartRef.value || !data.value) return
  priorityChart = priorityChart || echarts.init(priorityChartRef.value)
  priorityChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(data.value.byPriority) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        barWidth: 40,
        itemStyle: {
          color: (params: { dataIndex: number }) => {
            const colors = ['#f56c6c', '#e6a23c', '#409eff', '#909399']
            return colors[params.dataIndex] || '#409eff'
          }
        },
        data: Object.values(data.value.byPriority)
      }
    ]
  })
}

function handleResize() {
  typeChart?.resize()
  priorityChart?.resize()
}

onMounted(async () => {
  data.value = await getDashboard()
  renderTypeChart()
  renderPriorityChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  priorityChart?.dispose()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 30px;
  font-weight: 700;
  color: #303133;
}

.stat-num.warning {
  color: #e6a23c;
}

.stat-num.primary {
  color: #409eff;
}

.stat-num.success {
  color: #67c23a;
}

.stat-num .unit {
  font-size: 16px;
}

.stat-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.mt16 {
  margin-top: 16px;
}

.chart {
  height: 320px;
}
</style>