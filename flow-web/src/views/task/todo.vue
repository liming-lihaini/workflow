<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">待办任务</span>
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
          <template v-if="column.key === 'status'">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getTodoTasks, claimTask, transferTask, delegateTask } from '../../api/task'
import { renderDate } from '../../utils/date'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const dataList = ref([])
const currentTask = ref(null)

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程实例', dataIndex: 'processKey', key: 'processKey' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: renderDate },
  { title: '操作', key: 'action', width: 200 }
]

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
  } catch { /* ignore */ }
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
  } catch { /* ignore */ }
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
  } catch { /* ignore */ }
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
  } catch { /* ignore */ }
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
</style>
