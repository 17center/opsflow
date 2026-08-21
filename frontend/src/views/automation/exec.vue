<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="脚本">
          <el-select v-model="query.scriptId" filterable clearable placeholder="选择脚本" style="width: 180px">
            <el-option v-for="s in scriptOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机">
          <el-select v-model="query.hostId" filterable clearable placeholder="选择主机" style="width: 180px">
            <el-option v-for="h in hostOptions" :key="h.id" :label="`${h.hostname}（${h.ipAddress}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in EXEC_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="VideoPlay" @click="openStart">发起执行</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="scriptName" label="脚本" min-width="150" show-overflow-tooltip />
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.scriptVersion }}</template>
        </el-table-column>
        <el-table-column prop="hostname" label="目标主机" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostname }}（{{ row.hostIp }}）</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="EXEC_STATUS_TAG[row.status] || 'info'" size="small">{{ row.statusName || EXEC_STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退出码" width="80" align="center">
          <template #default="{ row }">{{ row.exitCode ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="center">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
        <el-table-column label="触发" width="110" align="center">
          <template #default="{ row }">{{ row.triggerTypeName || TRIGGER_TYPE_TEXT[row.triggerType] }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" align="center" />
        <el-table-column prop="createTime" label="执行时间" width="165" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1 || row.status === 2" link type="danger" @click="handleCancel(row)">取消</el-button>
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

    <!-- 发起执行弹窗 -->
    <el-dialog v-model="startVisible" title="发起执行" width="520px" destroy-on-close>
      <el-form ref="startFormRef" :model="startForm" :rules="startRules" label-width="90px">
        <el-form-item label="脚本" prop="scriptId">
          <el-select v-model="startForm.scriptId" filterable placeholder="选择脚本" style="width: 100%">
            <el-option v-for="s in scriptOptions" :key="s.id" :label="`${s.name}（${SCRIPT_TYPE_TEXT[s.scriptType]} v${s.currentVersion || 1}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标主机" prop="hostId">
          <el-select v-model="startForm.hostId" filterable placeholder="选择主机" style="width: 100%">
            <el-option v-for="h in hostOptions" :key="h.id" :label="`${h.hostname}（${h.ipAddress}）`" :value="h.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" :loading="starting" @click="handleStart">执行</el-button>
      </template>
    </el-dialog>

    <!-- 执行详情弹窗 -->
    <el-drawer v-model="detailVisible" title="执行详情" size="720px" destroy-on-close>
      <template #default>
        <el-descriptions v-if="detail" :column="2" border size="small">
          <el-descriptions-item label="脚本">{{ detail.record.scriptName }}</el-descriptions-item>
          <el-descriptions-item label="版本">v{{ detail.record.scriptVersion }}</el-descriptions-item>
          <el-descriptions-item label="目标主机">{{ detail.record.hostname }}（{{ detail.record.hostIp }}）</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="EXEC_STATUS_TAG[detail.record.status] || 'info'" size="small">{{ detail.record.statusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退出码">{{ detail.record.exitCode ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ formatDuration(detail.record.durationMs) }}</el-descriptions-item>
          <el-descriptions-item label="触发方式">{{ detail.record.triggerTypeName }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ detail.record.operatorName }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.record.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ detail.record.endTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.record.errorMessage" label="错误信息" :span="2">
            <span class="err">{{ detail.record.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="log-header">
          <span>执行输出</span>
          <span class="log-status" :class="wsConnected ? 'online' : 'offline'">
            {{ wsConnected ? '实时推送已连接' : '实时推送未连接' }}
          </span>
        </div>
        <div ref="logBoxRef" class="log-box" v-loading="logLoading">
          <template v-if="logLines.length">
            <div v-for="l in logLines" :key="l.key" class="log-line" :class="logLineClass(l.streamType)">
              <span class="log-no">{{ l.lineNumber }}</span>
              <span class="log-content">{{ l.content }}</span>
            </div>
          </template>
          <el-empty v-else description="暂无日志" :image-size="60" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { VideoPlay, Refresh } from '@element-plus/icons-vue'
import {
  pageExecs,
  getExecDetail,
  startExec,
  cancelExec,
  pageScripts,
  pageHosts,
  EXEC_STATUS_TEXT,
  EXEC_STATUS_TAG,
  TRIGGER_TYPE_TEXT,
  SCRIPT_TYPE_TEXT,
  type AutoScript,
  type CmdbHost,
  type AutoExecRecord,
  type AutoExecDetail,
  type AutoExecLog
} from '@/api/automation'
import { getToken } from '@/utils/request'

const loading = ref(false)
const starting = ref(false)
const logLoading = ref(false)
const list = ref<AutoExecRecord[]>([])
const total = ref(0)
const scriptOptions = ref<AutoScript[]>([])
const hostOptions = ref<CmdbHost[]>([])

const query = reactive<{ current: number; size: number; scriptId?: number; hostId?: number; status?: number }>({
  current: 1,
  size: 10,
  scriptId: undefined,
  hostId: undefined,
  status: undefined
})

// 发起执行
const startVisible = ref(false)
const startFormRef = ref<FormInstance>()
const startForm = reactive<{ scriptId?: number; hostId?: number }>({ scriptId: undefined, hostId: undefined })
const startRules: FormRules = {
  scriptId: [{ required: true, message: '请选择脚本', trigger: 'change' }],
  hostId: [{ required: true, message: '请选择目标主机', trigger: 'change' }]
}

// 详情
const detailVisible = ref(false)
const detail = ref<AutoExecDetail | null>(null)
const logLines = ref<Array<{ key: number; lineNumber: number; content: string; streamType: number }>>([])
const logBoxRef = ref<HTMLElement>()
const wsConnected = ref(false)
let ws: WebSocket | null = null
let lineKey = 0

async function loadOptions() {
  const [scripts, hosts] = await Promise.all([
    pageScripts({ current: 1, size: 100 }),
    pageHosts({ current: 1, size: 100 })
  ])
  scriptOptions.value = scripts.records
  hostOptions.value = hosts.records
}

async function load() {
  loading.value = true
  try {
    const res = await pageExecs({
      current: query.current,
      size: query.size,
      scriptId: query.scriptId,
      hostId: query.hostId,
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
  query.scriptId = undefined
  query.hostId = undefined
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

async function openStart() {
  await loadOptions()
  startForm.scriptId = undefined
  startForm.hostId = undefined
  startVisible.value = true
}

async function handleStart() {
  await startFormRef.value?.validate()
  starting.value = true
  try {
    const res = await startExec({
      scriptId: startForm.scriptId!,
      hostId: startForm.hostId!,
      triggerType: 2
    })
    ElMessage.success('已提交执行')
    startVisible.value = false
    load()
    // 提交后自动打开详情实时查看
    openDetail({ id: res.id } as AutoExecRecord)
  } finally {
    starting.value = false
  }
}

async function handleCancel(row: AutoExecRecord) {
  await ElMessageBox.confirm(`确定取消执行 #${row.id} 吗？`, '提示', { type: 'warning' })
  await cancelExec(row.id)
  ElMessage.success('已取消')
  load()
}

function formatDuration(ms?: number): string {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

function logLineClass(streamType: number): string {
  return streamType === 2 ? 'stderr' : ''
}

function scrollToBottom() {
  nextTick(() => {
    if (logBoxRef.value) {
      logBoxRef.value.scrollTop = logBoxRef.value.scrollHeight
    }
  })
}

function connectWs(recordId: number) {
  closeWs()
  const token = getToken()
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  ws = new WebSocket(`${protocol}://${location.host}/ws/exec?recordId=${recordId}&token=${token}`)
  ws.onopen = () => {
    wsConnected.value = true
  }
  ws.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data)
      if (data.content) {
        logLines.value.push({
          key: ++lineKey,
          lineNumber: data.line || logLines.value.length + 1,
          content: data.content,
          streamType: data.streamType || 1
        })
        scrollToBottom()
      }
    } catch {
      // 忽略非 JSON 消息
    }
  }
  ws.onclose = () => {
    wsConnected.value = false
  }
  ws.onerror = () => {
    wsConnected.value = false
  }
}

function closeWs() {
  wsConnected.value = false
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
}

async function openDetail(row: AutoExecRecord) {
  logLines.value = []
  lineKey = 0
  detailVisible.value = true
  logLoading.value = true
  try {
    detail.value = await getExecDetail(row.id)
    logLines.value = detail.value.logs.map((l: AutoExecLog) => ({
      key: ++lineKey,
      lineNumber: l.lineNumber,
      content: l.content,
      streamType: l.streamType
    }))
    scrollToBottom()
    // 若仍在执行中则开启实时推送
    if (row.status === 1 || row.status === 2) {
      connectWs(row.id)
    }
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  load()
  loadOptions()
})

onBeforeUnmount(closeWs)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.err {
  color: #f56c6c;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0 8px;
  font-weight: 600;
}

.log-status {
  font-size: 12px;
  font-weight: normal;
}

.log-status.online {
  color: #67c23a;
}

.log-status.offline {
  color: #909399;
}

.log-box {
  height: 420px;
  overflow-y: auto;
  background: #1e1f22;
  border-radius: 4px;
  padding: 8px 12px;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.log-line {
  display: flex;
  color: #d4d4d4;
}

.log-line.stderr {
  color: #f56c6c;
}

.log-no {
  width: 42px;
  flex-shrink: 0;
  color: #6a6d73;
  user-select: none;
}

.log-content {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>