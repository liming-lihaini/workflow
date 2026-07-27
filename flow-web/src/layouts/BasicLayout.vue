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
          <span class="username">{{ userStore.username || '管理员' }}</span>
          <a-button type="link" @click="handleLogout">退出</a-button>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed, watch, h, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  DashboardOutlined,
  ApartmentOutlined,
  ScheduleOutlined,
  SettingOutlined,
  MonitorOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
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
    key: 'monitor',
    title: '流程监控',
    path: '/monitor',
    permKey: 'monitor',
    icon: markRaw(MonitorOutlined)
  }
]

// 按权限过滤菜单
const visibleMenuItems = computed(() => {
  return menuConfig
    .filter(item => userStore.hasPermission(item.permKey))
    .map(item => {
      if (!item.children) return item
      const visibleChildren = item.children.filter(child => userStore.hasPermission(child.permKey))
      if (visibleChildren.length === 0) return null
      return { ...item, children: visibleChildren }
    })
    .filter(Boolean)
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
    '/monitor': ['monitor']
  }
  selectedKeys.value = pathMap[path] || []

  // 自动展开父菜单（累加式，不折叠已展开的其他菜单）
  let parentKey = null
  if (path.startsWith('/process') || path.startsWith('/form') || path.startsWith('/data-model')) parentKey = 'process'
  else if (path.startsWith('/task')) parentKey = 'task'
  else if (path.startsWith('/system')) parentKey = 'system'

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

.content {
  margin: 12px;
  background: var(--bg-page);
  flex: 1;
  overflow: auto;
  min-height: 0;
}
</style>
