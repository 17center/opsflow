<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="主机名/IP" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="query.groupName" placeholder="分组" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in HOST_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增主机</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="hostname" label="主机名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP 地址" width="130" />
        <el-table-column prop="sshPort" label="SSH端口" width="80" align="center" />
        <el-table-column prop="osType" label="系统" width="90" align="center">
          <template #default="{ row }">{{ row.osType || '-' }}</template>
        </el-table-column>
        <el-table-column label="认证" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.authType === 2 ? 'warning' : 'info'" size="small">{{ row.authTypeName || AUTH_TYPE_TEXT[row.authType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="hostStatusTag(row.status)" size="small">{{ row.statusName || HOST_STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupName" label="分组" width="100" align="center">
          <template #default="{ row }">{{ row.groupName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="100" align="center">
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lastCheckTime" label="最后检查" width="165">
          <template #default="{ row }">{{ row.lastCheckTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :loading="testingId === row.id" @click="handleTest(row)">连接测试</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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
    <el-dialog v-model="editVisible" :title="editingId ? '编辑主机' : '新增主机'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="主机名" prop="hostname">
          <el-input v-model="form.hostname" placeholder="如：web-01" maxlength="128" />
        </el-form-item>
        <el-form-item label="IP 地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" placeholder="如：192.168.2.100" maxlength="45" />
        </el-form-item>
        <el-form-item label="SSH 端口" prop="sshPort">
          <el-input-number v-model="form.sshPort" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="系统类型">
          <el-select v-model="form.osType" clearable placeholder="选择系统" style="width: 100%">
            <el-option label="CentOS" value="CentOS" />
            <el-option label="Ubuntu" value="Ubuntu" />
            <el-option label="Debian" value="Debian" />
            <el-option label="Windows" value="Windows" />
            <el-option label="其他" value="Other" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-radio-group v-model="form.authType">
            <el-radio-button :value="1">密码</el-radio-button>
            <el-radio-button :value="2">密钥</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="凭据" prop="credential">
          <el-input
            v-model="form.credential"
            :type="form.authType === 1 ? 'password' : 'textarea'"
            :rows="form.authType === 2 ? 6 : undefined"
            :show-password="form.authType === 1"
            :placeholder="form.authType === 1 ? '请输入 root 密码' : '请粘贴私钥内容（含 BEGIN/END 行）'"
            class="credential-area"
          />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="form.groupName" placeholder="如：生产环境" maxlength="64" />
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
  pageHosts,
  createHost,
  updateHost,
  deleteHost,
  testHost,
  AUTH_TYPE_TEXT,
  HOST_STATUS_TEXT,
  type CmdbHost,
  type CmdbHostForm
} from '@/api/automation'

const loading = ref(false)
const saving = ref(false)
const testingId = ref<number>()
const list = ref<CmdbHost[]>([])
const total = ref(0)

const query = reactive<{ current: number; size: number; keyword: string; groupName: string; status?: number }>({
  current: 1,
  size: 10,
  keyword: '',
  groupName: '',
  status: undefined
})

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<CmdbHostForm>({
  hostname: '',
  ipAddress: '',
  sshPort: 22,
  osType: 'CentOS',
  authType: 1,
  credential: '',
  groupName: '',
  remark: ''
})

const rules: FormRules = {
  hostname: [{ required: true, message: '请输入主机名', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入 IP 地址', trigger: 'blur' }],
  sshPort: [{ required: true, message: '请输入 SSH 端口', trigger: 'blur' }],
  authType: [{ required: true, message: '请选择认证方式', trigger: 'change' }],
  credential: [{ required: true, message: '请输入凭据', trigger: 'blur' }]
}

function hostStatusTag(status: number): 'success' | 'info' | 'warning' | 'danger' {
  return status === 1 ? 'success' : status === 2 ? 'warning' : status === 3 ? 'info' : 'danger'
}

async function load() {
  loading.value = true
  try {
    const res = await pageHosts({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
      groupName: query.groupName || undefined
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
  query.groupName = ''
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
  form.hostname = ''
  form.ipAddress = ''
  form.sshPort = 22
  form.osType = 'CentOS'
  form.authType = 1
  form.credential = ''
  form.groupName = ''
  form.remark = ''
  editVisible.value = true
}

function openEdit(row: CmdbHost) {
  editingId.value = row.id
  form.hostname = row.hostname
  form.ipAddress = row.ipAddress
  form.sshPort = row.sshPort
  form.osType = row.osType || 'CentOS'
  form.authType = row.authType
  form.credential = ''
  form.groupName = row.groupName || ''
  form.remark = row.remark || ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateHost(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createHost({ ...form })
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleTest(row: CmdbHost) {
  testingId.value = row.id
  try {
    const res = await testHost(row.id)
    ElMessage.success(`连接测试：${res.result}`)
    load()
  } finally {
    testingId.value = undefined
  }
}

async function handleDelete(row: CmdbHost) {
  await ElMessageBox.confirm(`确定删除主机「${row.hostname}（${row.ipAddress}）」吗？`, '提示', { type: 'warning' })
  await deleteHost(row.id)
  ElMessage.success('删除成功')
  load()
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

.credential-area :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>