<template>
  <div class="page-container">
    <a-card title="检测数据录入工作台" :bordered="false">
      <a-space style="margin-bottom:16px">
        <a-button type="primary" @click="showCreate">+ 新建检测任务</a-button>
        <a-select v-model:value="filterStatus" style="width:160px" @change="loadTasks" placeholder="按状态筛选">
          <a-select-option value="">全部状态</a-select-option>
          <a-select-option value="录入中">录入中</a-select-option>
          <a-select-option value="已提交">已提交</a-select-option>
          <a-select-option value="已复核">已复核</a-select-option>
          <a-select-option value="已退回">已退回</a-select-option>
        </a-select>
        <a-input-search v-model:value="keyword" placeholder="条码/名称/任务号" style="width:240px" @search="loadTasks" allow-clear />
      </a-space>

      <a-table :columns="columns" :data-source="tasks" :loading="loading" row-key="id" :pagination="pagination">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openEntry(record)">录入</a>
              <a @click="openDetail(record)">详情</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建任务 -->
    <a-modal v-model:open="createVisible" title="新建检测任务" @ok="submitCreate" :confirm-loading="saving">
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="选择已收样样品">
          <a-select v-model:value="createForm.sampleId" placeholder="请选择样品" show-search option-filter-prop="label">
            <a-select-option v-for="s in pendingSamples" :key="s.id" :value="s.id" :label="s.barcode + ' ' + s.name">
              {{ s.barcode }} - {{ s.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="监测项目（逗号分隔）">
          <a-input v-model:value="createForm.monitorItems" placeholder="例如 pH,COD,氨氮,总磷" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 录入抽屉 -->
    <a-drawer v-model:open="entryVisible" :title="'录入检测结果 - ' + (current?.barcode || '')" width="720" @close="loadTasks">
      <a-form layout="vertical">
        <a-form-item label="录入员">
          <a-input v-model:value="entryBy" />
        </a-form-item>
        <a-table :columns="resultColumns" :data-source="resultRows" :pagination="false" size="small">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'value'">
              <a-input v-model:value="record.value" placeholder="检测值" />
            </template>
            <template v-else-if="column.key === 'conclusion'">
              <a-tag :color="record.conclusion === '超标' ? 'red' : (record.conclusion === '达标' ? 'green' : 'default')">
                {{ record.conclusion || '未判定' }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <a-space style="margin-top:16px">
          <a-button type="primary" :loading="saving" @click="saveResults">保存结果</a-button>
          <a-button type="primary" ghost :loading="saving" @click="saveAndSubmit">保存并提交复核</a-button>
        </a-space>
      </a-form>
    </a-drawer>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="检测任务详情" width="720" @close="loadTasks">
      <template v-if="detail">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="任务号">{{ detail.task.taskNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(detail.task.status)">{{ detail.task.status }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="条码">{{ detail.task.barcode }}</a-descriptions-item>
          <a-descriptions-item label="样品名称">{{ detail.task.sampleName }}</a-descriptions-item>
          <a-descriptions-item label="录入员">{{ detail.task.entryBy }}</a-descriptions-item>
          <a-descriptions-item label="复核人">{{ detail.task.reviewBy || '-' }}</a-descriptions-item>
          <a-descriptions-item label="复核意见" :span="2">{{ detail.task.reviewOpinion || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-divider class="title-divider">检测结果</a-divider>
        <a-table :columns="resultColumns" :data-source="detail.results" :pagination="false" size="small" row-key="id">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'conclusion'">
              <a-tag :color="record.conclusion === '超标' ? 'red' : (record.conclusion === '达标' ? 'green' : 'default')">
                {{ record.conclusion || '未判定' }}
              </a-tag>
            </template>
          </template>
        </a-table>
        <a-divider v-if="detail.reviews && detail.reviews.length" class="title-divider">复核记录</a-divider>
        <a-timeline v-if="detail.reviews && detail.reviews.length">
          <a-timeline-item v-for="r in detail.reviews" :key="r.id" :color="r.decision === '通过' ? 'green' : 'red'">
            {{ r.reviewer }} - {{ r.decision }} - {{ r.opinion }}（{{ r.createTime }}）
          </a-timeline-item>
        </a-timeline>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getDetectionTasks, getPendingSamples, createDetectionTask, getDetectionTaskDetail,
  saveDetectionResults, submitDetection
} from '../../../api/ems'

const loading = ref(false)
const saving = ref(false)
const tasks = ref([])
const keyword = ref('')
const filterStatus = ref('')
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
  { title: '检测值', key: 'value', width: 140 },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '限值', dataIndex: 'limitValue', key: 'limitValue' },
  { title: '结论', key: 'conclusion' }
]

const statusColor = (s) => ({ '录入中': 'blue', '已提交': 'orange', '已复核': 'green', '已退回': 'red' }[s] || 'default')

// 新建
const createVisible = ref(false)
const pendingSamples = ref([])
const createForm = reactive({ sampleId: undefined, monitorItems: 'pH,COD,氨氮' })

async function loadTasks() {
  loading.value = true
  try {
    const res = await getDetectionTasks({ status: filterStatus.value, keyword: keyword.value, page: pagination.current, size: pagination.pageSize })
    const p = res.data || res
    tasks.value = p.records || p.list || []
    pagination.total = p.total || tasks.value.length
  } finally { loading.value = false }
}

async function showCreate() {
  createVisible.value = true
  const res = await getPendingSamples()
  pendingSamples.value = res.data || res || []
}

async function submitCreate() {
  if (!createForm.sampleId) { message.warning('请选择样品'); return }
  saving.value = true
  try {
    await createDetectionTask({ ...createForm })
    message.success('检测任务已创建')
    createVisible.value = false
    createForm.sampleId = undefined
    loadTasks()
  } finally { saving.value = false }
}

// 录入
const entryVisible = ref(false)
const current = ref(null)
const entryBy = ref('录入员')
const resultRows = ref([])

async function openEntry(record) {
  current.value = record
  const res = await getDetectionTaskDetail(record.id)
  const d = res.data || res
  resultRows.value = (d.results && d.results.length ? d.results : record.monitorItems.split(',').map(m => ({
    monitorItem: m.trim(), value: '', unit: '', limitValue: '', conclusion: ''
  }))).map(r => ({ ...r, value: r.value || '', unit: r.unit || '', limitValue: r.limitValue || '', conclusion: r.conclusion || '' }))
  entryVisible.value = true
}

async function saveResults() {
  saving.value = true
  try {
    await saveDetectionResults(current.value.id, { entryBy: entryBy.value, results: resultRows.value })
    message.success('结果已保存')
  } finally { saving.value = false }
}

async function saveAndSubmit() {
  saving.value = true
  try {
    await saveDetectionResults(current.value.id, { entryBy: entryBy.value, results: resultRows.value })
    await submitDetection(current.value.id)
    message.success('已保存并提交复核')
    entryVisible.value = false
    loadTasks()
  } finally { saving.value = false }
}

// 详情
const detailVisible = ref(false)
const detail = ref(null)
async function openDetail(record) {
  const res = await getDetectionTaskDetail(record.id)
  detail.value = res.data || res
  detailVisible.value = true
}

onMounted(loadTasks)
</script>
