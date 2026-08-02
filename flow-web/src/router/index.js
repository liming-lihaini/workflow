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
        meta: { title: '工作台', permKey: 'dashboard' }
      },
      {
        path: 'process/definition',
        name: 'ProcessDefinition',
        component: () => import('../views/process/definition/index.vue'),
        meta: { title: '流程定义', permKey: 'process:definition' }
      },
      {
        path: 'process/designer',
        name: 'ProcessDesigner',
        component: () => import('../views/process/designer.vue'),
        meta: { title: '流程设计器', permKey: 'process:designer' }
      },
      {
        path: 'process/config',
        name: 'ProcessConfig',
        component: () => import('../views/process/config/index.vue'),
        meta: { title: '流程配置', permKey: 'process:config' }
      },
      {
        path: 'process/instance',
        name: 'ProcessInstance',
        component: () => import('../views/process/instance/index.vue'),
        meta: { title: '流程实例', permKey: 'process:instance' }
      },
      {
        path: 'form/definition',
        name: 'FormDefinition',
        component: () => import('../views/form/definition/index.vue'),
        meta: { title: '表单定义', permKey: 'form:definition' }
      },
      {
        path: 'form/design',
        name: 'FormDesign',
        component: () => import('../views/form/design/index.vue'),
        meta: { title: '表单设计器', permKey: 'form:design' }
      },
      {
        path: 'data-model',
        name: 'DataModel',
        component: () => import('../views/data-model/index.vue'),
        meta: { title: '数据模型', permKey: 'data-model' }
      },
      {
        path: 'model-data/:modelKey',
        name: 'ModelDataManage',
        component: () => import('../views/model-data/manage.vue'),
        meta: { title: '业务数据', dynamicModelPerm: true }
      },
      {
        path: 'task/start',
        name: 'TaskStart',
        component: () => import('../views/task/start.vue'),
        meta: { title: '发起流程', permKey: 'task:start' }
      },
      {
        path: 'task/start-detail',
        name: 'TaskStartDetail',
        component: () => import('../views/task/start-detail.vue'),
        meta: { title: '发起流程', permKey: 'task:start' }
      },
      {
        path: 'task/todo',
        name: 'TaskTodo',
        component: () => import('../views/task/todo.vue'),
        meta: { title: '待办任务', permKey: 'task:todo' }
      },
      {
        path: 'task/handle',
        name: 'TaskHandle',
        component: () => import('../views/task/handle.vue'),
        meta: { title: '任务办理', permKey: 'task:todo' }
      },
      {
        path: 'task/my-request',
        name: 'MyRequest',
        component: () => import('../views/task/my-request.vue'),
        meta: { title: '我的申请', permKey: 'task:my-request' }
      },
      {
        path: 'task/done',
        name: 'TaskDone',
        component: () => import('../views/task/done.vue'),
        meta: { title: '已办任务', permKey: 'task:done' }
      },
      {
        path: 'task/delegation',
        name: 'TaskDelegation',
        component: () => import('../views/task/delegation.vue'),
        meta: { title: '委托与代理', permKey: 'task:delegation' }
      },
      {
        path: 'task/proxy',
        name: 'TaskProxy',
        component: () => import('../views/task/proxy.vue'),
        meta: { title: '代理记录', permKey: 'task:proxy' }
      },
      {
        path: 'system/dept',
        name: 'DeptManage',
        component: () => import('../views/system/dept.vue'),
        meta: { title: '部门管理', permKey: 'system:dept' }
      },
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('../views/system/user.vue'),
        meta: { title: '用户管理', permKey: 'system:user' }
      },
      {
        path: 'system/user-detail',
        name: 'UserDetail',
        component: () => import('../views/system/user-detail.vue'),
        meta: { title: '用户详情', permKey: 'system:user' }
      },
      {
        path: 'system/dept-detail',
        name: 'DeptDetail',
        component: () => import('../views/system/dept-detail.vue'),
        meta: { title: '部门详情', permKey: 'system:dept' }
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('../views/system/role.vue'),
        meta: { title: '角色管理', permKey: 'system:role' }
      },
      {
        path: 'system/log',
        name: 'LogManage',
        component: () => import('../views/system/log.vue'),
        meta: { title: '日志管理', permKey: 'system:log' }
      },
      {
        path: 'system/dict',
        name: 'DictManage',
        component: () => import('../views/system/dict.vue'),
        meta: { title: '数据字典', permKey: 'system:dict' }
      },
      {
        path: 'system/admin',
        name: 'AdminManage',
        component: () => import('../views/system/admin.vue'),
        meta: { title: '三员管理', permKey: 'system:admin' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('../views/monitor/index.vue'),
        meta: { title: '流程监控', permKey: 'monitor' }
      },
      // ===== 环境监测 LIMS（ISSUE-022）=====
      {
        path: 'ems/base/customer',
        name: 'EmsCustomer',
        component: () => import('../views/ems/base/Customer.vue'),
        meta: { title: '客户管理', permKey: 'ems:customer' }
      },
      {
        path: 'ems/base/entrust',
        name: 'EmsEntrust',
        component: () => import('../views/ems/base/Entrust.vue'),
        meta: { title: '委托管理', permKey: 'ems:entrust' }
      },
      {
        path: 'ems/base/vehicle',
        name: 'EmsVehicle',
        component: () => import('../views/ems/base/Vehicle.vue'),
        meta: { title: '车辆台账', permKey: 'ems:vehicle' }
      },
      {
        path: 'ems/base/asset-manage',
        name: 'EmsAssetManage',
        component: () => import('../views/ems/base/AssetManage.vue'),
        meta: { title: '物资管理', permKey: 'ems:instrument' }
      },
      {
        path: 'ems/base/dispatch',
        name: 'EmsDispatch',
        component: () => import('../views/ems/base/DispatchBoard.vue'),
        meta: { title: '采样调度', permKey: 'ems:dispatch' }
      },
      // ===== 采样与样品管理（ISSUE-024）=====
      {
        path: 'ems/base/receive',
        name: 'EmsReceive',
        component: () => import('../views/ems/base/ReceiveWorkbench.vue'),
        meta: { title: '收样工作台', permKey: 'ems:sample' }
      },
      {
        path: 'ems/base/sample',
        name: 'EmsSample',
        component: () => import('../views/ems/base/Sample.vue'),
        meta: { title: '样品管理', permKey: 'ems:sample' }
      },
      {
        path: 'ems/base/retain',
        name: 'EmsRetain',
        component: () => import('../views/ems/base/Retain.vue'),
        meta: { title: '留样库管理', permKey: 'ems:sample' }
      },
      // ===== 检测数据录入与复核（ISSUE-025）=====
      {
        path: 'ems/base/data-entry',
        name: 'EmsDataEntry',
        component: () => import('../views/ems/base/DataEntry.vue'),
        meta: { title: '检测数据录入', permKey: 'ems:detection' }
      },
      {
        path: 'ems/base/review',
        name: 'EmsReview',
        component: () => import('../views/ems/base/Review.vue'),
        meta: { title: '检测复核', permKey: 'ems:detection' }
      },
      // ===== 质量控制（ISSUE-026）=====
      {
        path: 'ems/quality/hazardous',
        name: 'EmsHazardous',
        component: () => import('../views/ems/quality/Hazardous.vue'),
        meta: { title: '危化品管理', permKey: 'ems:quality' }
      },
      {
        path: 'ems/quality/plan',
        name: 'EmsQcPlan',
        component: () => import('../views/ems/quality/QcPlan.vue'),
        meta: { title: '质控计划', permKey: 'ems:quality' }
      },
      {
        path: 'ems/quality/proficiency',
        name: 'EmsProficiency',
        component: () => import('../views/ems/quality/Proficiency.vue'),
        meta: { title: '能力验证与比对', permKey: 'ems:quality' }
      },
      // ===== 监测数据驾驶舱与统计（ISSUE-028）=====
      {
        path: 'ems/dashboard',
        name: 'EmsDashboard',
        component: () => import('../views/ems/dashboard/Dashboard.vue'),
        meta: { title: '监测驾驶舱', permKey: 'ems:dashboard' }
      },
      // ===== 基础设施底座：规则引擎配置（ISSUE-029）=====
      {
        path: 'ems/base/rule-admin',
        name: 'EmsRuleAdmin',
        component: () => import('../views/ems/base/RuleAdmin.vue'),
        meta: { title: '规则引擎配置', permKey: 'ems:base' }
      },
      // ===== 报告生成与审核（ISSUE-027）=====
      {
        path: 'ems/report/generate',
        name: 'EmsReportGenerate',
        component: () => import('../views/ems/report/ReportGenerate.vue'),
        meta: { title: '报告生成', permKey: 'ems:report' }
      },
      {
        path: 'ems/report/review',
        name: 'EmsReportReview',
        component: () => import('../views/ems/report/ReportReview.vue'),
        meta: { title: '报告审核', permKey: 'ems:report' }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（含权限检查）
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  // 自愈：老会话 localStorage 缺少 userId/realName（后加字段），进入页面前补齐
  if (token && !localStorage.getItem('userId')) {
    try {
      const { useUserStore } = await import('../stores/user')
      await useUserStore().fetchUserInfo()
    } catch { /* 忽略，正常流程继续 */ }
  }
  if (to.meta.requiresAuth === false) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!token) {
      next('/login')
    } else {
      // 权限检查（工作台作为兜底页，跳过权限校验避免无限重定向）
      let permKey = to.meta.permKey
      // 模型数据页按 modelKey 动态计算权限 Key
      if (to.meta.dynamicModelPerm && to.params.modelKey) {
        permKey = `model-data:${to.params.modelKey}`
      }
      if (permKey && to.path !== '/dashboard') {
        const isAdmin = localStorage.getItem('isAdmin') === 'true'
        const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
        if (!isAdmin && !permissions.includes(permKey)) {
          // 无权限，重定向到工作台
          next('/dashboard')
          return
        }
      }
      next()
    }
  }
})

export default router
