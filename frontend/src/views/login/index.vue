<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-grid"></div>
    <div class="bg-glow glow-a"></div>
    <div class="bg-glow glow-b"></div>

    <div class="login-shell">
      <div class="login-hero">
        <div class="hero-logo">
          <div class="logo-mark">
            <el-icon :size="22"><Connection /></el-icon>
          </div>
          <span class="hero-name">Ops<span>Flow</span></span>
        </div>
        <p class="hero-sub">智能工单与自动化运维平台</p>
        <div class="hero-tags">
          <span class="tag">工单</span>
          <span class="tag">工作流</span>
          <span class="tag">自动化</span>
          <span class="tag">告警</span>
          <span class="tag">知识库</span>
        </div>
      </div>

      <el-card class="login-card">
        <div class="login-title">
          <h1>欢迎回来</h1>
          <p>请登录以继续使用 OpsFlow</p>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item class="btn-item">
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-foot">默认账号 admin / admin123</div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { post } from '@/utils/request'
import type { LoginResult } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await post<LoginResult>('/auth/login', {
      username: form.username,
      password: form.password
    })
    userStore.setLoginInfo(res.accessToken, res.refreshToken, res.userInfo)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-page);
}

/* 网格背景 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(var(--grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-line) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 75%);
}

/* 光晕 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
}
.glow-a {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -120px;
  background: color-mix(in srgb, var(--brand) 55%, transparent);
}
.glow-b {
  width: 420px;
  height: 420px;
  bottom: -140px;
  right: -100px;
  background: color-mix(in srgb, var(--brand-dark-2) 60%, transparent);
}

.login-shell {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 56px;
}

.login-hero {
  max-width: 320px;
}
.hero-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}
.logo-mark {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark-2));
  box-shadow: 0 8px 22px color-mix(in srgb, var(--brand) 45%, transparent);
}
.hero-name {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--text-primary);
}
.hero-name span {
  color: var(--brand);
}
.hero-sub {
  margin-top: 18px;
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.6;
}
.hero-tags {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--text-regular);
  border: 1px solid var(--border-color);
  background: var(--bg-card);
}

.login-card {
  width: 380px;
  border-radius: 16px;
  padding: 8px 6px;
}
.login-title {
  text-align: center;
  margin-bottom: 26px;
}
.login-title h1 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.login-title p {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 6px;
}
.login-btn {
  width: 100%;
  height: 44px;
  font-weight: 600;
  letter-spacing: 2px;
}
.btn-item {
  margin-top: 6px;
}
.login-foot {
  margin-top: 6px;
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .login-shell {
    flex-direction: column;
    gap: 28px;
    padding: 0 20px;
  }
  .login-hero {
    text-align: center;
  }
  .hero-logo {
    justify-content: center;
  }
  .hero-tags {
    justify-content: center;
  }
}
</style>