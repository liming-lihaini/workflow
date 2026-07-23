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
        <a-menu-item key="dashboard" @click="$router.push('/dashboard')">
          <template #icon><DashboardOutlined /></template>
          <span>工作台</span>
        </a-menu-item>

        <a-sub-menu key="process">
          <template #icon><ApartmentOutlined /></template>
          <template #title>流程管理</template>
          <a-menu-item key="process-definition" @click="$router.push('/process/definition')">流程定义</a-menu-item>
          <a-menu-item key="process-designer" @click="$router.push('/process/designer')">流程设计器</a-menu-item>
          <a-menu-item key="process-instance" @click="$router.push('/process/instance')">流程实例</a-menu-item>
          <a-menu-item key="form-definition" @click="$router.push('/form/definition')">表单定义</a-menu-item>
          <a-menu-item key="form-design" @click="$router.push('/form/design')">表单设计器</a-menu-item>
          <a-menu-item key="data-model" @click="$router.push('/data-model')">数据模型</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="task">
          <template #icon><ScheduleOutlined /></template>
          <template #title>任务中心</template>
          <a-menu-item key="task-start" @click="$router.push('/task/start')">发起流程</a-menu-item>
          <a-menu-item key="task-todo" @click="$router.push('/task/todo')">待办任务</a-menu-item>
          <a-menu-item key="task-done" @click="$router.push('/task/done')">已办任务</a-menu-item>
        </a-sub-menu>

        <a-sub-menu key="system">
          <template #icon><SettingOutlined /></template>
          <template #title>后台管理</template>
          <a-menu-item key="system-dept" @click="$router.push('/system/dept')">部门管理</a-menu-item>
          <a-menu-item key="system-user" @click="$router.push('/system/user')">用户管理</a-menu-item>
          <a-menu-item key="system-role" @click="$router.push('/system/role')">角色管理</a-menu-item>
          <a-menu-item key="system-log" @click="$router.push('/system/log')">日志管理</a-menu-item>
          <a-menu-item key="system-dict" @click="$router.push('/system/dict')">数据字典</a-menu-item>
          <a-menu-item key="system-admin" @click="$router.push('/system/admin')">三员管理</a-menu-item>
        </a-sub-menu>

        <a-menu-item key="monitor" @click="$router.push('/monitor')">
          <template #icon><MonitorOutlined /></template>
          <span>流程监控</span>
        </a-menu-item>
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
import { ref, watch } from 'vue'
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

// 根据路由同步菜单选中状态
watch(() => route.path, (path) => {
  const pathMap = {
    '/dashboard': ['dashboard'],
    '/process/definition': ['process-definition'],
    '/process/designer': ['process-designer'],
    '/process/instance': ['process-instance'],
    '/form/definition': ['form-definition'],
    '/form/design': ['form-design'],
    '/data-model': ['data-model'],
    '/task/start': ['task-start'],
    '/task/todo': ['task-todo'],
    '/task/done': ['task-done'],
    '/system/dept': ['system-dept'],
    '/system/user': ['system-user'],
    '/system/role': ['system-role'],
    '/system/log': ['system-log'],
    '/system/dict': ['system-dict'],
    '/system/admin': ['system-admin'],
    '/monitor': ['monitor']
  }
  selectedKeys.value = pathMap[path] || []

  // 自动展开父菜单
  if (path.startsWith('/process')) openKeys.value = ['process']
  else if (path.startsWith('/task')) openKeys.value = ['task']
  else if (path.startsWith('/system')) openKeys.value = ['system']
}, { immediate: true })

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.sider {
  background: #001529;
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
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 1;
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
  margin: 16px;
  background: var(--bg-page);
  min-height: 280px;
}
</style>
