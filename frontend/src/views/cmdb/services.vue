<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="服务名/类型" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="query.serviceType" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="t in SERVICE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in SERVICE_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增服务</el-button>
        <el-button :icon="MagicStick" @click="openDiscover">自动发现</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="服务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.serviceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" align="center">
          <template #default="{ row }">{{ row.version || '-' }}</template>
        </el-table-column>
        <el-table-column label="所在主机" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.hostName ? `${row.hostName}（${row.hostIp}）` : '-' }}</template>
        </el-table-column>
        <el-table-column prop="port" label="端口" width="80" align="center">
          <template #default="{ row }">{{ row.port ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="SERVICE_STATUS_TAG[row.status] || 'info'" size="small">{{ row.statusName || SERVICE_STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="100" align="center">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown style="margin-left: 4px" @command="(cmd: number) => handleStatus(row, cmd)">
              <el-button link type="warning">状态</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="1">运行中</el-dropdown-item>
                  <el-dropdown-item :command="2">维护中</el-dropdown-item>
                  <el-dropdown-item :command="0">不可用</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
    <el-dialog v-model="editVisible" :title="editingId ? '编辑服务' : '新增服务'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" placeholder="如：订单库" maxlength="128" />
        </el-form-item>
        <el-form-item label="服务类型" prop="serviceType">
          <el-select v-model="form.serviceType" filterable allow-create placeholder="选择/输入类型" style="width: 100%">
            <el-option v-for="t in SERVICE_TYPES" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.version" placeholder="如：8.0.33" maxlength="32" />
        </el-form-item>
        <el-form-item label="所在主机">
          <el-select v-model="form.hostId" filterable clearable placeholder="选择主机" style="width: 100%">
            <el-option v-for="h in hostOptions" :key="h.id" :label="`${h.hostname}（${h.ipAddress}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务端口">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
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

    <!-- 自动发现弹窗 -->
    <el-dialog v-model="discoverVisible" title="自动发现服务" width="560px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="目标主机">
          <el-select v-model="discoverHostId" filterable placeholder="选择主机" style="width: 100%">
            <el-option v-for="h in hostOptions" :key="h.id" :label="`${h.hostname}（${h.ipAddress}）`" :value="h.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="discovering" @click="handleDiscover">开始扫描</el-button>
        </el-form-item>
      </el-form>
      <div v-if="discovered.length" class="discover-result">
        <div class="discover-title">探测到如下服务，确认后批量录入：</div>
        <el-table :data="discovered" border size="small">
          <el-table-column prop="serviceType" label="类型" width="110" />
          <el-table-column prop="port" label="端口" width="80" align="center" />
          <el-table-column prop="name" label="建议名称" />
        </el-table>
        <div class="discover-actions">
          <el-button type="primary" :loading="saving" @click="handleBatchCreate">批量录入</el-button>
        </div>
      </div>
      <el-empty v-else-if="discoveredDone" description="未探测到可录入的新服务" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, MagicStick } from '@element-plus/icons-vue'
import {
  pageServices,
  createService,
  updateService,
  deleteService,
  changeServiceStatus,
  discoverServices,
  batchCreateServices,
  SERVICE_STATUS_TEXT,
  SERVICE_STATUS_TAG,
  SERVICE_TYPES,
  type CmdbService,
  type CmdbServiceForm,
  type DiscoveredService
} from '@/api/cmdb'
import { pageHosts, type CmdbHost } from '@/api/automation'

const loading = ref(false)
const saving = ref(false)
const discovering = ref(false)
const list = ref<CmdbService[]>([])
const total = ref(0)
const hostOptions = ref<CmdbHost[]>([])

const query = reactive<{ current: number; size: number; keyword: string; serviceType?: string; status?: number }>({
  current: 1,
  size: 10,
  keyword: '',
  serviceType: undefined,
  status: undefined
})

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<CmdbServiceForm>({
  name: '',
  serviceType: 'MySQL',
  version: '',
  hostId: undefined,
  port: undefined,
  remark: ''
})

const discoverVisible = ref(false)
const discoverHostId = ref<number>()
const discovered = ref<DiscoveredService[]>([])
const discoveredDone = ref(false)

const rules: FormRules = {
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }]
}

async function loadHosts() {
  hostOptions.value = (await pageHosts({ current: 1, size: 100 })).records
}

async function load() {
  loading.value = true
  try {
    const res = await pageServices({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      serviceType: query.serviceType,
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
  query.keyword = ''
  query.serviceType = undefined
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

function openCreate() {
  editingId.value = undefined
  form.name = ''
  form.serviceType = 'MySQL'
  form.version = ''
  form.hostId = undefined
  form.port = undefined
  form.remark = ''
  editVisible.value = true
}

function openEdit(row: CmdbService) {
  editingId.value = row.id
  form.name = row.name
  form.serviceType = row.serviceType
  form.version = row.version || ''
  form.hostId = row.hostId
  form.port = row.port
  form.remark = row.remark || ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateService(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createService({ ...form })
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: CmdbService, status: number) {
  await changeServiceStatus(row.id, status)
  ElMessage.success('状态已更新')
  load()
}

async function handleDelete(row: CmdbService) {
  await ElMessageBox.confirm(`确定删除服务「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteService(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openDiscover() {
  await loadHosts()
  discoverHostId.value = undefined
  discovered.value = []
  discoveredDone.value = false
  discoverVisible.value = true
}

async function handleDiscover() {
  if (!discoverHostId.value) {
    ElMessage.warning('请先选择目标主机')
    return
  }
  discovering.value = true
  try {
    discovered.value = await discoverServices(discoverHostId.value)
    discoveredDone.value = true
    if (!discovered.value.length) {
      ElMessage.info('未探测到可录入的新服务')
    }
  } finally {
    discovering.value = false
  }
}

async function handleBatchCreate() {
  if (!discovered.value.length) return
  saving.value = true
  try {
    const data: CmdbServiceForm[] = discovered.value.map((d) => ({
      name: d.name,
      serviceType: d.serviceType,
      hostId: d.hostId,
      port: d.port,
      status: 1
    }))
    await batchCreateServices(data)
    ElMessage.success('批量录入成功')
    discoverVisible.value = false
    load()
  } finally {
    saving.value = false
  }
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

.discover-result {
  margin-top: 8px;
}

.discover-title {
  margin-bottom: 8px;
  font-weight: 600;
}

.discover-actions {
  margin-top: 12px;
  text-align: right;
}
</style>