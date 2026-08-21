<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="标签名称" min-width="200" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="新增标签" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="如：Redis / MySQL / 排障手册" maxlength="32" @keyup.enter="handleSave" />
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
import { listKbTags, createKbTag, deleteKbTag, type KbTag } from '@/api/kb'

const loading = ref(false)
const saving = ref(false)
const list = ref<KbTag[]>([])

const editVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ name: string }>({ name: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    list.value = await listKbTags()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.name = ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await createKbTag({ name: form.name })
    ElMessage.success('新增成功')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: KbTag) {
  await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteKbTag(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>