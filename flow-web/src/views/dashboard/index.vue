<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <div class="card-wrap stat-card">
          <a-statistic title="待办任务" :value="stats.todoCount" :value-style="{ color: '#1677ff' }">
            <template #prefix><ClockCircleOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card">
          <a-statistic title="已办任务" :value="stats.doneCount" :value-style="{ color: '#00b42a' }">
            <template #prefix><CheckCircleOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card">
          <a-statistic title="运行中流程" :value="stats.runningCount" :value-style="{ color: '#ff7d00' }">
            <template #prefix><PlayCircleOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card">
          <a-statistic title="流程定义" :value="stats.definitionCount" :value-style="{ color: '#86909c' }">
            <template #prefix><ApartmentOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
    </a-row>

    <!-- 待办快捷列表 + 快捷入口 -->
    <a-row :gutter="16" style="margin-top: 16px;">
      <a-col :span="16">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">待办任务</span>
            <a-button type="link" @click="$router.push('/task/todo')">查看全部</a-button>
          </div>
          <a-table
            :columns="todoColumns"
            :data-source="todoList"
            :loading="loading"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <span class="action-link" @click="$router.push('/task/todo')">处理</span>
              </template>
            </template>
          </a-table>
        </div>
      </a-col>
      <a-col :span="8">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">快捷入口</span>
          </div>
          <div class="quick-grid">
            <div class="quick-item" @click="$router.push('/process/definition')">
              <ApartmentOutlined class="quick-icon" style="color: #1677ff" />
              <span>流程定义</span>
            </div>
            <div class="quick-item" @click="$router.push('/task/todo')">
              <ScheduleOutlined class="quick-icon" style="color: #ff7d00" />
              <span>待办任务</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/user')">
              <UserOutlined class="quick-icon" style="color: #00b42a" />
              <span>用户管理</span>
            </div>
            <div class="quick-item" @click="$router.push('/monitor')">
              <MonitorOutlined class="quick-icon" style="color: #f53f3f" />
              <span>流程监控</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/dict')">
              <BookOutlined class="quick-icon" style="color: #86909c" />
              <span>数据字典</span>
            </div>
            <div class="quick-item" @click="$router.push('/system/log')">
              <FileTextOutlined class="quick-icon" style="color: #0958d9" />
              <span>日志审计</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  ApartmentOutlined,
  ScheduleOutlined,
  UserOutlined,
  MonitorOutlined,
  BookOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import { getTodoTasks } from '../../api/task'
import { getProcessDefinitions } from '../../api/process'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const todoList = ref([])

const stats = reactive({
  todoCount: 0,
  doneCount: 0,
  runningCount: 0,
  definitionCount: 0
})

const todoColumns = [
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 80 }
]

onMounted(async () => {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getTodoTasks({ userId, page: 1, size: 5 })
    const data = res.data || res
    todoList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    stats.todoCount = data.total || todoList.value.length
  } catch {
    // 后端未启动时忽略
  }
  try {
    const res = await getProcessDefinitions({ page: 1, size: 1 })
    const data = res.data || res
    stats.definitionCount = data.total || 0
  } catch {
    // ignore
  }
  loading.value = false
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.quick-item:hover {
  background: var(--bg-hover);
}

.quick-icon {
  font-size: 28px;
}
</style>
