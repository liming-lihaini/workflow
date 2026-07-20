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
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="handleClaim(record)" v-if="!record.assignee">签收</span>
            <template v-if="record.assignee">
              <span class="action-link" @click="showCompleteModal(record)">通过</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showRejectModal(record)">驳回</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showTransferModal(record)">转办</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showDelegateModal(record)">委派</span>
            </template>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 通过弹窗 -->
    <a-modal v-model:open="completeVisible" title="审批通过" @ok="handleComplete">
      <a-form layout="vertical">
        <a-form-item label="审批意见">
          <a-textarea v-model:value="completeComment" :rows="3" placeholder="请输入审批意见" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 驳回弹窗 -->
    <a-modal v-model:open="rejectVisible" title="驳回" @ok="handleReject">
      <a-form layout="vertical">
        <a-form-item label="驳回原因" required>
          <a-textarea v-model:value="rejectReason" :rows="3" placeholder="请输入驳回原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转办弹窗 -->
    <a-modal v-model:open="transferVisible" title="转办" @ok="handleTransfer">
      <a-form layout="vertical">
        <a-form-item label="转办人ID" required>
          <a-input-number v-model:value="transferUserId" style="width: 100%" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 委派弹窗 -->
    <a-modal v-model:open="delegateVisible" title="委派" @ok="handleDelegate">
      <a-form layout="vertical">
        <a-form-item label="委派人ID" required>
          <a-input-number v-model:value="delegateUserId" style="width: 100%" placeholder="请输入目标用户ID" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTodoTasks, claimTask, completeTask, rejectTask, transferTask, delegateTask } from '../../api/task'

const loading = ref(false)
const dataList = ref([])
const currentTask = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程实例ID', dataIndex: 'processInstanceId', key: 'processInstanceId' },
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName' },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 260 }
]

// 通过
const completeVisible = ref(false)
const completeComment = ref('')
// 驳回
const rejectVisible = ref(false)
const rejectReason = ref('')
// 转办
const transferVisible = ref(false)
const transferUserId = ref(null)
// 委派
const delegateVisible = ref(false)
const delegateUserId = ref(null)

function showCompleteModal(record) {
  currentTask.value = record
  completeComment.value = ''
  completeVisible.value = true
}
function showRejectModal(record) {
  currentTask.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}
function showTransferModal(record) {
  currentTask.value = record
  transferUserId.value = null
  transferVisible.value = true
}
function showDelegateModal(record) {
  currentTask.value = record
  delegateUserId.value = null
  delegateVisible.value = true
}

async function loadData() {
  loading.value = true
  try {
    const res = await getTodoTasks({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = data.list || data.records || []
    pagination.total = data.total || dataList.value.length
  } catch {
    // ignore
  }
  loading.value = false
}

async function handleClaim(record) {
  try {
    await claimTask(record.id, { userId: record.assignee || 'current-user' })
    message.success('签收成功')
    loadData()
  } catch {
    // ignore
  }
}

async function handleComplete() {
  try {
    await completeTask(currentTask.value.id, { opinion: completeComment.value })
    message.success('审批通过')
    completeVisible.value = false
    loadData()
  } catch {
    // ignore
  }
}

async function handleReject() {
  if (!rejectReason.value) {
    message.warning('请输入驳回原因')
    return
  }
  try {
    await rejectTask(currentTask.value.id, { comment: rejectReason.value })
    message.success('已驳回')
    rejectVisible.value = false
    loadData()
  } catch {
    // ignore
  }
}

async function handleTransfer() {
  if (!transferUserId.value) {
    message.warning('请输入转办人ID')
    return
  }
  try {
    await transferTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      targetUserId: String(transferUserId.value)
    })
    message.success('转办成功')
    transferVisible.value = false
    loadData()
  } catch {
    // ignore
  }
}

async function handleDelegate() {
  if (!delegateUserId.value) {
    message.warning('请输入委派人ID')
    return
  }
  try {
    await delegateTask(currentTask.value.id, {
      operatorId: currentTask.value.assignee,
      delegateUserId: String(delegateUserId.value)
    })
    message.success('委派成功')
    delegateVisible.value = false
    loadData()
  } catch {
    // ignore
  }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(loadData)
</script>
