<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">流程实例</span>
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
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="handleView(record)">详情</span>
            <a-divider type="vertical" v-if="record.status === 0" />
            <a-popconfirm
              v-if="record.status === 0"
              title="确定终止该流程？"
              @confirm="handleTerminate(record)"
            >
              <span class="action-link danger">终止</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="流程实例详情" :footer="null" width="640px">
      <a-descriptions :column="2" bordered v-if="currentRecord">
        <a-descriptions-item label="实例ID">{{ currentRecord.id }}</a-descriptions-item>
        <a-descriptions-item label="流程名称">{{ currentRecord.processName }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(currentRecord.status)">{{ statusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="当前节点">{{ currentRecord.currentNodeId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ formatDate(currentRecord.createTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getProcessInstances, terminateProcessInstance } from '../../../api/process'
import { formatDate } from '../../../utils/date'

const loading = ref(false)
const dataList = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '流程名称', dataIndex: 'processName', key: 'processName' },
  { title: '状态', key: 'status', width: 120 },
  { title: '当前节点', dataIndex: 'currentNodeId', key: 'currentNodeId' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: ({ text }) => formatDate(text) },
  { title: '操作', key: 'action', width: 150 }
]

function statusText(status) {
  const map = { 0: '运行中', 1: '已完成', 2: '已暂停', 3: '已终止' }
  return map[status] || '未知'
}

function statusColor(status) {
  const map = { 0: 'blue', 1: 'green', 2: 'orange', 3: 'red' }
  return map[status] || 'default'
}

function handleView(record) {
  currentRecord.value = record
  detailVisible.value = true
}

async function handleTerminate(record) {
  try {
    await terminateProcessInstance(record.id)
    message.success('终止成功')
    loadData()
  } catch {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProcessInstances({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || res
    dataList.value = Array.isArray(data) ? data : (data.list || data.records || [])
    pagination.total = data.total || dataList.value.length
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
