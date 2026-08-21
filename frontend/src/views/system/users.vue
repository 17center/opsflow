<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="query.nickname" placeholder="显示名称" clearable style="width: 160px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作栏 -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增用户</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="显示名称" min-width="120" />
        <el-table-column prop="deptName" label="部门" min-width="120">
          <template #default="{ row }">{{ deptMap[row.deptId] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="!form.id" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="显示名称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-64 位" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择部门"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
import { Plus } from '@element-plus/icons-vue'
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetUserPassword,
  getDeptTree,
  type UserItem,
  type UserForm,
  type DeptTreeNode
} from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const deptOptions = ref<DeptTreeNode[]>([])
const deptMap = reactive<Record<number, string>>({})

const query = reactive({ current: 1, size: 10, username: '', nickname: '', status: undefined })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<UserForm & { id?: number }>({ id: undefined, username: '', password: '', nickname: '', email: '', phone: '', deptId: undefined, status: 1, remark: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 64, message: '长度 6-64 位', trigger: 'blur' }
  ]
}

async function loadDepts() {
  const tree = await getDeptTree()
  deptOptions.value = tree
  const walk = (nodes: DeptTreeNode[]) => {
    nodes.forEach((n) => {
      deptMap[n.id] = n.deptName
      if (n.children?.length) walk(n.children)
    })
  }
  walk(tree)
}

async function load() {
  loading.value = true
  try {
    const res = await pageUsers({
      current: query.current,
      size: query.size,
      username: query.username || undefined,
      nickname: query.nickname || undefined,
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
  query.username = ''
  query.nickname = ''
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

function openDialog(row?: UserItem) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      nickname: row.nickname,
      email: row.email || '',
      phone: row.phone || '',
      deptId: row.deptId,
      status: row.status,
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, { id: undefined, username: '', password: '', nickname: '', email: '', phone: '', deptId: undefined, status: 1, remark: '' })
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
      await updateUser(form.id, {
        nickname: form.nickname,
        email: form.email || undefined,
        phone: form.phone || undefined,
        deptId: form.deptId,
        status: form.status,
        remark: form.remark || undefined
      })
      ElMessage.success('修改成功')
    } else {
      await createUser({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        email: form.email || undefined,
        phone: form.phone || undefined,
        deptId: form.deptId,
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

async function handleResetPwd(row: UserItem) {
  const { value } = await ElMessageBox.prompt('请输入新的初始密码（6-64 位）', `重置用户 ${row.username} 密码`, {
    inputType: 'password',
    inputValidator: (v) => (v && v.length >= 6 ? true : '密码长度需 ≥ 6'),
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await resetUserPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function handleStatus(row: UserItem) {
  await changeUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  loadDepts()
  load()
})
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