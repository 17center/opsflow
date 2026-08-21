<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="taskName" label="审批节点" width="150" />
        <el-table-column prop="ticketNo" label="工单编号" width="190" />
        <el-table-column prop="ticketTitle" label="工单标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="签批方式" width="90">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ SIGN_TYPE_TEXT[row.signType || 1] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="到达时间" width="165" />
        <el-table-column label="截止时间" width="165">
          <template #default="{ row }">{{ row.dueTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="openApprove(row)">通过</el-button>
            <el-button link type="danger" @click="openReject(row)">驳回</el-button>
            <el-button link type="warning" @click="openDelegate(row)">转交</el-button>
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

    <!-- 审批意见弹窗 -->
    <el-dialog v-model="actionVisible" :title="actionTitle" width="480px">
      <div v-if="actionMode === 'delegate'" class="delegate-user">
        <el-select v-model="targetUserId" filterable placeholder="选择转交对象" style="width: 100%">
          <el-option v-for="u in userOptions" :key="u.id" :label="`${u.nickname}（${u.username}）`" :value="u.id" />
        </el-select>
      </div>
      <el-input v-model="comment" type="textarea" :rows="4" :placeholder="actionMode === 'approve' ? '审批意见（可选）' : '驳回/转交说明'" />
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button type="primary" :loading="operating" @click="handleAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  pageTodo,
  approveTask,
  rejectTask,
  delegateTask,
  SIGN_TYPE_TEXT,
  type WfTodo
} from '@/api/workflow'
import { pageUsers } from '@/api/ticket'

const loading = ref(false)
const operating = ref(false)
const list = ref<WfTodo[]>([])
const total = ref(0)

const query = reactive<{ current: number; size: number }>({ current: 1, size: 10 })

// 操作弹窗
const actionVisible = ref(false)
const actionTitle = ref('')
const actionMode = ref<'approve' | 'reject' | 'delegate'>('approve')
const currentTask = ref<WfTodo | null>(null)
const comment = ref('')
const targetUserId = ref<number>()
const userOptions = ref<Array<{ id: number; nickname: string; username: string }>>([])

async function load() {
  loading.value = true
  try {
    const res = await pageTodo({ current: query.current, size: query.size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
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

function openApprove(row: WfTodo) {
  currentTask.value = row
  actionMode.value = 'approve'
  actionTitle.value = `审批通过：${row.taskName}`
  comment.value = ''
  actionVisible.value = true
}

function openReject(row: WfTodo) {
  currentTask.value = row
  actionMode.value = 'reject'
  actionTitle.value = `驳回：${row.taskName}`
  comment.value = ''
  actionVisible.value = true
}

async function openDelegate(row: WfTodo) {
  currentTask.value = row
  actionMode.value = 'delegate'
  actionTitle.value = `转交：${row.taskName}`
  comment.value = ''
  targetUserId.value = undefined
  userOptions.value = (await pageUsers({ current: 1, size: 100 })).records
  actionVisible.value = true
}

async function handleAction() {
  if (!currentTask.value) return
  if (actionMode.value === 'delegate' && !targetUserId.value) {
    ElMessage.warning('请选择转交对象')
    return
  }
  operating.value = true
  try {
    if (actionMode.value === 'approve') {
      await approveTask(currentTask.value.taskId, comment.value)
      ElMessage.success('已通过')
    } else if (actionMode.value === 'reject') {
      await rejectTask(currentTask.value.taskId, comment.value)
      ElMessage.success('已驳回')
    } else {
      await delegateTask(currentTask.value.taskId, targetUserId.value!, comment.value)
      ElMessage.success('转交成功')
    }
    actionVisible.value = false
    load()
  } finally {
    operating.value = false
  }
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

.delegate-user {
  margin-bottom: 12px;
}
</style>