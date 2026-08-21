<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>报表导出</span>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="export-form">
        <el-form-item label="报表类型" required>
          <el-radio-group v-model="form.reportType">
            <el-radio v-for="t in REPORT_TYPES" :key="t.value" :value="t.value" class="type-radio">
              <div>
                <div>{{ t.label }}</div>
                <div class="type-desc">{{ t.desc }}</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="导出格式" required>
          <el-radio-group v-model="form.format">
            <el-radio value="EXCEL">Excel</el-radio>
            <el-radio value="PDF">PDF</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
            v-model="form.range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 300px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">导出并下载</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="lastResult"
        class="result-alert"
        type="success"
        :closable="false"
        show-icon
        :title="`导出成功：${lastResult} 已生成，正在下载...`"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { REPORT_TYPES, exportReport } from '@/api/report'

const form = reactive<{
  reportType: string
  format: string
  range?: [string, string]
}>({
  reportType: 'ticket_stats',
  format: 'EXCEL',
  range: undefined
})

const exporting = ref(false)
const lastResult = ref<string>()

async function load() {
  lastResult.value = undefined
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await exportReport({
      reportType: form.reportType,
      format: form.format,
      startTime: form.range?.[0],
      endTime: form.range?.[1]
    })
    const typeLabel = REPORT_TYPES.find((t) => t.value === form.reportType)?.label || form.reportType
    lastResult.value = `${typeLabel}（${form.format}）`
    downloadFile(`${window.location.origin}${res.downloadUrl}`)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/** 携带 token 下载报表文件 */
function downloadFile(url: string) {
  const token = localStorage.getItem('opsflow_token')
  const xhr = new XMLHttpRequest()
  xhr.open('GET', url, true)
  if (token) {
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  }
  xhr.responseType = 'blob'
  xhr.onload = () => {
    if (xhr.status === 200) {
      const blob = xhr.response
      const fileName = url.split('/').pop() || 'report'
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = fileName
      a.click()
      URL.revokeObjectURL(a.href)
    } else {
      ElMessage.error('下载失败')
    }
  }
  xhr.onerror = () => ElMessage.error('网络异常，下载失败')
  xhr.send()
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.export-form {
  max-width: 640px;
}

.type-radio {
  height: auto;
  margin-bottom: 8px;
  align-items: flex-start;
}

.type-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.result-alert {
  margin-top: 8px;
}
</style>