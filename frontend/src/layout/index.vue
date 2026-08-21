<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '232px'" class="layout-aside">
      <div class="logo" @click="router.push('/dashboard')">
        <div class="logo-mark">
          <el-icon :size="20"><Connection /></el-icon>
        </div>
        <transition name="fade-slide">
          <span v-show="!collapsed" class="logo-text">
            Ops<span class="logo-accent">Flow</span>
          </span>
        </transition>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="layout-menu"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <template #title>运维仪表盘</template>
          </el-menu-item>

          <el-sub-menu index="ticket">
            <template #title>
              <el-icon><Tickets /></el-icon>
              <span>工单中心</span>
            </template>
            <el-menu-item index="/tickets">工单列表</el-menu-item>
            <el-menu-item index="/tickets/create">创建工单</el-menu-item>
            <el-menu-item index="/tickets/dashboard">工单看板</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="workflow">
            <template #title>
              <el-icon><SetUp /></el-icon>
              <span>工作流</span>
            </template>
            <el-menu-item index="/workflow/todo">我的待办</el-menu-item>
            <el-menu-item index="/workflow/definitions">流程定义</el-menu-item>
            <el-menu-item index="/workflow/instances">流程实例</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="automation">
            <template #title>
              <el-icon><Cpu /></el-icon>
              <span>自动化</span>
            </template>
            <el-menu-item index="/automation/scripts">脚本仓库</el-menu-item>
            <el-menu-item index="/automation/exec">执行中心</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="cmdb">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>资产管理</span>
            </template>
            <el-menu-item index="/cmdb/hosts">主机管理</el-menu-item>
            <el-menu-item index="/cmdb/services">服务管理</el-menu-item>
            <el-menu-item index="/cmdb/relations">关联拓扑</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="alert">
            <template #title>
              <el-icon><Warning /></el-icon>
              <span>监控告警</span>
            </template>
            <el-menu-item index="/alert/rules">告警规则</el-menu-item>
            <el-menu-item index="/alert/events">告警事件</el-menu-item>
            <el-menu-item index="/alert/duty">值班管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="knowledge">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>知识库</span>
            </template>
            <el-menu-item index="/knowledge/articles">文章管理</el-menu-item>
            <el-menu-item index="/knowledge/tags">标签管理</el-menu-item>
            <el-menu-item index="/knowledge/qa">智能问答</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="report">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>数据报表</span>
            </template>
            <el-menu-item index="/report/dashboard">运维仪表盘</el-menu-item>
            <el-menu-item index="/report/export">报表导出</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/users">用户管理</el-menu-item>
            <el-menu-item index="/system/roles">角色管理</el-menu-item>
            <el-menu-item index="/system/menus">菜单管理</el-menu-item>
            <el-menu-item index="/system/depts">部门管理</el-menu-item>
            <el-menu-item index="/system/audit-logs">审计日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>

      <div class="aside-footer" v-if="!collapsed">
        <span class="pulse-dot"></span>
        <span class="aside-status">服务运行正常</span>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="18" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(b, i) in breadcrumbs" :key="i" :to="i < breadcrumbs.length - 1 ? b.path : undefined">
              {{ b.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="个性化设置" placement="bottom">
            <el-icon class="head-icon" :size="19" @click="settingsOpen = true"><Brush /></el-icon>
          </el-tooltip>

          <el-dropdown trigger="click" @command="handleNotifCommand">
            <span class="bell">
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="notif-dropdown">
                <div class="notif-header">
                  <span>通知</span>
                  <el-button link type="primary" size="small" @click.stop="handleReadAll">全部已读</el-button>
                </div>
                <el-dropdown-item v-for="n in notifications" :key="n.id" :command="n.id" class="notif-item">
                  <div class="notif-title" :class="{ unread: n.isRead === 0 }">{{ n.title }}</div>
                  <div class="notif-time">{{ n.createTime }}</div>
                </el-dropdown-item>
                <el-dropdown-item disabled v-if="!notifications.length" class="notif-empty">暂无通知</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">
                {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || '未登录' }}</span>
              <el-icon class="user-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 个性化设置抽屉 -->
    <el-drawer v-model="settingsOpen" title="个性化设置" size="300px" :with-header="true">
      <div class="set-block">
        <div class="set-label">外观模式</div>
        <div class="set-modes">
          <div
            class="mode-card"
            :class="{ active: theme.mode === 'light' }"
            @click="theme.setMode('light')"
          >
            <el-icon :size="20"><Sunny /></el-icon>
            <span>明亮</span>
          </div>
          <div
            class="mode-card"
            :class="{ active: theme.mode === 'dark' }"
            @click="theme.setMode('dark')"
          >
            <el-icon :size="20"><Moon /></el-icon>
            <span>暗黑</span>
          </div>
        </div>
      </div>

      <div class="set-block">
        <div class="set-label">主题强调色</div>
        <div class="brand-grid">
          <div
            v-for="p in BRAND_PRESETS"
            :key="p.key"
            class="brand-swatch"
            :style="{ background: p.color }"
            :title="p.name"
            @click="theme.setBrand(p.key)"
          >
            <el-icon v-if="theme.brandKey === p.key" :size="16" color="#fff"><Check /></el-icon>
          </div>
        </div>
      </div>

      <div class="set-block">
        <div class="set-label">侧边栏风格</div>
        <div class="sidebar-opts">
          <div
            v-for="s in SIDEBAR_OPTS"
            :key="s.value"
            class="sidebar-opt"
            :class="{ active: theme.sidebar === s.value }"
            @click="theme.setSidebar(s.value)"
          >
            <span class="opt-dot" :style="{ background: s.dot }"></span>
            <span>{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div class="set-foot">主题偏好已自动保存到本地</div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useThemeStore, BRAND_PRESETS, type SidebarStyle } from '@/stores/theme'
import { get } from '@/utils/request'
import {
  pageNotifications,
  getUnreadCount,
  markRead,
  markAllRead,
  type NotificationItem
} from '@/api/ticket'
import type { UserInfo } from '@/types/api'

const SIDEBAR_OPTS: { value: SidebarStyle; label: string; dot: string }[] = [
  { value: 'dark', label: '深色', dot: '#0b101c' },
  { value: 'light', label: '浅色', dot: '#f7f9fc' },
  { value: 'gradient', label: '渐变', dot: 'linear-gradient(135deg,#0d1526,#0e1a2e)' }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const theme = useThemeStore()

const collapsed = ref(false)
const settingsOpen = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)

const breadcrumbs = computed(() =>
  route.matched
    .filter((r) => r.meta?.title)
    .map((r) => ({ title: r.meta.title as string, path: r.path }))
)

async function loadNotifications() {
  try {
    const res = await pageNotifications({ current: 1, size: 10 })
    notifications.value = res.records
    const count = await getUnreadCount()
    unreadCount.value = count.count
  } catch {
    // 未登录或失败时静默
  }
}

async function handleNotifCommand(id: number) {
  await markRead(id)
  await loadNotifications()
}

async function handleReadAll() {
  await markAllRead()
  ElMessage.success('已全部标记为已读')
  await loadNotifications()
}

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      const info = await get<UserInfo>('/auth/me')
      userStore.userInfo = info
    } catch {
      // 若获取失败（如 token 失效），request 拦截器会自动跳转登录
    }
  }
  loadNotifications()
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.layout-aside {
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  border-right: 1px solid var(--border-color);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark-2));
  box-shadow: 0 4px 14px color-mix(in srgb, var(--brand) 45%, transparent);
  flex-shrink: 0;
}

