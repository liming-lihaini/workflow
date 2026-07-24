<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">我的申请</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <!-- 条件查询 -->
      <div class="search-bar">
        <a-input v-model:value="search.processName" placeholder="流程名称" allow-clear style="width: 160px" />
        <a-input v-model:value="search.processType" placeholder="流程类型" allow-clear style="width: 140px" />
        <a-range-picker v-model:value="search.dateRange" :placeholder="['发起开始', '发起结束']" style="width: 240px" />
        <a-button type="primary" @click="handleSearch">查询</a-button>
        <a-button @click="handleReset">重置</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredData"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ y: tableScrollY }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'instanceNo'">
            <a @click="openDetail(record)" style="font-family: monospace">{{ record.instanceNo || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'processName'">
            <a @click="openDetail(record)" style="font-weight: 500">{{ record.processName || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColorMap[record.status] || 'default'">
              {{ statusLabelMap[record.status] || '未知' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'currentNode'">
            {{ record.currentNodeName || record.currentNodeId || '-' }}
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatDuration(record.duration) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a v-if="record.status === 0 && hasPerm('task:my-request:terminate')" @click="handleTerminate(record)" style="color: #ff4d4f">终止</a>
            <span v-else style="color: #999">-</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getMyProcessInstances, terminateProcessInstance } from '../../api/process'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'
import { usePermission } from '../../composables/usePermission'

const router = useRouter()
const userStore = useUserStore()
const { hasPerm } = usePermission()
const loading = ref(false)
const dataList = ref([])

// 后端: 0-运行中 1-已完成 2-已暂停 3-已终止
const statusLabelMap = { 0: '运行中', 1: '已完成', 2: '已暂停', 3: '已终止' }
const statusColorMap = { 0: 'processing', 1: 'success', 2: 'warning', 3: 'error' }

const search = reactive({
  processName: '',
  processType: '',
  dateRange: null
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: '流程编号', key: 'instanceNo', width: 220 },
  { title: '流程名称', dataIndex: 'processName', key: 'processName', width: 160 },
  { title: '流程Key', dataIndex: 'processKey', key: 'processKey', width: 140 },
  { title: '状态', key: 'status', width: 100 },
  { title: '当前节点', key: 'currentNode', width: 140 },
  { title: '发起时间', dataIndex: 'startTime', key: 'startTime', width: 120, customRender: renderDate },
  { title: '耗时', key: 'duration', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

// 前端过滤（后端暂不支持条件查询时，在前端过滤）
const filteredData = computed(() => {
  let list = dataList.value
  if (search.processName) {
    const kw = search.processName.toLowerCase()
    list = list.filter(r => (r.processName || '').toLowerCase().includes(kw))
  }
  if (search.processType) {
    const kw = search.processType.toLowerCase()
    list = list.filter(r => (r.processType || '').toLowerCase().includes(kw))
  }
  if (search.dateRange && search.dateRange.length === 2) {
    const start = search.dateRange[0].startOf('day')
    const end = search.dateRange[1].endOf('day')
    list = list.filter(r => {
      if (!r.startTime) return false
      const t = dayjs(r.startTime)
      return t.isAfter(start) && t.isBefore(end)
    })
  }
  pagination.total = list.length
  return list
})

// 表格固定高度
const tableScrollY = ref(400)
function calcTableHeight() {
  nextTick(() => {
    const tableWrap = document.querySelector('.card-wrap .ant-table-wrapper')
    if (tableWrap) {
      const rect = tableWrap.getBoundingClientRect()
      tableScrollY.value = Math.max(window.innerHeight - rect.top - 56 - 16, 200)
    }
  })
}

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
      } catch {}
    }
  })
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getMyProcessInstances(userId)
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
  } catch {}
  loading.value = false
}

function handleSearch() {
  pagination.current = 1
}

function handleReset() {
  search.processName = ''
  search.processType = ''
  search.dateRange = null
  pagination.current = 1
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
}

onMounted(() => {
  loadData()
  calcTableHeight()
  window.addEventListener('resize', calcTableHeight)
})
onUnmounted(() => {
  window.removeEventListener('resize', calcTableHeight)
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
