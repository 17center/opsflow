<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增部门</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table
        :data="tree"
        v-loading="loading"
        border
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="leader" label="负责人" min-width="100" />
        <el-table-column prop="phone" label="联系电话" min-width="130" />
        <el-table-column prop="email" label="联系邮箱" min-width="160" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" @click="openDialog(undefined, row)">新增下级</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : (form.parentId ? '新增下级部门' : '新增部门')" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="tree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="顶级部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="部门名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.leader" placeholder="负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.email" placeholder="联系邮箱" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  getDeptTree,
  createDept,
  updateDept,
  deleteDept,
  changeDeptStatus,
  type DeptTreeNode,
  type DeptForm
} from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const tree = ref<DeptTreeNode[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<DeptForm & { id?: number }>({
  id: undefined,
  deptName: '',
  parentId: 0,
  leader: '',
  phone: '',
  email: '',
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    tree.value = await getDeptTree()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: DeptTreeNode, parent?: DeptTreeNode) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      deptName: row.deptName,
      parentId: row.parentId,
      leader: row.leader || '',
      phone: row.phone || '',
      email: row.email || '',
      sortOrder: row.sortOrder || 0,
      status: row.status,
      remark: ''
    })
  } else {
    Object.assign(form, {
      id: undefined,
      deptName: '',
      parentId: parent?.id ?? 0,
      leader: '',
      phone: '',
      email: '',
      sortOrder: 0,
      status: 1,
      remark: ''
    })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      deptName: form.deptName,
      parentId: form.parentId || 0,
      leader: form.leader || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      sortOrder: form.sortOrder,
      status: form.status,
      remark: form.remark || undefined
    }
    if (form.id) {
      await updateDept(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createDept(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: DeptTreeNode) {
  await changeDeptStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row: DeptTreeNode) {
  await ElMessageBox.confirm(`确定删除部门「${row.deptName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>