.logo-text {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--sidebar-text-active);
  white-space: nowrap;
}
.logo-accent {
  color: var(--brand);
}

.menu-scroll {
  flex: 1;
  overflow: hidden;
}

.layout-menu {
  border-right: none;
  background: transparent;
  padding: 6px 10px;
}

.layout-menu :deep(.el-menu-item),
.layout-menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  border-radius: 10px;
  margin: 3px 0;
  color: var(--sidebar-text);
  transition: background 0.2s ease, color 0.2s ease;
}

.layout-menu :deep(.el-menu-item:hover),
.layout-menu :deep(.el-sub-menu__title:hover) {
  background: var(--sidebar-hover);
  color: var(--sidebar-text-active);
}

.layout-menu :deep(.el-menu-item.is-active) {
  color: var(--sidebar-text-active);
  background: var(--sidebar-active-bg);
  box-shadow: 0 6px 16px color-mix(in srgb, var(--brand) 35%, transparent);
}

[data-sidebar='light'] .layout-menu :deep(.el-menu-item.is-active) {
  box-shadow: none;
}

.layout-menu :deep(.el-menu-item.is-active .el-icon) {
  color: var(--sidebar-text-active);
}

.layout-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.layout-menu :deep(.el-sub-menu .el-menu .el-menu-item) {
  padding-left: 46px !important;
  height: 40px;
  line-height: 40px;
  font-size: 13px;
}

