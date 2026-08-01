<template>
  <div class="page-container">
    <a-card title="检测复核工作台" :bordered="false">
      <a-alert
        v-if="tasks.length === 0 && !loading"
        type="info"
        show-icon
        message="当前没有待复核的检测任务"
        description="请在「检测数据录入」中录入并保存提交检测结果后，再来此复核。"
        style="margin-bottom:16px"
      />
      <a-table :columns="columns" :data-source="tasks" :loading="loading" row-key="id" :pagination="pagination">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openReview(record)">复核</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="reviewVisible" :title="'复核 - ' + (current?.barcode || '')" width="760" @close="loadTasks">
      <template v-if="current">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="任务号">{{ current.taskNo }}</a-descriptions-item>
          <a-descriptions-item label="条码">{{ current.barcode }}</a-descriptions-item>
          <a-descriptions-item label="样品名称">{{ current.sampleName }}</a-descriptions-item>
          <a-descriptions-item label="录入员">{{ current.entryBy }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>检测结果</a-divider>
        <a-table :columns="resultColumns" :data-source="results" :pagination="false" size="small" row-key="id" />

        <a-divider>复核意见</a-divider>
        <a-form layout="vertical">
          <a-form-item label="复核人">
            <a-input v-model:value="reviewer" />
          </a-form-item>
          <a-form-item label="复核意见">
            <a-textarea v-model:value="opinion" :rows="3" placeholder="通过可留空；退回必填" />
          </a-form-item>
          <a-space>
            <a-button type="primary" :loading="saving" @click="doApprove">通过</a-button>
            <a-button danger :loading="saving" @click="doReject">退回</a-button>
          </a-space>
        </a-form>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getPendingReviews, getDetectionTaskDetail, approveDetection, rejectDetection
} from '@/api/ems'

const loading = ref(false)
const saving = ref(false)
const tasks = ref([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pagination.current = p; loadTasks() } })

const columns = [
  { title: '任务号', dataIndex: 'taskNo', key: 'taskNo' },
  { title: '条码', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'sampleName', key: 'sampleName' },
  { title: '监测项目', dataIndex: 'monitorItems', key: 'monitorItems' },
  { title: '录入员', dataIndex: 'entryBy', key: 'entryBy' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const resultColumns = [
  { title: '监测项目', dataIndex: 'monitorItem', key: 'monitorItem' },
  { title: '检测值', dataIndex: 'value', key: 'value' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '限值', dataIndex: 'limitValue', key: 'limitValue' },
  { title: '结论', key: 'conclusion',
    customRender: ({ text }) => text === '超标' ? '超标' : (text === '达标' ? '达标' : (text || '未判定')) }
]

const statusColor = (s) => ({ '录入中': 'blue', '已提交': 'orange', '已复核': 'green', '已退回': 'red' }[s] || 'default')

const reviewVisible = ref(false)
const current = ref(null)
const results = ref([])
const reviewer = ref('复核员')
const opinion = ref('')

async function loadTasks() {
  loading.value = true
  try {
    const res = await getPendingReviews({ page: pagination.current, size: pagination.pageSize })
    const p = res.data || res
    tasks.value = p.records || p.list || []
    pagination.total = p.total || tasks.value.length
  } finally { loading.value = false }
}

async function openReview(record) {
  current.value = record
  const res = await getDetectionTaskDetail(record.id)
  const d = res.data || res
  results.value = d.results || []
  reviewVisible.value = true
}

async function doApprove() {
  saving.value = true
  try {
    await approveDetection(current.value.id, { reviewer: reviewer.value, opinion: opinion.value })
    message.success('复核通过')
    reviewVisible.value = false
    loadTasks()
  } finally { saving.value = false }
}

async function doReject() {
  if (!opinion.value.trim()) { message.warning('退回必须填写复核意见'); return }
  saving.value = true
  try {
    await rejectDetection(current.value.id, { reviewer: reviewer.value, opinion: opinion.value })
    message.success('已退回')
    reviewVisible.value = false
    loadTasks()
  } finally { saving.value = false }
}

onMounted(loadTasks)
</script>
