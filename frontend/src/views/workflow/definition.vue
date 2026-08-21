<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="流程名称">
          <el-input v-model="query.name" placeholder="名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="流程标识">
          <el-input v-model="query.key" placeholder="key" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in DEF_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建流程</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="流程名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="key" label="流程标识" width="160" />
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.version || 1 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="DEF_STATUS_TAG[row.status] || 'info'" size="small">{{ row.statusName || DEF_STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="节点数" width="80" align="center">
          <template #default="{ row }">{{ row.nodes?.length || 0 }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status === 1" link type="warning" @click="handleDisable(row)">停用</el-button>
            <el-button link type="info" @click="openNodes(row)">节点</el-button>
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

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑流程' : '新建流程'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="form.name" placeholder="如：变更审批" maxlength="128" />
        </el-form-item>
        <el-form-item label="流程标识" prop="key">
          <el-input v-model="form.key" placeholder="如：change_approval（唯一）" :disabled="!!editingId" maxlength="64" />
        </el-form-item>
        <el-form-item label="流程描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="流程用途说明" />
        </el-form-item>
        <el-divider content-position="left">审批节点（按顺序执行）</el-divider>
        <div class="node-list">
          <div v-for="(n, idx) in form.nodes" :key="n.nodeKey" class="node-item">
            <el-icon class="node-idx"><Rank /></el-icon>
            <el-input v-model="n.nodeName" placeholder="节点名称" style="width: 150px" />
            <el-select v-model="n.assigneeId" filterable placeholder="审批人" style="width: 180px">
              <el-option v-for="u in userOptions" :key="u.id" :label="`${u.nickname}（${u.username}）`" :value="u.id" />
            </el-select>
            <el-select v-model="n.signType" style="width: 90px">
              <el-option v-for="(v, k) in SIGN_TYPE_TEXT" :key="k" :label="v" :value="Number(k)" />
            </el-select>
            <el-button link type="danger" :disabled="form.nodes.length <= 1" @click="form.nodes.splice(idx, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <el-button link type="primary" :icon="Plus" @click="addNode">添加节点</el-button>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 节点查看弹窗 -->
    <el-dialog v-model="nodesVisible" :title="`流程节点：${currentDef?.name || ''}`" width="560px">
      <el-steps direction="vertical" :active="currentDef?.nodes?.length || 0">
        <el-step v-for="(n, idx) in currentDef?.nodes" :key="n.nodeKey + idx">
          <template #title>{{ n.nodeName }}</template>
          <template #description>
            <div>
              <el-tag size="small">{{ NODE_TYPE_TEXT[n.nodeType || 1] }}</el-tag>
              <el-tag size="small" type="info">{{ SIGN_TYPE_TEXT[n.signType || 1] }}</el-tag>
              <span v-if="n.candidateGroup" class="node-group">角色：{{ n.candidateGroup }}</span>
            </div>
          </template>
        </el-step>
      </el-steps>
      <el-empty v-if="!currentDef?.nodes?.length" description="暂无节点" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Delete, Rank } from '@element-plus/icons-vue'
import {
  pageDefinitions,
  createDefinition,
  updateDefinition,
  publishDefinition,
  disableDefinition,
  DEF_STATUS_TEXT,
  DEF_STATUS_TAG,
  NODE_TYPE_TEXT,
  SIGN_TYPE_TEXT,
  type WfDefinition,
  type WfDefinitionForm
} from '@/api/workflow'
import { pageUsers } from '@/api/ticket'

const loading = ref(false)
const saving = ref(false)
const list = ref<WfDefinition[]>([])
const total = ref(0)

const query = reactive<{ current: number; size: number; name: string; key: string; status?: number }>({
  current: 1,
  size: 10,
  name: '',
  key: '',
  status: undefined
})

// 编辑弹窗
const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<WfDefinitionForm>({ name: '', key: '', description: '', nodes: [] })
const userOptions = ref<Array<{ id: number; nickname: string; username: string }>>([])

// 节点查看
const nodesVisible = ref(false)
const currentDef = ref<WfDefinition | null>(null)

const rules: FormRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  key: [{ required: true, message: '请输入流程标识', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageDefinitions({
      current: query.current,
      size: query.size,
      name: query.name || undefined,
      key: query.key || undefined,
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
  query.name = ''
  query.key = ''
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

function addNode() {
  form.nodes.push({ nodeKey: `n${form.nodes.length + 1}`, nodeName: '', nodeType: 1, signType: 1 })
}

async function openCreate() {
  editingId.value = undefined
  form.name = ''
  form.key = ''
  form.description = ''
  form.nodes = []
  addNode()
  editVisible.value = true
  userOptions.value = (await pageUsers({ current: 1, size: 100 })).records
}

async function openEdit(row: WfDefinition) {
  editingId.value = row.id
  form.name = row.name
  form.key = row.key
  form.description = row.description || ''
  form.nodes = (row.nodes || []).map((n) => ({ ...n }))
  editVisible.value = true
  userOptions.value = (await pageUsers({ current: 1, size: 100 })).records
}

async function handleSave() {
  await formRef.value?.validate()
  // 校验节点
  const bad = form.nodes.find((n) => !n.nodeName.trim() || !n.assigneeId)
  if (bad) {
    ElMessage.warning('每个节点需填写节点名称并指定审批人')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateDefinition(editingId.value, { ...form, nodes: form.nodes })
      ElMessage.success('修改成功')
    } else {
      await createDefinition({ ...form, nodes: form.nodes })
      ElMessage.success('创建成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handlePublish(row: WfDefinition) {
  await ElMessageBox.confirm(`确定发布流程「${row.name}」吗？发布后不可修改。`, '提示', { type: 'warning' })
  await publishDefinition(row.id)
  ElMessage.success('发布成功')
  load()
}

async function handleDisable(row: WfDefinition) {
  await ElMessageBox.confirm(`确定停用流程「${row.name}」吗？`, '提示', { type: 'warning' })
  await disableDefinition(row.id)
  ElMessage.success('停用成功')
  load()
}

function openNodes(row: WfDefinition) {
  currentDef.value = row
  nodesVisible.value = true
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

.node-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 8px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-idx {
  color: #909399;
}

.node-group {
  margin-left: 8px;
  color: #606266;
  font-size: 13px;
}
</style>