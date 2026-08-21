<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="规则名/指标" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在主机">
          <el-select v-model="query.hostId" placeholder="全部" clearable filterable style="width: 160px">
            <el-option v-for="h in hostOptions" :key="h.id" :label="h.hostname" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="规则名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="metric" label="指标" width="120" align="center">
          <template #default="{ row }">{{ METRIC_TEXT[row.metric] || row.metric }}</template>
        </el-table-column>
        <el-table-column label="条件" width="130" align="center">
          <template #default="{ row }">{{ row.operator }} {{ row.threshold }}%</template>
        </el-table-column>
        <el-table-column label="持续时间" width="100" align="center">
          <template #default="{ row }">{{ row.durationSeconds }}s</template>
        </el-table-column>
        <el-table-column label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="ALERT_LEVEL_TAG[row.alertLevel]" size="small">{{ row.alertLevelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所在主机" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostName || '全局' }}</template>
        </el-table-column>
        <el-table-column label="通知渠道" min-width="120">
          <template #default="{ row }">
            <el-tag v-for="c in parseChannels(row.notifyChannels)" :key="c" size="small" style="margin-right: 4px">{{ c }}</el-tag>
            <span v-if="!row.notifyChannels">值班人</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑规则' : '新增规则'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="form.name" placeholder="如：CPU 使用率告警" maxlength="128" />
        </el-form-item>
        <el-form-item label="监控指标" prop="metric">
          <el-select v-model="form.metric" style="width: 100%">
            <el-option v-for="m in METRICS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值条件" required>
          <div class="threshold-row">
            <el-select v-model="form.operator" style="width: 90px">
              <el-option v-for="op in OPERATORS" :key="op" :value="op">{{ op }}</el-option>
            </el-select>
            <el-input-number v-model="form.threshold" :min="0" :max="100" :precision="2" controls-position="right" style="width: 140px" />
            <span class="unit">%</span>
          </div>
        </el-form-item>
        <el-form-item label="持续时间">
          <el-input-number v-model="form.durationSeconds" :min="1" :max="86400" /> 秒
        </el-form-item>
        <el-form-item label="告警级别" prop="alertLevel">
          <el-select v-model="form.alertLevel" style="width: 100%">
            <el-option v-for="(v, k) in ALERT_LEVEL_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在主机">
          <el-select v-model="form.hostId" filterable clearable placeholder="留空表示全局规则" style="width: 100%">
            <el-option v-for="h in hostOptions" :key="h.id" :label="`${h.hostname}（${h.ipAddress}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道">
          <el-select v-model="form.notifyChannels" multiple placeholder="留空表示通知当日值班人" style="width: 100%">
            <el-option v-for="c in CHANNELS" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  pageAlertRules,
  createAlertRule,
  updateAlertRule,
  deleteAlertRule,
  changeAlertRuleStatus,
  ALERT_LEVEL_TEXT,
  ALERT_LEVEL_TAG,
  METRICS,
  OPERATORS,
  CHANNELS,
  type AlertRule,
  type AlertRuleForm
} from '@/api/alert'
import { pageHosts, type CmdbHost } from '@/api/automation'

const METRIC_TEXT = METRICS.reduce((acc, m) => {
  acc[m.value] = m.label
  return acc
}, {} as Record<string, string>)

const loading = ref(false)
const saving = ref(false)
const list = ref<AlertRule[]>([])
const total = ref(0)
const hostOptions = ref<CmdbHost[]>([])

const query = reactive<{ current: number; size: number; keyword: string; status?: number; hostId?: number }>({
  current: 1,
  size: 10,
  keyword: '',
  status: undefined,
  hostId: undefined
})

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<AlertRuleForm>({
  name: '',
  metric: 'cpu_usage',
  operator: '>',
  threshold: 90,
  durationSeconds: 300,
  alertLevel: 1,
  notifyChannels: [] as unknown as string,
  hostId: undefined,
  remark: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  metric: [{ required: true, message: '请选择监控指标', trigger: 'change' }],
  alertLevel: [{ required: true, message: '请选择告警级别', trigger: 'change' }]
}

function parseChannels(ch: string | undefined): string[] {
  if (!ch) return []
  return ch.split(',').filter(Boolean)
}

async function loadHosts() {
  hostOptions.value = (await pageHosts({ current: 1, size: 100 })).records
}

async function load() {
  loading.value = true
  try {
    const res = await pageAlertRules({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      hostId: query.hostId
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
  query.keyword = ''
  query.status = undefined
  query.hostId = undefined
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

function openCreate() {
  editingId.value = undefined
  form.name = ''
  form.metric = 'cpu_usage'
  form.operator = '>'
  form.threshold = 90
  form.durationSeconds = 300
  form.alertLevel = 1
  form.notifyChannels = [] as unknown as string
  form.hostId = undefined
  form.remark = ''
  editVisible.value = true
}

function openEdit(row: AlertRule) {
  editingId.value = row.id
  form.name = row.name
  form.metric = row.metric
  form.operator = row.operator
  form.threshold = row.threshold
  form.durationSeconds = row.durationSeconds
  form.alertLevel = row.alertLevel
  form.notifyChannels = (parseChannels(row.notifyChannels) as unknown as string)
  form.hostId = row.hostId
  form.remark = row.remark || ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  const notifyChannels = Array.isArray(form.notifyChannels) ? form.notifyChannels.join(',') : form.notifyChannels
  const payload: AlertRuleForm = { ...form, notifyChannels }
  saving.value = true
  try {
    if (editingId.value) {
      await updateAlertRule(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createAlertRule(payload)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: AlertRule) {
  const status = row.status === 1 ? 0 : 1
  await changeAlertRuleStatus(row.id, status)
  ElMessage.success(status === 1 ? '已启用' : '已停用')
  load()
}

async function handleDelete(row: AlertRule) {
  await ElMessageBox.confirm(`确定删除规则「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteAlertRule(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadHosts()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.threshold-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit {
  margin-left: 4px;
}
</style>