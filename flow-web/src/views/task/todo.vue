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
        :resize-column="true"
        @resizeColumn="handleResizeColumn"
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
          <template v-else-if="column.key === 'assignee'">
            {{ realName(record.assignee) }}
            <a-tag v-if="record.delegatedBy" color="purple" size="small" style="margin-left: 4px">代理</a-tag>
            <div v-if="record.actualOperatorId" style="font-size: 12px; color: #888">
              实际办理：{{ realName(record.actualOperatorId) }}
            </div>
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
            </template>
            <span v-else-if="!record.assignee" class="action-link" @click="handleClaim(record)">签收</span>
            <span v-else class="text-muted">已办结</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办任务" @ok="handleTransfer" :width="480">
      <a-form layout="vertical">
        <a-form-item label="转办给（必填）" required>
          <a-select
            v-model:value="transferForm.targetUserId"
            show-search
            :filter-option="false"
            placeholder="搜索用户姓名/用户名"
            :options="transferUserOptions"
            @search="onTransferSearch"
            :loading="transferSearchLoading"
            style="width: 100%"
          >
            <template #option="{ value, label, deptName, postName }">
              <span>{{ label }} - {{ deptName || '-' }} / {{ postName || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="转办原因（必填）" required>
          <a-textarea v-model:value="transferForm.reason" :rows="3" placeholder="请输入转办原因" :maxlength="200" show-count />
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
import { getTodoTasks, claimTask, transferTask, searchUsers } from '../../api/task'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'
import { useResizableColumns, sortClientData } from '../../composables/useResizableTable'
import { useUserMap } from '../../composables/useUserMap'

const router = useRouter()
const userStore = useUserStore()
const { buildUserMap, realName } = useUserMap()
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

const { columns, handleResizeColumn } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '流程编号', key: 'instanceNo', dataIndex: 'instanceNo', width: 220, sorter: true },
  { title: '流程类型', key: 'processType', dataIndex: 'processType', width: 100,
    customRender: ({ text }) => text || '-' },
  { title: '流程名称', key: 'processName', dataIndex: 'processName', width: 160, sorter: true },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 120, sorter: true },
  { title: '处理人', key: 'assignee', dataIndex: 'assignee', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate, sorter: true },
  { title: '操作', key: 'action', width: 140 }
])

// 排序状态
const currentSorter = ref(null)

// 前端过滤 + 排序
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
  list = sortClientData(list, currentSorter.value)
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

// 转办
const transferVisible = ref(false)
const transferForm = reactive({ targetUserId: undefined, reason: '' })
const transferUserOptions = ref([])
const transferSearchLoading = ref(false)
let transferSearchTimer = null

// 用户搜索
async function doSearchUsers(keyword) {
  if (!keyword || keyword.trim().length < 1) return []
  try {
    const res = await searchUsers({ keyword: keyword.trim(), size: 20 })
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    return list.map(u => ({
      value: u.username,
      label: u.realName || u.username,
      deptName: u.deptName || '',
      postName: u.postName || ''
    }))
  } catch { return [] }
}

function onTransferSearch(val) {
  clearTimeout(transferSearchTimer)
  transferSearchLoading.value = true
  transferSearchTimer = setTimeout(async () => {
    transferUserOptions.value = await doSearchUsers(val)
    transferSearchLoading.value = false
  }, 300)
}

function showTransferModal(record) {
  currentTask.value = record
  transferForm.targetUserId = undefined
  transferForm.reason = ''
  transferUserOptions.value = []
  transferVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getTodoTasks({ userId, page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
    // 解析处理人 / 实际办理人真实姓名
    buildUserMap()
  } catch {}
  loading.value = false
}

/** 跳转到任务办理页面 */
function handleProcess(record) {
  router.push(`/task/handle?id=${record.id}`)
}

const userId = computed(() => userStore.username || localStorage.getItem('username') || '')

async function handleClaim(record) {
  try {
    await claimTask(record.id, { userId: userId.value })
    message.success('签收成功')
    loadData()
  } catch {}
}

async function handleTransfer() {
  if (!transferForm.targetUserId) { message.warning('请选择转办目标用户'); return }
  if (!transferForm.reason.trim()) { message.warning('请填写转办原因'); return }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: transferForm.targetUserId,
      reason: transferForm.reason.trim()
    })
    message.success('转办成功')
    transferVisible.value = false
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

function handleTableChange(pag, filters, sorter) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  currentSorter.value = sorter && sorter.field ? sorter : null
  // 前端过滤无需重新 loadData
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
