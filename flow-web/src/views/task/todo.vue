<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">待办任务</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <!-- 条件查询 -->
      <div class="search-bar">
        <a-input v-model:value="search.processName" placeholder="流程名称" allow-clear style="width: 160px" />
        <a-input v-model:value="search.nodeName" placeholder="节点名称" allow-clear style="width: 140px" />
        <a-range-picker v-model:value="search.dateRange" :placeholder="['创建开始', '创建结束']" style="width: 240px" />
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
            <a @click="handleProcess(record)" style="font-family: monospace">{{ record.instanceNo || '-' }}</a>
          </template>
          <template v-else-if="column.key === 'processName'">
            <span style="font-weight: 500">{{ record.processName || record.processKey || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 2 ? 'green' : record.status === 1 ? 'blue' : 'default'">
              {{ record.statusDesc || (record.status === 2 ? '已完成' : record.status === 1 ? '处理中' : '待处理') }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <template v-if="record.assignee && record.status !== 2">
              <span class="action-link" @click="handleProcess(record)">办理</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showTransferModal(record)">转办</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showDelegateModal(record)">委派</span>
            </template>
            <span v-else-if="!record.assignee" class="action-link" @click="handleClaim(record)">签收</span>
            <span v-else class="text-muted">已办结</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办" @ok="handleTransfer">
      <a-form layout="vertical">
        <a-form-item label="转办目标用户ID" required>
          <a-input v-model:value="transferUserId" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 委派弹窗 -->
    <a-modal v-model:open="delegateVisible" title="委派" @ok="handleDelegate">
      <a-form layout="vertical">
        <a-form-item label="委派目标用户ID" required>
          <a-input v-model:value="delegateUserId" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getTodoTasks, claimTask, transferTask, delegateTask } from '../../api/task'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])
const currentTask = ref(null)

const search = reactive({
  processName: '',
  nodeName: '',
  dateRange: null
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程编号', key: 'instanceNo', width: 220 },
  { title: '流程名称', key: 'processName', width: 160 },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate },
  { title: '操作', key: 'action', width: 200 }
]

// 前端过滤
const filteredData = computed(() => {
  let list = dataList.value
  if (search.processName) {
    const kw = search.processName.toLowerCase()
    list = list.filter(r => ((r.processName || r.processKey) || '').toLowerCase().includes(kw))
  }
  if (search.nodeName) {
    const kw = search.nodeName.toLowerCase()
    list = list.filter(r => (r.nodeName || '').toLowerCase().includes(kw))
  }
  if (search.dateRange && search.dateRange.length === 2) {
    const start = search.dateRange[0].startOf('day')
    const end = search.dateRange[1].endOf('day')
    list = list.filter(r => {
      if (!r.createTime) return false
      const t = dayjs(r.createTime)
      return t.isAfter(start) && t.isBefore(end)
    })
  }
  pagination.total = list.length
  return list
})

// 表格固定高度（与已办页面保持一致）
const tableScrollY = ref(400)
function calcTableHeight() {
  nextTick(() => {
    const tableWrap = document.querySelector('.card-wrap .ant-table-wrapper')
    if (tableWrap) {
      const rect = tableWrap.getBoundingClientRect()
      // 56 = 分页高度, 16 = 底部留白
      tableScrollY.value = Math.max(window.innerHeight - rect.top - 56 - 16, 200)
    }
  })
}

// 转办
const transferVisible = ref(false)
const transferUserId = ref('')
// 委派
const delegateVisible = ref(false)
const delegateUserId = ref('')

function showTransferModal(record) {
  currentTask.value = record
  transferUserId.value = ''
  transferVisible.value = true
}
function showDelegateModal(record) {
  currentTask.value = record
  delegateUserId.value = ''
  delegateVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getTodoTasks({ userId, page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
  } catch {}
  loading.value = false
}

/** 跳转到任务办理页面 */
function handleProcess(record) {
  router.push(`/task/handle?id=${record.id}`)
}

async function handleClaim(record) {
  try {
    await claimTask(record.id, { userId: record.assignee || 'current-user' })
    message.success('签收成功')
    loadData()
  } catch {}
}

async function handleTransfer() {
  if (!transferUserId.value.trim()) { message.warning('请输入转办目标用户ID'); return }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: transferUserId.value.trim()
    })
    message.success('转办成功')
    transferVisible.value = false
    loadData()
  } catch {}
}

async function handleDelegate() {
  if (!delegateUserId.value.trim()) { message.warning('请输入委派目标用户ID'); return }
  try {
    await delegateTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      delegateUserId: delegateUserId.value.trim()
    })
    message.success('委派成功')
    delegateVisible.value = false
    loadData()
  } catch {}
}

function handleSearch() {
  pagination.current = 1
}

function handleReset() {
  search.processName = ''
  search.nodeName = ''
  search.dateRange = null
  pagination.current = 1
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
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
.text-muted { color: #999; }
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
