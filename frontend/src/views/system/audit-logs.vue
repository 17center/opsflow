<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="操作人" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 120px">
            <el-option label="用户" value="USER" />
            <el-option label="工单" value="TICKET" />
            <el-option label="脚本" value="SCRIPT" />
            <el-option label="告警" value="ALERT" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="danger" :icon="Delete" @click="handleClean">清理历史日志</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="90">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作描述" min-width="110" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="90" />
        <el-table-column label="参数" min-width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" min-width="170" />
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
    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.operation }}（{{ detail.module }}）</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.requestMethod }} {{ detail.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="结果">{{ detail.status === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMessage" label="失败原因">{{ detail.errorMessage }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="detail-pre">{{ pretty(detail.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应结果">
          <pre class="detail-pre">{{ pretty(detail.responseResult) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { pageAuditLogs, cleanAuditLogs, type AuditLogItem } from '@/api/system'

const loading = ref(false)
const list = ref<AuditLogItem[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const query = reactive({ current: 1, size: 10, username: '', module: '', status: undefined })

const detailVisible = ref(false)
const detail = ref<Partial<AuditLogItem>>({})

async function load() {
  loading.value = true
  try {
    const res = await pageAuditLogs({
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      module: query.module || undefined,
      status: query.status,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined
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
  query.username = ''
  query.module = ''
  query.status = undefined
  dateRange.value = null
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

function showDetail(row: AuditLogItem) {
  detail.value = row
  detailVisible.value = true
}

function pretty(str?: string) {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

async function handleClean() {
  const { value } = await ElMessageBox.prompt('清理该时间之前的所有日志（格式：yyyy-MM-dd HH:mm:ss）', '清理历史日志', {
    inputValue: '2026-01-01 00:00:00',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await cleanAuditLogs(value)
  ElMessage.success('清理完成')
  load()
}

onMounted(load)
</script>

<style scoped>
.search-form {
  margin-bottom: 0;
}
.toolbar {
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
.detail-pre {
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}
</style>