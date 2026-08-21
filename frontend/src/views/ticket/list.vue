<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="标题/编号" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.ticketType" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in TYPE_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in PRIORITY_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="(v, k) in STATUS_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="router.push('/tickets/create')">创建工单</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="ticketNo" label="工单编号" width="200" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ row.ticketTypeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small">{{ row.priorityName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small">{{ row.statusName || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="110" />
        <el-table-column prop="assigneeName" label="处理人" width="110">
          <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="SLA" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.slaBreached" type="danger" size="small">已超时</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="detail?.ticketNo" size="560px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-header">
          <h3 class="detail-title">{{ detail.title }}</h3>
          <div class="detail-tags">
            <el-tag size="small">{{ detail.ticketTypeName }}</el-tag>
            <el-tag size="small" :type="priorityTag(detail.priority)">{{ detail.priorityName }}</el-tag>
            <el-tag size="small" :type="STATUS_TAG[detail.status] || 'info'">{{ detail.statusName }}</el-tag>
            <el-tag v-if="detail.slaBreached" size="small" type="danger">SLA已超时</el-tag>
          </div>
          <div class="detail-meta">
            <span>创建人：{{ detail.creator?.nickname || '-' }}</span>
            <span>处理人：{{ detail.assignee?.nickname || '未指派' }}</span>
            <span>创建时间：{{ detail.createTime }}</span>
          </div>
          <div v-if="detail.slaResponseDeadline || detail.slaDeadline" class="detail-sla">
            <span>响应截止：{{ detail.slaResponseDeadline || '-' }}</span>
            <span>解决截止：{{ detail.slaDeadline || '-' }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions">
          <el-button v-if="can('DRAFT', 'submit')" type="primary" size="small" @click="handleSubmit">提交工单</el-button>
          <el-button v-if="can('PENDING_ASSIGN', 'assign')" type="primary" size="small" @click="openAssign">指派</el-button>
          <el-button v-if="can('IN_PROGRESS', 'resolve')" type="success" size="small" @click="openResolve">解决</el-button>
          <el-button v-if="can('RESOLVED', 'close') || can('IN_PROGRESS', 'close')" type="warning" size="small" @click="handleClose">关闭</el-button>
          <el-button v-if="can('RESOLVED', 'reopen') || can('CLOSED', 'reopen')" type="danger" size="small" @click="openReopen">重新打开</el-button>
        </div>

        <!-- 描述 -->
        <el-divider content-position="left">工单描述</el-divider>
        <div class="markdown">{{ detail.description || '暂无描述' }}</div>

        <!-- 评论 -->
        <el-divider content-position="left">评论（{{ detail.comments?.length || 0 }}）</el-divider>
        <div class="comment-list">
          <div v-for="c in detail.comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <b>{{ c.user?.nickname }}</b>
              <span>{{ c.createTime }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
          <el-empty v-if="!detail.comments?.length" description="暂无评论" :image-size="60" />
        </div>
        <el-input v-model="commentText" type="textarea" :rows="3" placeholder="发表评论（支持 Markdown）" />
        <div class="comment-footer">
          <el-button type="primary" size="small" :loading="commenting" @click="handleComment">发表评论</el-button>
        </div>

        <!-- 附件 -->
        <el-divider content-position="left">附件（{{ detail.attachments?.length || 0 }}）</el-divider>
        <div class="attachment-list">
          <div v-for="a in detail.attachments" :key="a.id" class="attachment-item">
            <el-icon><Paperclip /></el-icon>
            <span class="name">{{ a.fileName }}</span>
            <span class="size">{{ formatSize(a.fileSize) }}</span>
            <el-button link type="primary" size="small" @click="download(a)">下载</el-button>
          </div>
          <el-empty v-if="!detail.attachments?.length" description="暂无附件" :image-size="60" />
        </div>
        <el-upload :show-file-list="false" :before-upload="(f: File) => handleUpload(f)">
          <el-button size="small" :icon="Upload">上传附件</el-button>
        </el-upload>

        <!-- 操作日志 -->
        <el-divider content-position="left">操作日志</el-divider>
        <el-timeline>
          <el-timeline-item v-for="l in detail.logs" :key="l.createTime" :timestamp="l.createTime">
            <b>{{ l.operatorName }}</b> {{ actionText(l.action) }}
            <div v-if="l.content" class="log-content">{{ l.content }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>

    <!-- 指派弹窗 -->
    <el-dialog v-model="assignVisible" title="指派工单" width="420px">
      <el-select v-model="assignUserId" filterable placeholder="选择处理人" style="width: 100%">
        <el-option v-for="u in userOptions" :key="u.id" :label="`${u.nickname}（${u.username}）`" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="operating" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 解决/重开 输入弹窗 -->
    <el-dialog v-model="inputVisible" :title="inputTitle" width="480px">
      <el-input v-model="inputText" type="textarea" :rows="4" :placeholder="inputPlaceholder" />
      <template #footer>
        <el-button @click="inputVisible = false">取消</el-button>
        <el-button type="primary" :loading="operating" @click="handleInputConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Paperclip, Upload } from '@element-plus/icons-vue'
import {
  pageTickets,
  getTicket,
  submitTicket,
  assignTicket,
  resolveTicket,
  closeTicket,
  reopenTicket,
  addComment,
  uploadAttachment,
  pageUsers,
  STATUS_TEXT,
  STATUS_TAG,
  type TicketItem,
  type TicketDetail,
  type AttachmentItem
} from '@/api/ticket'

const TYPE_TEXT: Record<number, string> = { 1: '变更', 2: '故障', 3: '请求', 4: '巡检' }
const PRIORITY_TEXT: Record<number, string> = { 0: '紧急', 1: '高', 2: '中', 3: '低' }

const router = useRouter()
const loading = ref(false)
const operating = ref(false)
const list = ref<TicketItem[]>([])
const total = ref(0)

const query = reactive<{
  current: number
  size: number
  keyword: string
  ticketType?: number
  priority?: number
  status?: string
}>({ current: 1, size: 10, keyword: '', ticketType: undefined, priority: undefined, status: undefined })

// 详情抽屉
const drawerVisible = ref(false)
const detail = ref<TicketDetail | null>(null)
const commentText = ref('')
const commenting = ref(false)

// 指派
const assignVisible = ref(false)
const assignUserId = ref<number>()
const userOptions = ref<Array<{ id: number; nickname: string; username: string }>>([])

// 通用输入弹窗（解决/重开）
const inputVisible = ref(false)
const inputTitle = ref('')
const inputPlaceholder = ref('')
const inputText = ref('')
const inputAction = ref<'resolve' | 'reopen'>('resolve')

function priorityTag(p: number) {
  return p === 0 ? 'danger' : p === 1 ? 'warning' : p === 2 ? 'primary' : 'info'
}

function formatSize(size?: number) {
  if (!size && size !== 0) return ''
  return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)}MB` : `${(size / 1024).toFixed(0)}KB`
}

function actionText(action: string) {
  const map: Record<string, string> = {
    CREATE: '创建了工单',
    SUBMIT: '提交了工单',
    ASSIGN: '指派了工单',
    RESOLVE: '解决了工单',
    CLOSE: '关闭了工单',
    REOPEN: '重新打开了工单',
    COMMENT: '发表了评论'
  }
  return map[action] || action
}

function can(_status: string, action: string) {
  if (!detail.value) return false
  const s = detail.value.status
  if (action === 'submit') return s === 'DRAFT'
  if (action === 'assign') return s === 'PENDING_ASSIGN'
  if (action === 'resolve') return s === 'IN_PROGRESS'
  if (action === 'close') return s === 'IN_PROGRESS' || s === 'RESOLVED'
  if (action === 'reopen') return s === 'RESOLVED' || s === 'CLOSED'
  return false
}

async function load() {
  loading.value = true
  try {
    const res = await pageTickets({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      ticketType: query.ticketType,
      priority: query.priority,
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
  query.ticketType = undefined
  query.priority = undefined
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

async function openDetail(id: number) {
  drawerVisible.value = true
  detail.value = await getTicket(id)
  commentText.value = ''
}

async function handleSubmit() {
  if (!detail.value) return
  await submitTicket(detail.value.id)
  ElMessage.success('提交成功')
  detail.value = await getTicket(detail.value.id)
  load()
}

async function openAssign() {
  assignVisible.value = true
  assignUserId.value = undefined
  const res = await pageUsers({ current: 1, size: 100 })
  userOptions.value = res.records
}

async function handleAssign() {
  if (!detail.value || !assignUserId.value) {
    ElMessage.warning('请选择处理人')
    return
  }
  operating.value = true
  try {
    await assignTicket(detail.value.id, assignUserId.value)
    ElMessage.success('指派成功')
    assignVisible.value = false
    detail.value = await getTicket(detail.value.id)
    load()
  } finally {
    operating.value = false
  }
}

function openResolve() {
  inputTitle.value = '解决工单'
  inputPlaceholder.value = '请输入解决方案'
  inputText.value = ''
  inputAction.value = 'resolve'
  inputVisible.value = true
}

function openReopen() {
  inputTitle.value = '重新打开工单'
  inputPlaceholder.value = '请输入重新打开原因'
  inputText.value = ''
  inputAction.value = 'reopen'
  inputVisible.value = true
}

async function handleInputConfirm() {
  if (!detail.value) return
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  operating.value = true
  try {
    if (inputAction.value === 'resolve') {
      await resolveTicket(detail.value.id, inputText.value)
      ElMessage.success('解决成功')
    } else {
      await reopenTicket(detail.value.id, inputText.value)
      ElMessage.success('重新打开成功')
    }
    inputVisible.value = false
    detail.value = await getTicket(detail.value.id)
    load()
  } finally {
    operating.value = false
  }
}

async function handleClose() {
  if (!detail.value) return
  await ElMessageBox.confirm('确定关闭该工单吗？', '提示', { type: 'warning' })
  await closeTicket(detail.value.id)
  ElMessage.success('关闭成功')
  detail.value = await getTicket(detail.value.id)
  load()
}

async function handleComment() {
  if (!detail.value) return
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commenting.value = true
  try {
    await addComment(detail.value.id, commentText.value)
    ElMessage.success('评论成功')
    commentText.value = ''
    detail.value = await getTicket(detail.value.id)
  } finally {
    commenting.value = false
  }
}

async function handleUpload(file: File) {
  if (!detail.value) return false
  try {
    await uploadAttachment(detail.value.id, file)
    ElMessage.success('上传成功')
    detail.value = await getTicket(detail.value.id)
  } catch {
    // 错误已由拦截器提示
  }
  return false
}

function download(a: AttachmentItem) {
  // 通过带认证的文件下载
  const token = localStorage.getItem('opsflow_token')
  const xhr = new XMLHttpRequest()
  xhr.open('GET', `/api/tickets/${detail.value?.id}/attachments/${a.id}/download`, true)
  xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  xhr.responseType = 'blob'
  xhr.onload = () => {
    if (xhr.status === 200) {
      const url = URL.createObjectURL(xhr.response)
      const link = document.createElement('a')
      link.href = url
      link.download = a.fileName
      link.click()
      URL.revokeObjectURL(url)
    } else {
      ElMessage.error('下载失败')
    }
  }
  xhr.send()
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

.detail-header {
  margin-bottom: 8px;
}

.detail-title {
  margin: 0 0 8px;
}

.detail-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #606266;
  font-size: 13px;
}

.detail-sla {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #e6a23c;
  font-size: 13px;
  margin-top: 6px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 4px;
}

.markdown {
  white-space: pre-wrap;
  color: #303133;
  line-height: 1.7;
}

.comment-item {
  border-bottom: 1px dashed #ebeef5;
  padding: 8px 0;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.comment-content {
  color: #303133;
  white-space: pre-wrap;
}

.comment-footer {
  margin-top: 8px;
  text-align: right;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.attachment-item .name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-item .size {
  color: #909399;
  font-size: 12px;
}

.log-content {
  color: #909399;
  font-size: 13px;
}
</style>