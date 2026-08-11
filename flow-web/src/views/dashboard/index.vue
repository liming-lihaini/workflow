<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <div class="card-wrap stat-card stat-clickable" @click="$router.push('/task/todo')">
          <a-statistic title="待办任务" :value="stats.todoCount" :value-style="{ color: '#1677ff' }">
            <template #prefix><ClockCircleOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card stat-clickable" @click="$router.push('/task/done')">
          <a-statistic title="我办理的流程任务" :value="stats.myFlowDone" :value-style="{ color: '#00b42a' }">
            <template #prefix><CheckCircleOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card stat-clickable" @click="$router.push('/ems/quality/plan')">
          <a-statistic title="我办理的质控活动" :value="stats.myQcDone" :value-style="{ color: '#ff7d00' }">
            <template #prefix><ExperimentOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="card-wrap stat-card stat-clickable" @click="$router.push('/ems/base/data-entry')">
          <a-statistic title="我办理的样品检测任务" :value="stats.myDetDone" :value-style="{ color: '#722ed1' }">
            <template #prefix><ContainerOutlined /></template>
          </a-statistic>
        </div>
      </a-col>
    </a-row>

    <!-- 待办快捷列表 + 快捷入口 -->
    <a-row :gutter="16" style="margin-top: 16px;">
      <a-col :span="16">
        <div class="card-wrap">
          <a-tabs v-model:activeKey="todoTab" size="small" class="todo-tabs">
            <!-- 流程待办 -->
            <a-tab-pane key="flow" tab="流程">
              <div class="tab-toolbar">
                <span class="tab-subtitle">待处理的流程任务</span>
                <a-button type="link" size="small" @click="$router.push('/task/todo')">查看全部</a-button>
              </div>
              <a-table
                :columns="todoColumns"
                :data-source="todoList"
                :loading="loading"
                :pagination="false"
                :resize-column="true"
                @resizeColumn="handleTodoResize"
                size="small"
                row-key="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'instanceNo'">
                    <a @click="$router.push(`/task/handle?id=${record.id}`)" style="font-family: monospace">
                      {{ record.instanceNo || '-' }}
                    </a>
                  </template>
                  <template v-if="column.key === 'action'">
                    <span class="action-link" @click="$router.push(`/task/handle?id=${record.id}`)">处理</span>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>

            <!-- 质控活动待办：当前用户名下未完成/未取消的监控活动 -->
            <a-tab-pane key="qc" tab="质控">
              <div class="tab-toolbar">
                <span class="tab-subtitle">名下待处理的质控活动</span>
                <a-button type="link" size="small" @click="$router.push('/ems/quality/plan')">前往质控计划</a-button>
              </div>
              <a-table
                :columns="qcTodoColumns"
                :data-source="qcTodoList"
                :loading="qcLoading"
                :pagination="false"
                size="small"
                row-key="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'taskNo'">
                    <a @click="$router.push(`/ems/quality/activity-detail/${record.id}`)" style="font-family: monospace">
                      {{ record.taskNo || '-' }}
                    </a>
                  </template>
                  <template v-if="column.key === 'taskStatus'">
                    <a-tag :color="qcStatusColor(record.taskStatus)">{{ record.taskStatus || '未开始' }}</a-tag>
                  </template>
                  <template v-if="column.key === 'action'">
                    <span class="action-link" @click="$router.push(`/ems/quality/activity-detail/${record.id}`)">去处理</span>
                  </template>
                </template>
                <template #emptyText>
                  <span style="color: var(--text-tertiary, #86909c)">暂无质控活动待办</span>
                </template>
              </a-table>
            </a-tab-pane>

            <!-- 检测待办：未复核的检测任务（待录入/录入中/已提交/已退回） -->
            <a-tab-pane key="detection" tab="检测">
              <div class="tab-toolbar">
                <span class="tab-subtitle">待录入/待复核的检测任务</span>
                <a-button type="link" size="small" @click="$router.push('/ems/base/data-entry')">前往检测录入</a-button>
              </div>
              <a-table
                :columns="detTodoColumns"
                :data-source="detTodoList"
                :loading="detLoading"
                :pagination="false"
                size="small"
                row-key="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'taskNo'">
                    <a @click="$router.push(`/ems/base/detection/detail/${record.id}`)" style="font-family: monospace">
                      {{ record.taskNo || '-' }}
                    </a>
                  </template>
                  <template v-if="column.key === 'status'">
                    <a-tag :color="detStatusColor(record.status)">{{ record.status || '-' }}</a-tag>
                  </template>
                  <template v-if="column.key === 'action'">
                    <span class="action-link" @click="$router.push('/ems/base/data-entry')">去处理</span>
                  </template>
                </template>
                <template #emptyText>
                  <span style="color: var(--text-tertiary, #86909c)">暂无检测待办</span>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
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
  ApartmentOutlined,
  ScheduleOutlined,
  UserOutlined,
  MonitorOutlined,
  BookOutlined,
  FileTextOutlined,
  ExperimentOutlined,
  ContainerOutlined
} from '@ant-design/icons-vue'
import { getTodoTasks, getDoneTasks } from '../../api/task'
import { getQcActivityTodos, getDetectionTasks, getQcActivities } from '../../api/ems'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'
import { useResizableColumns } from '../../composables/useResizableTable'

