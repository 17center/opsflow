<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" style="color: #f56c6c">{{ stats.activeAlerts }}</div>
          <div class="stat-label">当前告警中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" style="color: #409eff">{{ stats.todayAlerts }}</div>
          <div class="stat-label">今日新增</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" style="color: #e6a23c">{{ stats.byLevel.high }}</div>
          <div class="stat-label">高危未处理</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-num" style="color: #67c23a">{{ stats.byLevel.urgent }}</div>
          <div class="stat-label">紧急告警</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in EVENT_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="query.alertLevel" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in ALERT_LEVEL_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="指标/主机" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="ruleName" label="规则" min-width="140" show-overflow-tooltip />
        <el-table-column label="指标" width="120" align="center">
          <template #default="{ row }">{{ row.metric }}</template>
        </el-table-column>
        <el-table-column label="当前值/阈值" width="130" align="center">
          <template #default="{ row }">
            <span style="color: #f56c6c">{{ row.currentValue }}</span> / {{ row.threshold }}
          </template>
        </el-table-column>
        <el-table-column label="所在主机" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostName || '-' }}</template>
        </el-table-column>
        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="ALERT_LEVEL_TAG[row.alertLevel]" size="small">{{ row.alertLevelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="EVENT_STATUS_TAG[row.status]" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confirmUserName" label="确认人" width="90" align="center">
          <template #default="{ row }">{{ row.confirmUserName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="触发时间" width="165" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" link type="success" @click="handleConfirm(row)">确认</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" link type="warning" @click="handleSilence(row)">静默</el-button>
            <el-button v-if="row.status !== 3" link type="info" @click="handleRecover(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="query.current"
        :page-size="query.size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="告警详情" width="520px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="规则">{{ detail.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="指标">{{ detail.metric }}</el-descriptions-item>
        <el-descriptions-item label="当前值">{{ detail.currentValue }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ detail.threshold }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="ALERT_LEVEL_TAG[detail.alertLevel]" size="small">{{ detail.alertLevelName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="EVENT_STATUS_TAG[detail.status]" size="small">{{ detail.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所在主机">{{ detail.hostName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="触发时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="确认人">{{ detail.confirmUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detail.confirmTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="恢复时间">{{ detail.recoverTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="静默截止">{{ detail.silenceUntil || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  pageAlertEvents,
  getAlertEvent,
  confirmAlertEvent,
  silenceAlertEvent,
  recoverAlertEvent,
  getAlertStats,
  ALERT_LEVEL_TEXT,
  ALERT_LEVEL_TAG,
  EVENT_STATUS_TEXT,
  EVENT_STATUS_TAG,
  type AlertEvent,
  type AlertStats
} from '@/api/alert'

const loading = ref(false)
const list = ref<AlertEvent[]>([])
const total = ref(0)
const stats = ref<AlertStats>({ activeAlerts: 0, todayAlerts: 0, byLevel: { urgent: 0, high: 0, medium: 0, low: 0 }, topHosts: [] })
const detailVisible = ref(false)
const detail = ref<AlertEvent | null>(null)

const query = reactive<{ current: number; size: number; status?: number; alertLevel?: number; keyword: string }>({
  current: 1,
  size: 10,
  status: 1,
  alertLevel: undefined,
  keyword: ''
})

async function loadStats() {
  stats.value = await getAlertStats()
}

async function load() {
  loading.value = true
  try {
    const res = await pageAlertEvents({
      current: query.current,
      size: query.size,
      status: query.status,
      alertLevel: query.alertLevel,
      keyword: query.keyword || undefined
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.status = 1
  query.alertLevel = undefined
  query.keyword = ''
  handleSearch()
}

function handlePageChange(page: number) {
  query.current = page
  load()
}

function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  load()
}

async function openDetail(row: AlertEvent) {
  detail.value = await getAlertEvent(row.id)
  detailVisible.value = true
}

async function handleConfirm(row: AlertEvent) {
  await confirmAlertEvent(row.id)
  ElMessage.success('已确认')
  load()
  loadStats()
}

async function handleSilence(row: AlertEvent) {
  const { value } = await ElMessageBox.prompt('静默时长（分钟）', '静默告警', {
    inputValue: '60',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入数字'
  })
  await silenceAlertEvent(row.id, Number(value))
  ElMessage.success('已静默')
  load()
  loadStats()
}

async function handleRecover(row: AlertEvent) {
  await ElMessageBox.confirm('确认该告警已恢复？', '提示', { type: 'info' })
  await recoverAlertEvent(row.id)
  ElMessage.success('已恢复')
  load()
  loadStats()
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>