<template>
  <div>
    <el-card shadow="never">
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="标题/内容" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="(v, k) in CATEGORY_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="(v, k) in ARTICLE_STATUS_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="query.tagId" placeholder="全部" clearable filterable style="width: 140px">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建文章</el-button>
        <el-button :icon="DocumentAdd" @click="openFromTicket">工单转知识</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">{{ row.categoryName }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="t in row.tagNames" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
            <span v-if="!row.tagNames || !row.tagNames.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="ARTICLE_STATUS_TAG[row.status]" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" align="center" />
        <el-table-column prop="authorName" label="作者" width="110" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 1" link type="success" @click="handlePublish(row)">发布</el-button>
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

    <!-- 新建/编辑文章弹窗 -->
    <el-dialog
      v-model="editVisible"
      :title="editingId ? '编辑文章' : '新建文章'"
      width="760px"
      destroy-on-close
      top="4vh"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="256" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 200px">
            <el-option v-for="(v, k) in CATEGORY_TEXT" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple filterable placeholder="选择标签" style="width: 100%">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="14"
            placeholder="支持 Markdown 语法"
            class="code-input"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文章详情抽屉 -->
    <el-drawer v-model="detailVisible" title="文章详情" size="620px">
      <template v-if="detail">
        <h2 class="detail-title">{{ detail.title }}</h2>
        <div class="detail-meta">
          <el-tag size="small">{{ detail.categoryName }}</el-tag>
          <template v-if="detail.tagNames">
            <el-tag v-for="t in detail.tagNames" :key="t" size="small" type="info" style="margin-left: 4px">{{ t }}</el-tag>
          </template>
          <span class="meta-item">作者：{{ detail.authorName }}</span>
          <span class="meta-item">浏览：{{ detail.viewCount }}</span>
          <el-tag v-if="detail.relatedTicketId" size="small" type="success">关联工单 #{{ detail.relatedTicketId }}</el-tag>
        </div>
        <pre class="detail-content">{{ detail.content }}</pre>
      </template>
    </el-drawer>

    <!-- 工单转知识弹窗 -->
    <el-dialog v-model="fromTicketVisible" title="工单转知识" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="选择工单">
          <el-select v-model="fromTicketId" filterable placeholder="选择已关闭工单" style="width: 100%">
            <el-option v-for="t in closedTickets" :key="t.id" :value="t.id" :label="`${t.ticketNo} - ${t.title}`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fromTicketVisible = false">取消</el-button>
        <el-button type="primary" :loading="converting" @click="handleFromTicket">生成草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, DocumentAdd } from '@element-plus/icons-vue'
import {
  pageKbArticles,
  getKbArticle,
  createKbArticle,
  updateKbArticle,
  deleteKbArticle,
  changeKbArticleStatus,
  kbArticleFromTicket,
  listKbTags,
  CATEGORY_TEXT,
  ARTICLE_STATUS_TEXT,
  ARTICLE_STATUS_TAG,
  type KbArticle,
  type KbArticleForm,
  type KbTag
} from '@/api/kb'
import { pageClosedTickets, type TicketBasic } from '@/api/ticket'

const loading = ref(false)
const saving = ref(false)
const converting = ref(false)
const list = ref<KbArticle[]>([])
const total = ref(0)
const tags = ref<KbTag[]>([])
const closedTickets = ref<TicketBasic[]>([])

const query = reactive<{ current: number; size: number; keyword: string; category?: number; status?: number; tagId?: number }>({
  current: 1,
  size: 10,
  keyword: '',
  category: undefined,
  status: undefined,
  tagId: undefined
})

const editVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<KbArticleForm>({
  title: '',
  content: '',
  category: 1,
  tagIds: [],
  status: 0,
  remark: ''
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detail = ref<KbArticle>()

const fromTicketVisible = ref(false)
const fromTicketId = ref<number>()

async function load() {
  loading.value = true
  try {
    const res = await pageKbArticles({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      category: query.category,
      status: query.status,
      tagId: query.tagId
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadTags() {
  tags.value = await listKbTags()
}

async function loadClosedTickets() {
  closedTickets.value = (await pageClosedTickets({ current: 1, size: 100 })).records
}

function handleSearch() {
  query.current = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.category = undefined
  query.status = undefined
  query.tagId = undefined
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
  form.title = ''
  form.content = ''
  form.category = 1
  form.tagIds = []
  form.status = 0
  form.remark = ''
  editVisible.value = true
}

function openEdit(row: KbArticle) {
  editingId.value = row.id
  form.title = row.title
  form.content = row.content
  form.category = row.category
  form.tagIds = [...row.tagIds]
  form.status = row.status
  form.remark = row.remark || ''
  editVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await updateKbArticle(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createKbArticle({ ...form })
      ElMessage.success('新建成功')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function handlePublish(row: KbArticle) {
  await changeKbArticleStatus(row.id, 1)
  ElMessage.success('已发布')
  load()
}

async function handleDelete(row: KbArticle) {
  await ElMessageBox.confirm(`确定删除文章「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteKbArticle(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openDetail(row: KbArticle) {
  detail.value = await getKbArticle(row.id)
  load()
  detailVisible.value = true
}

function openFromTicket() {
  fromTicketId.value = undefined
  fromTicketVisible.value = true
}

async function handleFromTicket() {
  if (!fromTicketId.value) {
    ElMessage.warning('请选择工单')
    return
  }
  converting.value = true
  try {
    const article = await kbArticleFromTicket(fromTicketId.value)
    ElMessage.success(`已生成文章草稿「${article.title}」`)
    fromTicketVisible.value = false
    load()
  } finally {
    converting.value = false
  }
}

onMounted(() => {
  load()
  loadTags()
  loadClosedTickets()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.code-input :deep(textarea) {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.detail-title {
  margin: 0 0 12px;
}

.detail-meta {
  margin-bottom: 16px;
  font-size: 13px;
  color: #606266;
}

.meta-item {
  margin-left: 12px;
}

.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.7;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 4px;
  font-family: inherit;
}
</style>