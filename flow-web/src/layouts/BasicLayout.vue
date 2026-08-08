<template>
  <a-layout class="basic-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="220"
      :collapsed-width="64"
      class="sider"
    >
      <div class="logo">
        <span v-if="!collapsed">自定义流程引擎</span>
        <span v-else>FE</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
      >
        <template v-for="item in visibleMenuItems" :key="item.key">
          <!-- 无子菜单的菜单项 -->
          <a-menu-item v-if="!item.children" @click="$router.push(item.path)">
            <template #icon><component :is="item.icon" /></template>
            <span>{{ item.title }}</span>
          </a-menu-item>
          <!-- 有子菜单的子菜单 -->
          <a-sub-menu v-else>
            <template #icon><component :is="item.icon" /></template>
            <template #title>{{ item.title }}</template>
            <a-menu-item
              v-for="child in item.children"
              :key="child.key"
              @click="$router.push(child.path)"
            >
              {{ child.title }}
            </a-menu-item>
          </a-sub-menu>
        </template>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部 -->
      <a-layout-header class="header">
        <div class="header-left">
          <component
            :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item>首页</a-breadcrumb-item>
            <a-breadcrumb-item v-if="$route.meta.title">{{ $route.meta.title }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-trigger">
              <UserOutlined />
              <span class="username">{{ userStore.username || '管理员' }}</span>
              <DownOutlined class="down-icon" />
            </span>
            <template #overlay>
              <a-menu @click="handleUserMenu">
                <a-menu-item key="profile">
                  <EditOutlined /> 修改个人信息
                </a-menu-item>
                <a-menu-item key="token">
                  <KeyOutlined /> 个人Token管理
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>

    <!-- 个人信息编辑 -->
    <ProfileModal v-model:open="profileOpen" />
    <!-- 个人Token管理 -->
    <TokenManageModal v-model:open="tokenOpen" />
  </a-layout>
</template>

<script setup>
import { ref, computed, watch, h, markRaw, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getModelMenus } from '../api/modelData'
import ProfileModal from '../components/ProfileModal.vue'
import TokenManageModal from '../components/TokenManageModal.vue'
import {
  DashboardOutlined,
  ApartmentOutlined,
  ScheduleOutlined,
  SettingOutlined,
  MonitorOutlined,
  DatabaseOutlined,
  EnvironmentOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  DownOutlined,
  EditOutlined,
  KeyOutlined,
  LogoutOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const selectedKeys = ref([])
const openKeys = ref([])

// 菜单配置（数据驱动）
const menuConfig = [
  {
    key: 'dashboard',
    title: '工作台',
    path: '/dashboard',
    permKey: 'dashboard',
    icon: markRaw(DashboardOutlined)
  },
  {
    key: 'process',
    title: '流程管理',
    permKey: 'process',
    icon: markRaw(ApartmentOutlined),
    children: [
      { key: 'process-definition', title: '流程定义', path: '/process/definition', permKey: 'process:definition' },
      { key: 'process-instance', title: '流程实例', path: '/process/instance', permKey: 'process:instance' },
      { key: 'form-definition', title: '表单定义', path: '/form/definition', permKey: 'form:definition' },
      { key: 'data-model', title: '数据模型', path: '/data-model', permKey: 'data-model' }
    ]
  },
  {
    key: 'task',
    title: '任务中心',
    permKey: 'task',
    icon: markRaw(ScheduleOutlined),
    children: [
      { key: 'task-start', title: '发起流程', path: '/task/start', permKey: 'task:start' },
      { key: 'task-todo', title: '待办任务', path: '/task/todo', permKey: 'task:todo' },
      { key: 'task-done', title: '已办任务', path: '/task/done', permKey: 'task:done' },
      { key: 'task-my-request', title: '我的申请', path: '/task/my-request', permKey: 'task:my-request' },
      { key: 'task-delegation', title: '委托与代理', path: '/task/delegation', permKey: 'task:delegation' },
      { key: 'task-proxy', title: '代理记录', path: '/task/proxy', permKey: 'task:proxy' }
    ]
  },
  {
    key: 'system',
    title: '后台管理',
    permKey: 'system',
    icon: markRaw(SettingOutlined),
    children: [
      { key: 'system-dept', title: '部门管理', path: '/system/dept', permKey: 'system:dept' },
      { key: 'system-user', title: '用户管理', path: '/system/user', permKey: 'system:user' },
      { key: 'system-role', title: '角色管理', path: '/system/role', permKey: 'system:role' },
      { key: 'system-log', title: '日志管理', path: '/system/log', permKey: 'system:log' },
      { key: 'system-dict', title: '数据字典', path: '/system/dict', permKey: 'system:dict' },
      { key: 'system-admin', title: '三员管理', path: '/system/admin', permKey: 'system:admin' }
    ]
  },
  {
    key: 'ems',
    title: '环境监测LIMS',
    permKey: 'ems',
    icon: markRaw(EnvironmentOutlined),
    children: [
      { key: 'ems-customer', title: '客户管理', path: '/ems/base/customer', permKey: 'ems:customer' },
      { key: 'ems-entrust', title: '委托管理', path: '/ems/base/entrust', permKey: 'ems:entrust' },
      { key: 'ems-dispatch', title: '采样订单', path: '/ems/base/dispatch', permKey: 'ems:dispatch' },
      { key: 'ems-sample', title: '样品管理', path: '/ems/base/sample', permKey: 'ems:sample' },
      { key: 'ems-retain', title: '留样库管理', path: '/ems/base/retain', permKey: 'ems:sample' },
      { key: 'ems-data-entry', title: '检测管理', path: '/ems/base/data-entry', permKey: 'ems:detection' },
      { key: 'ems-qcplan', title: '质控计划', path: '/ems/quality/plan', permKey: 'ems:quality' },
      { key: 'ems-dashboard', title: '监测驾驶舱', path: '/ems/dashboard', permKey: 'ems:dashboard' },
      { key: 'ems-rule-admin', title: '规则引擎配置', path: '/ems/base/rule-admin', permKey: 'ems:base' },
      { key: 'ems-report-generate', title: '报告生成', path: '/ems/report/generate', permKey: 'ems:report' },
      { key: 'ems-report-review', title: '报告审核', path: '/ems/report/review', permKey: 'ems:report' }
    ]
  },
  {
    key: 'monitor',
    title: '流程监控',
    path: '/monitor',
    permKey: 'monitor',
    icon: markRaw(MonitorOutlined)
  },
  {
    key: 'ems-resource',
    title: '资源管理',
    permKey: 'ems:base',
    icon: markRaw(ToolOutlined),
    children: [
      { key: 'ems-vehicle', title: '车辆台账', path: '/ems/base/vehicle', permKey: 'ems:vehicle' },
      { key: 'ems-instrument', title: '物资管理', path: '/ems/base/asset-manage', permKey: 'ems:instrument' },
      { key: 'ems-sample-param-config', title: '采样参数配置', path: '/ems/base/sample-param-config', permKey: 'ems:base' }
    ]
  }
]

// 模型数据动态菜单（数据模型生成表后同步产生）
const modelMenus = ref([])

async function loadModelMenus() {
  try {
    const res = await getModelMenus()
    modelMenus.value = res.data || res || []
  } catch {
    modelMenus.value = []
  }
}

onMounted(() => {
  loadModelMenus()
  // 生成表成功后由数据模型页派发事件，刷新动态菜单
  window.addEventListener('model-menus-changed', loadModelMenus)
})

onBeforeUnmount(() => {
  window.removeEventListener('model-menus-changed', loadModelMenus)
})

// 按权限过滤菜单
const visibleMenuItems = computed(() => {
  const items = menuConfig
    .filter(item => userStore.hasPermission(item.permKey))
    .map(item => {
      if (!item.children) return item
      const visibleChildren = item.children.filter(child => userStore.hasPermission(child.permKey))
      if (visibleChildren.length === 0) return null
      return { ...item, children: visibleChildren }
    })
    .filter(Boolean)

  // 业务数据动态菜单组
  const bizChildren = modelMenus.value
    .filter(menu => userStore.hasPermission(menu.permKey))
    .map(menu => ({
      key: `model-data-${menu.modelKey}`,
      title: menu.permName,
      path: menu.resourcePath,
      permKey: menu.permKey
    }))
  if (bizChildren.length > 0 && userStore.hasPermission('model-data')) {
    items.push({
      key: 'biz-data',
      title: '业务数据',
      permKey: 'model-data',
      icon: markRaw(DatabaseOutlined),
      children: bizChildren
    })
  }
  return items
})

// 根据路由同步菜单选中状态
watch(() => route.path, (path) => {
  const pathMap = {
    '/dashboard': ['dashboard'],
    '/process/definition': ['process-definition'],
    '/process/designer': ['process-definition'],
    '/process/config': ['process-definition'],
    '/process/instance': ['process-instance'],
    '/form/definition': ['form-definition'],
    '/form/design': ['form-definition'],
    '/data-model': ['data-model'],
    '/task/start': ['task-start'],
    '/task/start-detail': ['task-start'],
    '/task/todo': ['task-todo'],
    '/task/handle': ['task-todo'],
    '/task/done': ['task-done'],
    '/task/my-request': ['task-my-request'],
    '/task/delegation': ['task-delegation'],
    '/task/proxy': ['task-proxy'],
    '/system/dept': ['system-dept'],
    '/system/user': ['system-user'],
    '/system/role': ['system-role'],
    '/system/log': ['system-log'],
    '/system/dict': ['system-dict'],
    '/system/admin': ['system-admin'],
    '/ems/base/customer': ['ems-customer'],
    '/ems/base/entrust': ['ems-entrust'],
    '/ems/base/vehicle': ['ems-vehicle'],
    '/ems/base/asset-manage': ['ems-instrument'],
    '/ems/base/dispatch': ['ems-dispatch'],
    '/ems/base/receive': ['ems-receive'],
    '/ems/base/sample': ['ems-sample'],
    '/ems/base/retain': ['ems-retain'],
    '/ems/base/data-entry': ['ems-data-entry'],
    '/ems/quality/plan': ['ems-qcplan'],
    '/ems/dashboard': ['ems-dashboard'],
    '/ems/base/rule-admin': ['ems-rule-admin'],
    '/ems/report/generate': ['ems-report-generate'],
    '/ems/report/review': ['ems-report-review'],
    '/monitor': ['monitor']
  }
  selectedKeys.value = pathMap[path] || []

  // 模型数据管理页：/model-data/{modelKey} 选中对应动态菜单
  if (path.startsWith('/model-data/')) {
    selectedKeys.value = [`model-data-${path.split('/')[2]}`]
  }

  // 自动展开父菜单（累加式，不折叠已展开的其他菜单）
  let parentKey = null
  if (path.startsWith('/model-data/')) parentKey = 'biz-data'
  else if (path.startsWith('/process') || path.startsWith('/form') || path.startsWith('/data-model')) parentKey = 'process'
  else if (path.startsWith('/task')) parentKey = 'task'
  else if (path.startsWith('/system')) parentKey = 'system'
  else if (path.startsWith('/ems')) parentKey = 'ems'

  if (parentKey && !openKeys.value.includes(parentKey)) {
    openKeys.value = [...openKeys.value, parentKey]
  }
}, { immediate: true })

// 子菜单展开/折叠由 v-model:openKeys 自动处理
// 点击一级菜单标题自动切换展开/折叠，点击二级菜单不影响其他已展开菜单

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

// 用户下拉菜单
const profileOpen = ref(false)
const tokenOpen = ref(false)

function handleUserMenu({ key }) {
  if (key === 'profile') profileOpen.value = true
  else if (key === 'token') tokenOpen.value = true
  else if (key === 'logout') handleLogout()
}
</script>

<style scoped>
.basic-layout {
  height: 100vh;
  overflow: hidden;
}

.sider {
  background: #001529;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.05);
}

.header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  line-height: 60px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 1;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  padding: 0 12px;
}

.trigger:hover {
  color: var(--color-primary);
}

.breadcrumb {
  margin-left: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  color: var(--text-content);
  font-size: 14px;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 0 8px;
  color: var(--text-content);
}

.user-trigger:hover {
  color: var(--color-primary);
}

.down-icon {
  font-size: 10px;
}

.content {
  margin: 12px;
  background: var(--bg-page);
  flex: 1;
  overflow: auto;
  min-height: 0;
}
</style>
