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

      <!-- 任务状态卡片（平铺） -->
      <div class="stat-row">
        <div
          v-for="s in statCards"
          :key="s.status"
          :class="['stat-card', { 'stat-active': filterStatus === s.status }]"
          :style="{ '--accent': s.color }"
          @click="onStatClick(s.status)"
        >
          <span class="stat-bar"></span>
          <div class="stat-body">
            <div class="stat-title">{{ s.title }}</div>
            <div class="stat-value">{{ s.value }}</div>
          </div>
        </div>
      </div>

      <a-table :columns="columns" :data-source="tasks" :loading="loading" row-key="id" :pagination="pagination" :scroll="{ y: 420 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskNo'">
            <a @click="goDetail(record)">{{ record.taskNo }}</a>
          </template>
          <template v-else-if="column.key === 'barcode'">
            <a @click="goDetail(record)">{{ record.barcode }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'entryBy'">
            {{ realName(record.entryBy) }}
          </template>
          <template v-else-if="column.key === 'reviewBy'">
            {{ realName(record.reviewBy) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="goEntry(record)">录入</a>
              <a @click="goDetail(record)">详情</a>
              <a v-if="record.status === '已提交'" @click="openReview(record)">复核</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建任务 -->
    <a-modal v-model:open="createVisible" title="新建检测任务" @ok="submitCreate" :confirm-loading="saving">
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="选择样品（已收样 / 留样中）">
          <a-select
            v-model:value="createForm.sampleId"
            placeholder="请选择样品"
            show-search
            option-filter-prop="label"
            @change="onSampleChange"
          >
            <a-select-option v-for="s in pendingSamples" :key="s.id" :value="s.id" :label="(s.barcode || '') + ' ' + (s.name || '')">
              {{ s.barcode }} - {{ s.name }}
              <a-tag v-if="s.status" :color="'green'" style="margin-left:8px">{{ s.status }}</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="检测人员">
          <a-select v-model:value="createForm.entryBy" placeholder="请选择检测人员" show-search option-filter-prop="label">
            <a-select-option v-for="u in userOptions" :key="u.value" :value="u.value" :label="u.label">
              {{ u.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="复核人员">
          <a-select v-model:value="createForm.reviewBy" placeholder="请选择复核人员" show-search option-filter-prop="label">
            <a-select-option v-for="u in userOptions" :key="u.value" :value="u.value" :label="u.label">
              {{ u.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="监测项目（选择样品后自动带出，可调整）">
          <a-select
            v-model:value="createForm.monitorItems"
            mode="tags"
            placeholder="选择样品后自动加载检测项目"
            :options="monitorItemOptions"
            style="width:100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 复核表单 -->
    <a-modal v-model:open="reviewVisible" :title="'复核 - ' + (reviewTask?.barcode || '')" @ok="submitReview" :confirm-loading="saving" ok-text="确定">
      <a-descriptions bordered :column="1" size="small" v-if="reviewTask">
        <a-descriptions-item label="任务号">{{ reviewTask.taskNo }}</a-descriptions-item>
        <a-descriptions-item label="条码">{{ reviewTask.barcode }}</a-descriptions-item>
        <a-descriptions-item label="样品名称">{{ reviewTask.sampleName }}</a-descriptions-item>
        <a-descriptions-item label="录入员">{{ realName(reviewTask.entryBy) }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="review-divider">复核信息</a-divider>
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="复核人">{{ realName(reviewForm.reviewer) }}</a-descriptions-item>
        <a-descriptions-item label="复核时间">{{ reviewForm.reviewTime }}</a-descriptions-item>
        <a-descriptions-item label="复核结论" required>
          <a-radio-group v-model:value="reviewForm.decision">
            <a-radio value="通过">复核通过</a-radio>
            <a-radio value="退回">退回处理</a-radio>
          </a-radio-group>
        </a-descriptions-item>
        <a-descriptions-item label="复核意见">
          <a-textarea v-model:value="reviewForm.opinion" :rows="3" placeholder="复核通过可留空；退回处理必填" />
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getDetectionTasks, getPendingSamples, createDetectionTask, approveDetection, rejectDetection, getDetectionTaskStat
} from '../../../api/ems'
import { getUsers } from '../../../api/system'
import { useUserMap } from '../../../composables/useUserMap'

const router = useRouter()
const { realName, buildUserMap } = useUserMap()

const loading = ref(false)
const saving = ref(false)
const tasks = ref([])
const keyword = ref('')
const filterStatus = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { pagination.current = p; loadTasks() } })

// 任务状态卡片
const taskStat = reactive({ total: 0, 录入中: 0, 已提交: 0, 已复核: 0, 已退回: 0 })
const statCards = reactive([
  { status: '', title: '全部任务', value: 0, color: '#1677ff' },
  { status: '录入中', title: '录入中', value: 0, color: '#1677ff' },
  { status: '已提交', title: '已提交', value: 0, color: '#fa8c16' },
  { status: '已复核', title: '已复核', value: 0, color: '#52c41a' },
  { status: '已退回', title: '已退回', value: 0, color: '#ff4d4f' }
])
function refreshStatCards() {
  statCards[0].value = taskStat.total
  statCards[1].value = taskStat.录入中
  statCards[2].value = taskStat.已提交
  statCards[3].value = taskStat.已复核
  statCards[4].value = taskStat.已退回
}
function onStatClick(status) {
  filterStatus.value = status
  pagination.current = 1
  loadTasks()
}
async function loadStat() {
  try {
    const res = await getDetectionTaskStat()
    const data = res.data || res
    Object.assign(taskStat, {
      total: data.total || 0,
      录入中: data['录入中'] || 0,
      已提交: data['已提交'] || 0,
      已复核: data['已复核'] || 0,
      已退回: data['已退回'] || 0
    })
    refreshStatCards()
  } catch (e) { /* 统计失败不影响主列表 */ }
}

const columns = [
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo' },
  { title: '样品编号', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'sampleName', key: 'sampleName', width: 220, ellipsis: true },
  { title: '监测项目', dataIndex: 'monitorItems', key: 'monitorItems' },
  { title: '录入员', dataIndex: 'entryBy', key: 'entryBy' },
  { title: '复核人', dataIndex: 'reviewBy', key: 'reviewBy' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action' }
]

const resultColumns = [
  { title: '检测项目名称', dataIndex: 'monitorItem', key: 'monitorItem' },
  { title: '检测标准', dataIndex: 'method', key: 'method' },
  { title: '合格限值', dataIndex: 'limitValue', key: 'limitValue' },
  { title: '实测结果', key: 'value', width: 120 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单项判定', key: 'conclusion', width: 120 },
  { title: '项目备注', key: 'remark', width: 140 }
]

const statusColor = (s) => ({ '录入中': 'blue', '已提交': 'orange', '已复核': 'green', '已退回': 'red' }[s] || 'default')

// 新建
const createVisible = ref(false)
const pendingSamples = ref([])
const userOptions = ref([])
const monitorItemOptions = ref([])
const createForm = reactive({ sampleId: undefined, entryBy: undefined, reviewBy: undefined, monitorItems: [] })

async function loadTasks() {
  loading.value = true
  try {
    const res = await getDetectionTasks({ status: filterStatus.value, keyword: keyword.value, page: pagination.current, size: pagination.pageSize })
    const p = res.data || res
    tasks.value = p.records || p.list || []
    pagination.total = p.total || tasks.value.length
  } finally {
    loading.value = false
    loadStat()
  }
}

async function showCreate() {
  createVisible.value = true
  // 加载可创建任务的样品（已收样 / 留样中）
  const res = await getPendingSamples()
  pendingSamples.value = res.data || res || []
  // 加载检测人员
  try {
    const ures = await getUsers()
    const udata = ures.data || ures
    const records = Array.isArray(udata) ? udata : (udata.list || udata.records || [])
    userOptions.value = records.map(u => ({ value: u.username || u.name || String(u.id), label: u.realName || u.name || u.username || String(u.id) }))
  } catch (e) { userOptions.value = [] }
}

// 选择样品后，自动加载其检测项目（item 字段，逗号分隔）
function onSampleChange(sampleId) {
  const s = pendingSamples.value.find(x => x.id === sampleId)
  if (s && s.item) {
    monitorItemOptions.value = s.item.split(',').map(x => x.trim()).filter(Boolean).map(x => ({ value: x, label: x }))
    createForm.monitorItems = monitorItemOptions.value.map(o => o.value)
  } else {
    monitorItemOptions.value = []
    createForm.monitorItems = []
  }
}

async function submitCreate() {
  if (!createForm.sampleId) { message.warning('请选择样品'); return }
  if (!createForm.entryBy) { message.warning('请选择检测人员'); return }
  const items = (createForm.monitorItems || []).join(',')
  if (!items) { message.warning('请选择检测项目'); return }
  saving.value = true
  try {
    const res = await createDetectionTask({ sampleId: createForm.sampleId, entryBy: createForm.entryBy, reviewBy: createForm.reviewBy, monitorItems: items })
    const created = res.data || res
    if (created && created.id && created.entryBy && created.entryBy !== createForm.entryBy) {
      // 后端幂等：该样品已存在检测任务，接口返回的是已有任务
      message.warning(`该样品已存在检测任务（任务号：${created.taskNo || created.id}）`)
    } else {
      message.success('检测任务已创建')
    }
    createVisible.value = false
    createForm.sampleId = undefined
    createForm.entryBy = undefined
    createForm.reviewBy = undefined
    createForm.monitorItems = []
    monitorItemOptions.value = []
    loadTasks()
  } finally { saving.value = false }
}

// 录入：跳转独立录入页面
function goEntry(record) {
  router.push('/ems/base/detection/entry/' + record.id)
}

// 详情：跳转独立详情页面（参考录入界面展示方式）
function goDetail(record) {
  router.push('/ems/base/detection/detail/' + record.id)
}

// 复核：打开复核表单（复核人=任务指定复核人，复核时间=当前时间）
const reviewVisible = ref(false)
const reviewTask = ref(null)
const reviewForm = reactive({ reviewer: '', reviewTime: '', decision: '通过', opinion: '' })

function openReview(record) {
  reviewTask.value = record
  reviewForm.reviewer = record.reviewBy || ''
  reviewForm.reviewTime = new Date().toLocaleString('zh-CN', { hour12: false })
  reviewForm.decision = '通过'
  reviewForm.opinion = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!reviewForm.reviewer) { message.warning('该任务未指定复核人'); return }
  if (reviewForm.decision === '退回' && !reviewForm.opinion.trim()) {
    message.warning('退回处理必须填写复核意见'); return
  }
  saving.value = true
  try {
    const payload = { reviewer: reviewForm.reviewer, opinion: reviewForm.opinion }
    if (reviewForm.decision === '通过') {
      await approveDetection(reviewTask.value.id, payload)
      message.success('复核通过')
    } else {
      await rejectDetection(reviewTask.value.id, payload)
      message.success('已退回处理')
    }
    reviewVisible.value = false
    loadTasks()
  } finally { saving.value = false }
}

onMounted(() => {
  buildUserMap()
  loadTasks()
})
</script>

<style scoped>
.mt-16 { margin-top: 16px; }
.review-divider { margin: 16px 0 12px; font-weight: 600; }
/* 任务状态卡片（平铺） */
.stat-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: stretch;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  transition: all .25s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}
.stat-bar {
  width: 5px;
  background: var(--accent);
  flex: none;
}
.stat-body {
  flex: 1;
  padding: 14px 16px;
}
.stat-title {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin-bottom: 6px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent);
}
.stat-active {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent);
}
.stat-active .stat-bar { width: 6px; }
</style>
