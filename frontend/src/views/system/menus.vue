<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增菜单</el-button>
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
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeMap[row.menuType]?.type || 'info'">{{ typeMap[row.menuType]?.label || row.menuType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限标识" min-width="160" />
        <el-table-column prop="path" label="路由路径" min-width="140" />
        <el-table-column prop="icon" label="图标" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" @click="openDialog(undefined, row)">新增子级</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : (form.parentId ? '新增子菜单' : '新增菜单')" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="tree"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="菜单名称" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="/system/users" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 2" label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="system/users" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission">
          <el-input v-model="form.permission" placeholder="如 sys:user:manage" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="图标名，如 User" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
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
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  changeMenuStatus,
  type MenuAdminNode,
  type MenuForm
} from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const tree = ref<MenuAdminNode[]>([])

const typeMap: Record<number, { label: string; type: 'success' | 'primary' | 'warning' }> = {
  1: { label: '目录', type: 'warning' },
  2: { label: '菜单', type: 'success' },
  3: { label: '按钮', type: 'primary' }
}

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuForm & { id?: number }>({
  id: undefined,
  menuName: '',
  parentId: 0,
  menuType: 2,
  path: '',
  component: '',
  permission: '',
  icon: '',
  sortOrder: 0,
  visible: 1,
  status: 1,
  remark: ''
})

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    tree.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: MenuAdminNode, parent?: MenuAdminNode) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      menuName: row.menuName,
      parentId: row.parentId,
      menuType: row.menuType,
      path: row.path || '',
      component: row.component || '',
      permission: row.permission || '',
      icon: row.icon || '',
      sortOrder: row.sortOrder || 0,
      visible: row.visible ?? 1,
      status: row.status,
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, {
      id: undefined,
      menuName: '',
      parentId: parent?.id ?? 0,
      menuType: 2,
      path: '',
      component: '',
      permission: '',
      icon: '',
      sortOrder: 0,
      visible: 1,
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
      menuName: form.menuName,
      parentId: form.parentId || 0,
      menuType: form.menuType,
      path: form.path || undefined,
      component: form.component || undefined,
      permission: form.permission || undefined,
      icon: form.icon || undefined,
      sortOrder: form.sortOrder,
      visible: form.visible,
      status: form.status,
      remark: form.remark || undefined
    }
    if (form.id) {
      await updateMenu(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: MenuAdminNode) {
  await changeMenuStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row: MenuAdminNode) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteMenu(row.id)
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