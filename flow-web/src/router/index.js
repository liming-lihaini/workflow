import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../layouts/BasicLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'process/definition',
        name: 'ProcessDefinition',
        component: () => import('../views/process/definition/index.vue'),
        meta: { title: '流程定义' }
      },
      {
        path: 'process/designer',
        name: 'ProcessDesigner',
        component: () => import('../views/process/designer.vue'),
        meta: { title: '流程设计器' }
      },
      {
        path: 'process/config',
        name: 'ProcessConfig',
        component: () => import('../views/process/config/index.vue'),
        meta: { title: '流程配置' }
      },
      {
        path: 'process/instance',
        name: 'ProcessInstance',
        component: () => import('../views/process/instance/index.vue'),
        meta: { title: '流程实例' }
      },
      {
        path: 'form/definition',
        name: 'FormDefinition',
        component: () => import('../views/form/definition/index.vue'),
        meta: { title: '表单定义' }
      },
      {
        path: 'form/design',
        name: 'FormDesign',
        component: () => import('../views/form/design/index.vue'),
        meta: { title: '表单设计器' }
      },
      {
        path: 'data-model',
        name: 'DataModel',
        component: () => import('../views/data-model/index.vue'),
        meta: { title: '数据模型' }
      },
      {
        path: 'task/start',
        name: 'TaskStart',
        component: () => import('../views/task/start.vue'),
        meta: { title: '发起流程' }
      },
      {
        path: 'task/start-detail',
        name: 'TaskStartDetail',
        component: () => import('../views/task/start-detail.vue'),
        meta: { title: '发起流程' }
      },
      {
        path: 'task/todo',
        name: 'TaskTodo',
        component: () => import('../views/task/todo.vue'),
        meta: { title: '待办任务' }
      },
      {
        path: 'task/handle',
        name: 'TaskHandle',
        component: () => import('../views/task/handle.vue'),
        meta: { title: '任务办理' }
      },
      {
        path: 'task/my-request',
        name: 'MyRequest',
        component: () => import('../views/task/my-request.vue'),
        meta: { title: '我的申请' }
      },
      {
        path: 'task/done',
        name: 'TaskDone',
        component: () => import('../views/task/done.vue'),
        meta: { title: '已办任务' }
      },
      {
        path: 'system/dept',
        name: 'DeptManage',
        component: () => import('../views/system/dept.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('../views/system/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('../views/system/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/log',
        name: 'LogManage',
        component: () => import('../views/system/log.vue'),
        meta: { title: '日志管理' }
      },
      {
        path: 'system/dict',
        name: 'DictManage',
        component: () => import('../views/system/dict.vue'),
        meta: { title: '数据字典' }
      },
      {
        path: 'system/admin',
        name: 'AdminManage',
        component: () => import('../views/system/admin.vue'),
        meta: { title: '三员管理' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('../views/monitor/index.vue'),
        meta: { title: '流程监控' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth === false) {
    // 已登录时访问登录页，跳转首页
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    // 未登录跳转登录页
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
