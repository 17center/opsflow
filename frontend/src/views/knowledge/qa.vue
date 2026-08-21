<template>
  <div class="qa-page">
    <el-card shadow="never" class="qa-card">
      <div class="qa-header">
        <h2 class="qa-title"><el-icon><MagicStick /></el-icon> 智能问答</h2>
        <p class="qa-sub">基于知识库已发布文章检索生成答案，点击引用来源可查看文章</p>
      </div>

      <!-- 对话区 -->
      <div ref="chatRef" class="chat-body" v-loading="answering">
        <div v-if="!messages.length" class="chat-empty">
          <el-icon :size="48" color="#c0c4cc"><ChatDotRound /></el-icon>
          <p>输入问题，开始智能问答</p>
        </div>
        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role === 'user' ? 'msg-user' : 'msg-assistant'">
          <div class="msg-bubble">
            <div v-if="m.role === 'user'">{{ m.content }}</div>
            <template v-else>
              <pre class="answer-text">{{ m.content }}</pre>
              <div v-if="m.sources && m.sources.length" class="sources">
                <span class="sources-label">引用来源：</span>
                <el-tag
                  v-for="s in m.sources"
                  :key="s.articleId"
                  class="source-tag"
                  type="primary"
                  effect="plain"
                  @click="viewSource(s.articleId)"
                >
                  {{ s.title }}
                </el-tag>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input">
        <el-input
          v-model="question"
          type="textarea"
          :rows="2"
          placeholder="请输入要咨询的运维问题，例如：Redis 内存满了怎么处理？"
          @keydown.enter.exact.prevent="handleAsk"
        />
        <div class="input-actions">
          <el-button :icon="Delete" @click="clearChat">清空</el-button>
          <el-button type="primary" :icon="Promotion" :loading="answering" @click="handleAsk">提问</el-button>
        </div>
      </div>
    </el-card>

    <!-- 来源文章抽屉 -->
    <el-drawer v-model="sourceVisible" title="引用来源" size="560px">
      <template v-if="sourceArticle">
        <h2 class="detail-title">{{ sourceArticle.title }}</h2>
        <div class="detail-meta">
          <el-tag size="small">{{ sourceArticle.categoryName }}</el-tag>
          <span class="meta-item">作者：{{ sourceArticle.authorName }}</span>
        </div>
        <pre class="detail-content">{{ sourceArticle.content }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Delete, Promotion, MagicStick, ChatDotRound } from '@element-plus/icons-vue'
import { askKbQuestion, getKbArticle, type KbArticle } from '@/api/kb'

interface QaMessage {
  role: 'user' | 'assistant'
  content: string
  sources?: { articleId: number; title: string; relevance: number }[]
}

const messages = ref<QaMessage[]>([])
const question = ref('')
const answering = ref(false)
const conversationId = ref<string>()
const chatRef = ref<HTMLElement>()

const sourceVisible = ref(false)
const sourceArticle = ref<KbArticle>()

async function handleAsk() {
  const q = question.value.trim()
  if (!q || answering.value) return
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  answering.value = true
  scrollToBottom()
  try {
    const res = await askKbQuestion(q, conversationId.value)
    conversationId.value = res.conversationId
    messages.value.push({ role: 'assistant', content: res.answer, sources: res.sources })
  } catch {
    // 错误提示已由拦截器处理
  } finally {
    answering.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  conversationId.value = undefined
}

async function viewSource(articleId: number) {
  sourceArticle.value = await getKbArticle(articleId)
  sourceVisible.value = true
}

function scrollToBottom() {
  nextTick(() => {
    if (chatRef.value) {
      chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.qa-page {
  max-width: 900px;
  margin: 0 auto;
}

.qa-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.qa-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
}

.qa-sub {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 4px;
}

.chat-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.msg {
  display: flex;
  margin-bottom: 16px;
}

.msg-user {
  justify-content: flex-end;
}

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}

.msg-user .msg-bubble {
  background: #409eff;
  color: #fff;
}

.msg-assistant .msg-bubble {
  background: #f4f4f5;
  color: #303133;
}

.answer-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.sources {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #dcdfe6;
}

.sources-label {
  font-size: 12px;
  color: #909399;
  margin-right: 6px;
}

.source-tag {
  cursor: pointer;
  margin-right: 6px;
  margin-top: 4px;
}

.chat-input {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
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