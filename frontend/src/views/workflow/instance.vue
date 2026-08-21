<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="工单ID">
          <el-input v-model="query.ticketId" placeholder="工单ID" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in INST_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="实例ID" width="80" />
        <el-table-column prop="wfDefName" label="流程名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.wfDefVersion || 1 }}</template>
        </el-table-column>
        <el-table-column prop="ticketNo" label="关联工单" width="190" />
        <el-table-column prop="ticketTitle" label="工单标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="INST_STATUS_TAG[row.status ?? -1] || 'info'" size="small">{{ row.statusName || instStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="启动时间" width="165" />
        <el-table-column prop="endTime" label="结束时间" width="165">
          <template #default="{ row }">{{ row.endTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="流程实例详情" size="600px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-header">
          <h3 class="detail-title">{{ detail.wfDefName }}</h3>
          <div class="detail-tags">
            <el-tag size="small">v{{ detail.wfDefVersion }}</el-tag>
            <el-tag size="small" :type="INST_STATUS_TAG[detail.status ?? -1] || 'info'">{{ detail.statusName }}</el-tag>
          </div>
          <div class="detail-meta">
            <span>实例ID：{{ detail.id }}</span>
            <span>关联工单：{{ detail.ticketNo }}（{{ detail.ticketTitle }}）</span>
            <span>启动时间：{{ detail.startTime }}</span>
            <span>结束时间：{{ detail.endTime || '-' }}</span>
          </div>
        </div>

        <el-divider content-position="left">审批轨迹</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="t in detail.tasks"
            :key="t.taskId"
            :timestamp="t.completeTime || t.dueTime"
            :color="taskColor(t.status)"
          >
            <div class="task-item">
              <b>{{ t.taskName }}</b>
              <el-tag size="small" :type="TASK_STATUS_TAG[t.status ?? -1] || 'info'">{{ t.statusName || taskStatusText(t.status) }}</el-tag>
            </div>
            <div class="task-meta">
              <span>审批人：{{ t.assigneeName || '-' }}</span>
              <span v-if="t.action">动作：{{ t.action }}</span>
            </div>
            <div v-if="t.comment" class="task-comment">意见：{{ t.comment }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!detail.tasks?.length" description="暂无审批记录" :image-size="60" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  pageInstances,
  getInstance,
  INST_STATUS_TEXT,
  INST_STATUS_TAG,
  TASK_STATUS_TEXT,
  TASK_STATUS_TAG,
  type WfInstance
} from '@/api/workflow'

const loading = ref(false)
const list = ref<WfInstance[]>([])
const total = ref(0)

const query = reactive<{ current: number; size: number; ticketId?: number; status?: number }>({
  current: 1,
  size: 10,
  ticketId: undefined,
  status: undefined
})

const drawerVisible = ref(false)
const detail = ref<WfInstance | null>(null)

function taskColor(status?: number) {
  if (status === 2) return '#67c23a'
  if (status === 3 || status === 5) return '#f56c6c'
  if (status === 4) return '#909399'
  return '#409eff'
}

function instStatusText(status?: number) {
  return status != null ? INST_STATUS_TEXT[status] : ''
}

function taskStatusText(status?: number) {
  return status != null ? TASK_STATUS_TEXT[status] : ''
}

async function load() {
  loading.value = true
  try {
    // 后端按 ticketId 查询；此处通过工单编号无法直接查，前端仅支持按状态筛选
    const res = await pageInstances({
      current: query.current,
      size: query.size,
      ticketId: query.ticketId,
      status: query.status
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
  query.ticketId = undefined
  query.status = undefined
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

async function openDetail(id: number) {
  drawerVisible.value = true
  detail.value = await getInstance(id)
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-header {
  margin-bottom: 8px;
}

.detail-title {
  margin: 0 0 8px;
}

.detail-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #606266;
  font-size: 13px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-meta {
  color: #909399;
  font-size: 13px;
  margin-top: 2px;
}

.task-comment {
  color: #606266;
  font-size: 13px;
  margin-top: 2px;
}
</style>