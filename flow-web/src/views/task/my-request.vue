<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">我的申请</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'processName'">
            <a @click="openDetail(record)" style="font-weight: 500">{{ record.processName || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status] || 'default'">
              {{ statusLabelMap[record.status] || '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatDuration(record.duration) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a v-if="record.status === 1" @click="handleTerminate(record)" style="color: #ff4d4f">终止</a>
            <span v-else style="color: #999">-</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { getMyProcessInstances, terminateProcessInstance } from '../../api/process'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])

const statusLabelMap = { 0: '草稿', 1: '进行中', 2: '已完成', 3: '已终止', 4: '已暂停' }
const statusColorMap = { 0: 'default', 1: 'processing', 2: 'success', 3: 'error', 4: 'warning' }

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '流程名称', dataIndex: 'processName', key: 'processName', width: 160 },
  { title: '流程Key', dataIndex: 'processKey', key: 'processKey', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '当前节点', dataIndex: 'currentNodeId', key: 'currentNodeId', width: 140 },
  { title: '发起时间', dataIndex: 'startTime', key: 'startTime', width: 120, customRender: renderDate },
  { title: '耗时', key: 'duration', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

function formatDuration(seconds) {
  if (!seconds && seconds !== 0) return '-'
  if (seconds < 60) return seconds + '秒'
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (mins < 60) return `${mins}分${secs > 0 ? secs + '秒' : ''}`
  const hours = Math.floor(mins / 60)
  const remainMins = mins % 60
  if (hours < 24) return `${hours}时${remainMins > 0 ? remainMins + '分' : ''}`
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  return `${days}天${remainHours > 0 ? remainHours + '时' : ''}`
}

function openDetail(record) {
  router.push({ path: '/task/handle', query: { instanceId: record.id } })
}

function handleTerminate(record) {
  Modal.confirm({
    title: '确认终止',
    content: `确定要终止流程「${record.processName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await terminateProcessInstance(record.id)
        message.success('终止成功')
        loadData()
      } catch {
        // handled by interceptor
      }
    }
  })
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getMyProcessInstances(userId)
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    dataList.value = list
    pagination.total = list.length
  } catch {
    // ignore
  }
  loading.value = false
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>
