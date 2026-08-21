import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 静态路由
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运维仪表盘', icon: 'Odometer' }
      },
      // 工单中心
      {
        path: 'tickets',
        name: 'TicketList',
        component: () => import('@/views/ticket/list.vue'),
        meta: { title: '工单列表', icon: 'Tickets' }
      },
      {
        path: 'tickets/create',
        name: 'TicketCreate',
        component: () => import('@/views/ticket/create.vue'),
        meta: { title: '创建工单', icon: 'Plus' }
      },
      {
        path: 'tickets/dashboard',
        name: 'TicketDashboard',
        component: () => import('@/views/ticket/dashboard.vue'),
        meta: { title: '工单看板', icon: 'DataAnalysis' }
      },
      // 工作流
      {
        path: 'workflow/todo',
        name: 'WfTodo',
        component: () => import('@/views/workflow/todo.vue'),
        meta: { title: '我的待办', icon: 'Bell' }
      },
      {
        path: 'workflow/definitions',
        name: 'WfDefinitionList',
        component: () => import('@/views/workflow/definition.vue'),
        meta: { title: '流程定义', icon: 'SetUp' }
      },
      {
        path: 'workflow/instances',
        name: 'WfInstanceList',
        component: () => import('@/views/workflow/instance.vue'),
        meta: { title: '流程实例', icon: 'Operation' }
      },
      // 自动化
      {
        path: 'automation/scripts',
        name: 'ScriptList',
        component: () => import('@/views/automation/scripts.vue'),
        meta: { title: '脚本仓库', icon: 'Document' }
      },
      {
        path: 'automation/exec',
        name: 'ExecList',
        component: () => import('@/views/automation/exec.vue'),
        meta: { title: '执行中心', icon: 'VideoPlay' }
      },
      // 资产管理
      {
        path: 'cmdb/hosts',
        name: 'HostList',
        component: () => import('@/views/cmdb/hosts.vue'),
        meta: { title: '主机管理', icon: 'Monitor' }
      },
      {
        path: 'cmdb/services',
        name: 'ServiceList',
        component: () => import('@/views/cmdb/services.vue'),
        meta: { title: '服务管理', icon: 'Cpu' }
      },
      {
        path: 'cmdb/relations',
        name: 'RelationList',
        component: () => import('@/views/cmdb/relations.vue'),
        meta: { title: '关联拓扑', icon: 'Share' }
      },
      // 监控告警
      {
        path: 'alert/rules',
        name: 'AlertRules',
        component: () => import('@/views/alert/rules.vue'),
        meta: { title: '告警规则', icon: 'Warning' }
      },
      {
        path: 'alert/events',
        name: 'AlertEvents',
        component: () => import('@/views/alert/events.vue'),
        meta: { title: '告警事件', icon: 'Bell' }
      },
      {
        path: 'alert/duty',
        name: 'AlertDuty',
        component: () => import('@/views/alert/duty.vue'),
        meta: { title: '值班管理', icon: 'Calendar' }
      },
      // 知识库
      {
        path: 'knowledge/articles',
        name: 'KbArticleList',
        component: () => import('@/views/knowledge/articles.vue'),
        meta: { title: '文章管理', icon: 'Reading' }
      },
      {
        path: 'knowledge/tags',
        name: 'KbTagList',
        component: () => import('@/views/knowledge/tags.vue'),
        meta: { title: '标签管理', icon: 'CollectionTag' }
      },
      {
        path: 'knowledge/qa',
        name: 'KbQa',
        component: () => import('@/views/knowledge/qa.vue'),
        meta: { title: '智能问答', icon: 'MagicStick' }
      },
      // 数据报表
      {
        path: 'report/dashboard',
        name: 'ReportDashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '运维仪表盘', icon: 'DataAnalysis' }
      },
      {
        path: 'report/export',
        name: 'ReportExport',
        component: () => import('@/views/report/export.vue'),
        meta: { title: '报表导出', icon: 'Download' }
      },
      // 系统管理
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/users.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/roles',
        name: 'RoleList',
        component: () => import('@/views/system/roles.vue'),
        meta: { title: '角色管理', icon: 'Avatar' }
      },
      {
        path: 'system/menus',
        name: 'MenuList',
        component: () => import('@/views/system/menus.vue'),
        meta: { title: '菜单管理', icon: 'Menu' }
      },
      {
        path: 'system/depts',
        name: 'DeptList',
        component: () => import('@/views/system/depts.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'system/audit-logs',
        name: 'AuditLogList',
        component: () => import('@/views/system/audit-logs.vue'),
        meta: { title: '审计日志', icon: 'Document' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('opsflow_token')
  if (to.meta.public) {
    next()
  } else if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router