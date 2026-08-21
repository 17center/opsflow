<template>
  <el-card shadow="never">
    <template #header>创建工单</template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 720px">
      <el-form-item label="工单标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入工单标题" maxlength="256" show-word-limit />
      </el-form-item>
      <el-form-item label="工单类型" prop="ticketType">
        <el-radio-group v-model="form.ticketType">
          <el-radio-button :value="1">变更</el-radio-button>
          <el-radio-button :value="2">故障</el-radio-button>
          <el-radio-button :value="3">请求</el-radio-button>
          <el-radio-button :value="4">巡检</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-radio-group v-model="form.priority">
          <el-radio-button :value="0">紧急</el-radio-button>
          <el-radio-button :value="1">高</el-radio-button>
          <el-radio-button :value="2">中</el-radio-button>
          <el-radio-button :value="3">低</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="工单描述">
        <el-input v-model="form.description" type="textarea" :rows="6" placeholder="请输入工单描述（支持 Markdown）" />
      </el-form-item>
      <el-alert
        v-if="form.ticketType === 1"
        type="info"
        :closable="false"
        show-icon
        title="变更工单提交后将进入审批/指派流程，请完善变更说明"
        class="form-alert"
      />
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSave">提交</el-button>
        <el-button @click="router.push('/tickets')">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createTicket, type TicketCreateForm } from '@/api/ticket'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<TicketCreateForm>({
  title: '',
  description: '',
  ticketType: 1,
  priority: 1
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }]
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await createTicket({
      title: form.title,
      description: form.description || undefined,
      ticketType: form.ticketType,
      priority: form.priority
    })
    ElMessage.success(`创建成功：${res.ticketNo}`)
    router.push('/tickets')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.form-alert {
  margin-bottom: 18px;
}
</style>