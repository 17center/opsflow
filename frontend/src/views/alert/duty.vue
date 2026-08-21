<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="month"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          :clearable="false"
          style="width: 140px"
          @change="load"
        />
        <div style="flex: 1" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增排班</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="dutyDate" label="值班日期" width="140" align="center" />
        <el-table-column prop="userName" label="值班人" min-width="140" />
        <el-table-column label="班次" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="SHIFT_TYPE_TAG[row.shiftType]" size="small">{{ row.shiftTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="本月暂无排班" :image-size="80" />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editingId ? '编辑排班' : '新增排班'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="值班人" prop="userId">
          <el-select v-model="form.userId" filterable placeholder="选择值班人" style="width: 100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="值班日期" prop="dutyDate">
          <el-date-picker v-model="form.dutyDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="班次" prop="shiftType">
          <el-select v-model="form.shiftType" style="width: 100%">
            <el-option v-for="(v, k) in SHIFT_TYPE_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
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
  listDutyByMonth,
  createDuty,
  updateDuty,
  deleteDuty,
  SHIFT_TYPE_TEXT,
  type AlertDuty,
  type AlertDutyForm
} from '@/api/alert'
import { pageUsers } from '@/api/ticket'

const SHIFT_TYPE_TAG: Record<number, 'success' | 'warning' | 'info'> = {
  1: 'success',
  2: 'warning',
  3: 'info'
}

const loading = ref(false)
const saving = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))
const list = ref<AlertDuty[]>([])
const userOptions = ref<{ id: number; nickname: string; username: string }[]>([])

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<AlertDutyForm>({
  userId: undefined as unknown as number,
  dutyDate: '',
  shiftType: 1
})

const rules: FormRules = {
  userId: [{ required: true, message: '请选择值班人', trigger: 'change' }],
  dutyDate: [{ required: true, message: '请选择值班日期', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次', trigger: 'change' }]
}

async function loadUsers() {
  const res = await pageUsers({ current: 1, size: 100 })
  userOptions.value = res.records
}

async function load() {
  loading.value = true
  try {
    list.value = await listDutyByMonth(month.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = undefined
  form.userId = undefined as unknown as number
  form.dutyDate = month.value + '-01'
  form.shiftType = 1
  editVisible.value = true
}

function openEdit(row: AlertDuty) {
  editingId.value = row.id
  form.userId = row.userId
  form.dutyDate = row.dutyDate
  form.shiftType = row.shiftType
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateDuty(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createDuty({ ...form })
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: AlertDuty) {
  await ElMessageBox.confirm(`确定删除 ${row.dutyDate} ${row.userName} 的排班吗？`, '提示', { type: 'warning' })
  await deleteDuty(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadUsers()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>