.layout-menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--sidebar-text-active);
}

.aside-footer {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--sidebar-text);
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2fbf71;
  box-shadow: 0 0 0 0 rgba(47, 191, 113, 0.6);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(47, 191, 113, 0.5); }
  70% { box-shadow: 0 0 0 8px rgba(47, 191, 113, 0); }
  100% { box-shadow: 0 0 0 0 rgba(47, 191, 113, 0); }
}

/* ===== 顶栏 ===== */
.layout-header {
  background: var(--bg-header);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  height: 60px;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--text-regular);
  transition: color 0.2s ease;
}
.collapse-btn:hover {
  color: var(--brand);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.head-icon {
  cursor: pointer;
  color: var(--text-regular);
  transition: color 0.2s ease, transform 0.2s ease;
}
.head-icon:hover {
  color: var(--brand);
  transform: rotate(-8deg);
}

.bell {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: var(--text-regular);
}

.notif-dropdown {
  width: 320px;
}
.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 12px;
  font-weight: 600;
}
.notif-item {
  white-space: normal;
  line-height: 1.4;
}
.notif-title {
  font-size: 13px;
}
.notif-title.unread {
  font-weight: 700;
}
.notif-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.notif-empty {
  justify-content: center;
  color: var(--text-secondary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.user-avatar {
  background: linear-gradient(135deg, var(--brand), var(--brand-dark-2));
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  color: var(--text-primary);
}
.user-caret {
  color: var(--text-secondary);
}

/* ===== 主内容区 ===== */
.layout-main {
  background: var(--bg-page);
  padding: 18px;
  overflow-y: auto;
}

/* ===== 设置抽屉 ===== */
.set-block {
  margin-bottom: 26px;
}
.set-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-regular);
  margin-bottom: 12px;
}
.set-modes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.mode-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 0;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  color: var(--text-regular);
  transition: all 0.2s ease;
}
.mode-card:hover {
  border-color: var(--brand);
  color: var(--brand);
}
.mode-card.active {
  border-color: var(--brand);
  background: var(--brand-light-9);
  color: var(--brand);
}
[data-theme='dark'] .mode-card.active {
  background: color-mix(in srgb, var(--brand) 16%, transparent);
}

.brand-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.brand-swatch {
  height: 34px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}
.brand-swatch:hover {
  transform: scale(1.12);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.sidebar-opts {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sidebar-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  color: var(--text-regular);
  transition: all 0.2s ease;
}
.sidebar-opt:hover {
  border-color: var(--brand);
}
.sidebar-opt.active {
  border-color: var(--brand);
  background: var(--brand-light-9);
  color: var(--brand);
}
[data-theme='dark'] .sidebar-opt.active {
  background: color-mix(in srgb, var(--brand) 16%, transparent);
}
.opt-dot {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.set-foot {
  margin-top: 30px;
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
}
</style>