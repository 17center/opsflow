<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="名称/描述" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.scriptType" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in SCRIPT_TYPE_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in SCRIPT_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建脚本</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="脚本名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="scriptTypeTag(row.scriptType)" size="small">{{ row.scriptTypeName || SCRIPT_TYPE_TEXT[row.scriptType] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.currentVersion || 1 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusName || SCRIPT_STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="90" align="center">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="超时" width="80" align="center">
          <template #default="{ row }">{{ row.timeoutSeconds }}s</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="info" @click="openVersions(row)">版本</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handleEnable(row)">启用</el-button>
            <el-button v-else link type="warning" @click="handleDisable(row)">停用</el-button>
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

    <!-- 创建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑脚本' : '新建脚本'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="脚本名称" prop="name">
          <el-input v-model="form.name" placeholder="如：系统巡检" maxlength="128" />
        </el-form-item>
        <el-form-item label="脚本类型" prop="scriptType">
          <el-radio-group v-model="form.scriptType">
            <el-radio-button v-for="(v, k) in SCRIPT_TYPE_TEXT" :key="k" :value="Number(k)">{{ v }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="脚本分类">
          <el-input v-model="form.category" placeholder="如：巡检/部署/变更" maxlength="64" />
        </el-form-item>
        <el-form-item label="超时(秒)" prop="timeoutSeconds">
          <el-input-number v-model="form.timeoutSeconds" :min="1" :max="3600" />
        </el-form-item>
        <el-form-item label="脚本内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入脚本内容（以 #!/bin/bash 等 shebang 开头）" class="code-area" />
        </el-form-item>
        <el-form-item label="脚本说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="脚本用途" />
        </el-form-item>
        <el-form-item v-if="editingId" label="变更说明" prop="changeLog">
          <el-input v-model="form.changeLog" placeholder="本次变更说明（将生成为新版本）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 版本查看弹窗 -->
    <el-dialog v-model="versionsVisible" :title="`版本历史：${currentScript?.name || ''}`" width="700px">
      <el-table :data="versions" border stripe size="small">
        <el-table-column prop="version" label="版本" width="70" align="center">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="changeLog" label="变更说明" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.changeLog || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column prop="createBy" label="创建人" width="100" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.version !== currentScript?.currentVersion" link type="primary" @click="handleRollback(row)">回滚到此版本</el-button>
            <el-tag v-else size="small" type="success">当前版本</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  pageScripts,
  createScript,
  updateScript,
  deleteScript,
  enableScript,
  disableScript,
  scriptVersions,
  rollbackScript,
  SCRIPT_TYPE_TEXT,
  SCRIPT_STATUS_TEXT,
  type AutoScript,
  type AutoScriptForm,
  type AutoScriptVersion
} from '@/api/automation'

const loading = ref(false)
const saving = ref(false)
const list = ref<AutoScript[]>([])
const total = ref(0)

const query = reactive<{ current: number; size: number; keyword: string; scriptType?: number; status?: number }>({
  current: 1,
  size: 10,
  keyword: '',
  scriptType: undefined,
  status: undefined
})

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<AutoScriptForm>({
  name: '',
  description: '',
  scriptType: 1,
  content: '',
  timeoutSeconds: 300,
  category: '',
  changeLog: ''
})

const versionsVisible = ref(false)
const versions = ref<AutoScriptVersion[]>([])
const currentScript = ref<AutoScript | null>(null)

const rules: FormRules = {
  name: [{ required: true, message: '请输入脚本名称', trigger: 'blur' }],
  scriptType: [{ required: true, message: '请选择脚本类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入脚本内容', trigger: 'blur' }],
  timeoutSeconds: [{ required: true, message: '请输入超时时间', trigger: 'blur' }]
}

function scriptTypeTag(type: number): 'success' | 'primary' | 'warning' {
  return type === 1 ? 'success' : type === 2 ? 'primary' : 'warning'
}

async function load() {
  loading.value = true
  try {
    const res = await pageScripts({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      scriptType: query.scriptType,
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
  query.scriptType = undefined
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
  form.description = ''
  form.scriptType = 1
  form.content = ''
  form.timeoutSeconds = 300
  form.category = ''
  form.changeLog = ''
  editVisible.value = true
}

function openEdit(row: AutoScript) {
  editingId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  form.scriptType = row.scriptType
  form.content = row.content
  form.timeoutSeconds = row.timeoutSeconds
  form.category = row.category || ''
  form.changeLog = ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  if (editingId.value && !form.changeLog?.trim()) {
    ElMessage.warning('编辑脚本需填写变更说明')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateScript(editingId.value, { ...form })
      ElMessage.success('修改成功（已生成新版本）')
    } else {
      await createScript({ ...form })
      ElMessage.success('创建成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleEnable(row: AutoScript) {
  await enableScript(row.id)
  ElMessage.success('已启用')
  load()
}

async function handleDisable(row: AutoScript) {
  await ElMessageBox.confirm(`确定停用脚本「${row.name}」吗？停用后不可执行。`, '提示', { type: 'warning' })
  await disableScript(row.id)
  ElMessage.success('已停用')
  load()
}

async function handleDelete(row: AutoScript) {
  await ElMessageBox.confirm(`确定删除脚本「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteScript(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openVersions(row: AutoScript) {
  currentScript.value = row
  versions.value = await scriptVersions(row.id)
  versionsVisible.value = true
}

async function handleRollback(v: AutoScriptVersion) {
  await ElMessageBox.confirm(`确定回滚脚本到 v${v.version} 吗？`, '提示', { type: 'warning' })
  await rollbackScript(currentScript.value!.id, v.version)
  ElMessage.success('回滚成功')
  versionsVisible.value = false
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

.code-area :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>