<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">代理记录</span>
        <a-button @click="loadData">刷新</a-button>
      </div>

      <div class="hint-bar">
        以下记录表示其他用户委托您代为处理流程任务。委托生效期间，您可在待办任务中看到委托人的任务并代为办理。
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
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ record.statusDesc || '未知' }}</a-tag>
          </template>
          <template v-if="column.key === 'timeRange'">
            {{ formatDateTime(record.startTime) }} ~ {{ record.endTime ? formatDateTime(record.endTime) : '永久' }}
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="goToTodo">查看待办</span>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProxyDelegations } from '../../api/task'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '委托人', dataIndex: 'delegatorName', key: 'delegatorName', width: 120 },
  { title: '委托时间区间', key: 'timeRange', width: 280 },
  { title: '委托说明', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 100 }
]

function getStatusColor(status) {
  if (status === 0) return 'blue'
  if (status === 2) return 'default'
  if (status === 3) return 'orange'
  return 'default'
}

function formatDateTime(val) {
  if (!val) return '-'
  return String(val).substring(0, 16).replace('T', ' ')
}

async function loadData() {
  loading.value = true
  try {
    const userId = userStore.username || localStorage.getItem('username') || ''
    const res = await getProxyDelegations({ userId })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = dataList.value.length
  } catch {}
  loading.value = false
}

function goToTodo() {
  router.push('/task/todo')
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.text-muted { color: #999; }
.action-link { cursor: pointer; color: #1677ff; }
.action-link:hover { text-decoration: underline; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.page-title { font-size: 18px; font-weight: bold; }
.hint-bar {
  background: #f0f7ff;
  border: 1px solid #91caff;
  border-radius: 6px;
  padding: 10px 14px;
  color: #555;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