const userStore = useUserStore()
const loading = ref(false)
const todoList = ref([])
// 待办标签页：流程 / 质控 / 检测
const todoTab = ref('flow')

// ===== 质控活动待办 =====
const qcLoading = ref(false)
const qcTodoList = ref([])
const qcTodoColumns = [
  { title: '任务编号', key: 'taskNo', width: 140 },
  { title: '活动类型', dataIndex: 'qcType', key: 'qcType', width: 90 },
  { title: '检测项目', dataIndex: 'item', key: 'item', ellipsis: true },
  { title: '所属计划', dataIndex: 'planTitle', key: 'planTitle', ellipsis: true, customRender: ({ text }) => text || '-' },
  { title: '任务状态', key: 'taskStatus', width: 90 },
  { title: '操作', key: 'action', width: 80 }
]
const qcStatusColor = (s) => ({ '未开始': 'default', '进行中': 'blue', '已完成': 'green', '已取消': 'red' }[s] || 'default')

// ===== 检测待办：未复核的检测任务 =====
const detLoading = ref(false)
const detTodoList = ref([])
const detTodoColumns = [
  { title: '任务编号', key: 'taskNo', width: 140 },
  { title: '样品名称', dataIndex: 'sampleName', key: 'sampleName', ellipsis: true },
  { title: '监测项目', dataIndex: 'monitorItems', key: 'monitorItems', ellipsis: true, customRender: ({ text }) => text || '-' },
  { title: '录入员', dataIndex: 'entryBy', key: 'entryBy', width: 90, customRender: ({ text }) => text || '-' },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 80 }
]
const detStatusColor = (s) => ({ '待录入': 'default', '录入中': 'blue', '已提交': 'orange', '已退回': 'red', '已复核': 'green' }[s] || 'default')

const stats = reactive({
  todoCount: 0,
  myFlowDone: 0,   // 我办理的流程任务（已办）
  myQcDone: 0,     // 我办理的质控活动（已完成）
  myDetDone: 0     // 我办理的样品检测任务（已复核）
})

const { columns: todoColumns, handleResizeColumn: handleTodoResize } = useResizableColumns([
  { title: '流程编号', dataIndex: 'instanceNo', key: 'instanceNo', ellipsis: true, sorter: true },
  { title: '流程类型', dataIndex: 'processType', key: 'processType', width: 100,
    customRender: ({ text }) => text || '-' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', sorter: true, width: 100 },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 100, customRender: renderDate, sorter: true },
  { title: '操作', key: 'action', width: 80 }
])

onMounted(async () => {
  loading.value = true
  const userId = userStore.username || localStorage.getItem('username') || ''
  try {
    const res = await getTodoTasks({ userId, page: 1, size: 5 })
    const data = res.data || res
    todoList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    stats.todoCount = data.total || todoList.value.length
  } catch {
    // 后端未启动时忽略
  }
  loading.value = false
  // 质控活动待办：活动执行人名下未完成/未取消的活动，数量并入待办统计
  qcLoading.value = true
  try {
    const res = await getQcActivityTodos({ username: userId })
    const data = res.data || res
    qcTodoList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    stats.todoCount += qcTodoList.value.length
  } catch {
    // 后端未启动时忽略
  }
  qcLoading.value = false
  // 检测待办：未复核的检测任务，数量并入待办统计
  detLoading.value = true
  try {
    const res = await getDetectionTasks({ page: 1, size: 50 })
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    detTodoList.value = list.filter((t) => ['待录入', '录入中', '已提交', '已退回'].includes(t.status))
    stats.todoCount += detTodoList.value.length
  } catch {
    // 后端未启动时忽略
  }
  detLoading.value = false
  // 我办理的流程任务：已办任务列表数量
  try {
    const res = await getDoneTasks({ userId })
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    stats.myFlowDone = data.total || list.length
  } catch {
    // 后端未启动时忽略
  }
  // 我办理的质控活动：名下已完成的监控活动数量
  try {
    const res = await getQcActivities({ operatorId: userId, taskStatus: '已完成', page: 1, size: 1 })
    const data = res.data || res
    stats.myQcDone = data.total || 0
  } catch {
    // 后端未启动时忽略
  }
  // 我办理的样品检测任务：本人录入且已复核的检测任务数量
  try {
    const res = await getDetectionTasks({ entryBy: userId, status: '已复核', page: 1, size: 1 })
    const data = res.data || res
    stats.myDetDone = data.total || 0
  } catch {
    // 后端未启动时忽略
  }
  loading.value = false
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.stat-clickable:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.todo-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.tab-subtitle {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
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
