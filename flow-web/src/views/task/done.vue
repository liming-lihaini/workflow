<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">已办任务</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <!-- 条件查询 -->
      <div class="search-bar">
        <a-input v-model:value="search.processName" placeholder="流程名称" allow-clear style="width: 160px" />
        <a-input v-model:value="search.processType" placeholder="流程类型" allow-clear style="width: 140px" />
        <a-range-picker v-model:value="search.dateRange" :placeholder="['办理开始', '办理结束']" style="width: 240px" />
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
          <template v-else-if="column.key === 'processType'">
            <a-tag :color="typeColorMap[record.processType] || 'default'">
              {{ typeLabelMap[record.processType] || record.processType || '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatDuration(record.duration) }}
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { getDoneTasks } from '../../api/task'
import { getDictItemsByCode } from '../../api/dict'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])

// 流程类型字典
const typeLabelMap = ref({})
const typeColorMap = { approval: 'blue', process: 'green', callback: 'orange' }

async function loadProcessTypeDict() {
  try {
    const res = await getDictItemsByCode('process_type')
    const items = res.data || res || []
    const arr = Array.isArray(items) ? items : (items.list || [])
    const map = {}
    arr.forEach(item => { map[item.value || item.dictValue] = item.label || item.dictLabel })
    typeLabelMap.value = map
  } catch {
    typeLabelMap.value = { approval: '审批流', process: '业务流程', callback: '回调流程' }
  }
}

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
  { title: '流程类型', key: 'processType', width: 110 },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 140 },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee', width: 100 },
  { title: '处理时间', dataIndex: 'completeTime', key: 'completeTime', width: 120, customRender: renderDate },
  { title: '节点耗时', key: 'duration', width: 120 },
  { title: '办理时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate }
]

// 前端过滤
const filteredData = computed(() => {
  let list = dataList.value
  if (search.processName) {
    const kw = search.processName.toLowerCase()
    list = list.filter(r => (r.processName || '').toLowerCase().includes(kw))
  }
  if (search.processType) {
    const kw = search.processType.toLowerCase()
    list = list.filter(r => {
      const label = typeLabelMap.value[r.processType] || r.processType || ''
      return label.toLowerCase().includes(kw)
    })
  }
  if (search.dateRange && search.dateRange.length === 2) {
    const start = search.dateRange[0].startOf('day')
    const end = search.dateRange[1].endOf('day')
    list = list.filter(r => {
      if (!r.completeTime && !r.createTime) return false
      const t = dayjs(r.completeTime || r.createTime)
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

/** 格式化耗时（秒 -> 可读格式） */
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

/** 点击流程名称，打开任务详情 */
function openDetail(record) {
  router.push({ path: '/task/handle', query: { id: record.id, from: 'done' } })
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getDoneTasks({ userId, page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
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
  loadData()
}

onMounted(() => {
  loadProcessTypeDict()
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
