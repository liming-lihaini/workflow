<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">委托与代理</span>
        <a-button v-if="hasPerm('task:delegation:create')" type="primary" @click="showCreateModal">+ 创建委托</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        :resize-column="true"
        @resizeColumn="handleResizeColumn"
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
            <a-popconfirm
              v-if="record.status === 0 && hasPerm('task:delegation:cancel')"
              title="确定取消该委托？取消后代理人将无法继续代办您的任务"
              @confirm="doCancel(record)"
            >
              <span class="action-link" style="color: #f53f3f">取消委托</span>
            </a-popconfirm>
            <span v-else class="text-muted">-</span>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 创建委托弹窗 -->
    <a-modal v-model:open="createVisible" title="创建全局委托" @ok="doCreate" :width="520">
      <a-form layout="vertical">
        <a-form-item label="代理人（必填）" required>
          <a-select
            v-model:value="form.delegateUserId"
            show-search
            :filter-option="false"
            placeholder="搜索用户姓名/用户名"
            :options="userOptions"
            @search="onUserSearch"
            :loading="searchLoading"
            style="width: 100%"
          >
            <template #option="{ value, label, deptName, postName }">
              <span>{{ label }} - {{ deptName || '-' }} / {{ postName || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="委托时间区间（必填）" required>
          <a-range-picker
            v-model:value="form.dateRange"
            :placeholder="['开始日期', '结束日期']"
            style="width: 100%"
            value-format="YYYY-MM-DD HH:mm:ss"
            :show-time="{ defaultValue: [dayjs('00:00:00', 'HH:mm:ss'), dayjs('23:59:59', 'HH:mm:ss')] }"
          />
        </a-form-item>
        <a-form-item label="委托说明">
          <a-textarea v-model:value="form.reason" :rows="3" placeholder="请输入委托说明（如：出差期间代为处理）" :maxlength="200" show-count />
        </a-form-item>
      </a-form>
      <div style="color: #999; font-size: 12px; margin-top: -8px;">
        说明：创建后，代理人可在委托时间区间内查看并办理您的全部待办任务。结束时间留空则为永久委托。
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { getMyDelegations, createDelegation, cancelDelegation, searchUsers } from '../../api/task'
import { useUserStore } from '../../stores/user'
import { usePermission } from '../../composables/usePermission'
import { useResizableColumns } from '../../composables/useResizableTable'

const { hasPerm } = usePermission()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const { columns, handleResizeColumn } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '代理人', dataIndex: 'delegateName', key: 'delegateName', width: 120 },
  { title: '委托时间区间', key: 'timeRange', width: 280 },
  { title: '委托说明', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90, sorter: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, sorter: true },
  { title: '操作', key: 'action', width: 100 }
])

// 创建委托
const createVisible = ref(false)
const form = reactive({ delegateUserId: undefined, dateRange: null, reason: '' })
const userOptions = ref([])
const searchLoading = ref(false)
let searchTimer = null

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
    const res = await getMyDelegations({ userId })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = dataList.value.length
  } catch {}
  loading.value = false
}

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

function onUserSearch(val) {
  clearTimeout(searchTimer)
  searchLoading.value = true
  searchTimer = setTimeout(async () => {
    userOptions.value = await doSearchUsers(val)
    searchLoading.value = false
  }, 300)
}

function showCreateModal() {
  form.delegateUserId = undefined
  form.dateRange = null
  form.reason = ''
  userOptions.value = []
  createVisible.value = true
}

async function doCreate() {
  if (!form.delegateUserId) { message.warning('请选择代理人'); return }
  if (!form.dateRange || form.dateRange.length < 2) { message.warning('请选择委托时间区间'); return }
  const userId = userStore.username || localStorage.getItem('username') || ''
  try {
    await createDelegation({
      operatorId: userId,
      delegateUserId: form.delegateUserId,
      reason: form.reason.trim(),
      startTime: form.dateRange[0],
      endTime: form.dateRange[1]
    })
    message.success('委托创建成功')
    createVisible.value = false
    loadData()
  } catch {}
}

async function doCancel(record) {
  const userId = userStore.username || localStorage.getItem('username') || ''
  try {
    await cancelDelegation(record.id, { operatorId: userId })
    message.success('委托已取消')
    loadData()
  } catch {}
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
.action-link { cursor: pointer; }
.action-link:hover { text-decoration: underline; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { font-size: 18px; font-weight: bold; }
</style>
