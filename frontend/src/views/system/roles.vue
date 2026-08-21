<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" placeholder="角色编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增角色</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="roleCode" label="角色编码" min-width="130" />
        <el-table-column label="数据权限" width="110">
          <template #default="{ row }">{{ dataScopeText(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" @click="openAuthDialog(row)">分配权限</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色名称" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 ROLE_OPS" />
        </el-form-item>
        <el-form-item label="数据权限" prop="dataScope">
          <el-select v-model="form.dataScope" style="width: 100%">
            <el-option label="全部数据" :value="1" />
            <el-option label="本部门" :value="2" />
            <el-option label="本部门及以下" :value="3" />
            <el-option label="仅本人" :value="4" />
          </el-select>
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

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="authVisible" :title="`分配权限 - ${authRole.roleName || ''}`" width="480px" destroy-on-close>
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        default-expand-all
      />
      <template #footer>
        <el-button @click="authVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveAuth">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import {
  pageRoles,
  getRoleDetail,
  createRole,
  updateRole,
  deleteRole,
  changeRoleStatus,
  assignRoleMenus,
  getRoleMenuTree,
  type RoleItem,
  type RoleForm,
  type MenuTreeNode
} from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref<RoleItem[]>([])
const total = ref(0)
const menuTree = ref<MenuTreeNode[]>([])

const query = reactive({ current: 1, size: 10, roleName: '', roleCode: '', status: undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleForm & { id?: number }>({ id: undefined, roleName: '', roleCode: '', dataScope: 1, status: 1, remark: '' })

const authVisible = ref(false)
const authRole = reactive<RoleItem>({ id: 0, roleName: '', roleCode: '', status: 1 })
const treeRef = ref<InstanceType<typeof ElTree>>()

const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function dataScopeText(scope?: number) {
  const map: Record<number, string> = { 1: '全部数据', 2: '本部门', 3: '本部门及以下', 4: '仅本人' }
  return scope ? map[scope] || '-' : '-'
}

async function load() {
  loading.value = true
  try {
    const res = await pageRoles({
      current: query.current,
      size: query.size,
      roleName: query.roleName || undefined,
      roleCode: query.roleCode || undefined,
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
  query.roleName = ''
  query.roleCode = ''
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

function openDialog(row?: RoleItem) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      roleName: row.roleName,
      roleCode: row.roleCode,
      dataScope: row.dataScope || 1,
      status: row.status,
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, { id: undefined, roleName: '', roleCode: '', dataScope: 1, status: 1, remark: '' })
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await updateRole(form.id, {
        roleName: form.roleName,
        dataScope: form.dataScope,
        status: form.status,
        remark: form.remark || undefined
      })
      ElMessage.success('修改成功')
    } else {
      await createRole({
        roleName: form.roleName,
        roleCode: form.roleCode,
        dataScope: form.dataScope,
        status: form.status,
        remark: form.remark || undefined
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openAuthDialog(row: RoleItem) {
  Object.assign(authRole, row)
  menuTree.value = await getRoleMenuTree()
  authVisible.value = true
  // 回显已分配的菜单
  nextTick(async () => {
    const detail = await getRoleDetail(row.id)
    const checked = (detail.menuIds || []).map((id) => id as number)
    treeRef.value?.setCheckedKeys(checked)
  })
}

async function handleSaveAuth() {
  const checked = (treeRef.value?.getCheckedKeys(false) || []).map((k) => Number(k))
  const halfChecked = (treeRef.value?.getHalfCheckedKeys() || []).map((k) => Number(k))
  const menuIds = [...checked, ...halfChecked]
  saving.value = true
  try {
    await assignRoleMenus(authRole.id, menuIds)
    ElMessage.success('权限分配成功')
    authVisible.value = false
  } finally {
    saving.value = false
  }
}

async function handleStatus(row: RoleItem) {
  await changeRoleStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteRole(row.id)
  ElMessage.success('删除成功')
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
</style>