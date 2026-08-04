<template>
  <div class="page">
    <a-card :bordered="false">
      <template #title>样品管理</template>
      <template #extra>
        <a-button type="primary" @click="openCreate">新增样品</a-button>
      </template>

      <a-form layout="inline" class="filter" @finish="loadData">
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" style="width:140px" allow-clear @change="loadData">
            <a-select-option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关键字">
          <a-input v-model:value="filters.keyword" placeholder="样品名称/条码" @press-enter="loadData" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button style="margin-left:8px;" @click="resetFilter">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
        style="margin-top:12px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'barcode'">
            <span class="code-link" @click="openDetail(record)">{{ record.barcode }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openReceive(record)" :disabled="record.status !== '待收样'">收样</a-button>
            <a-button type="link" @click="openRetain(record)" :disabled="record.status !== '已收样'">留样</a-button>
            <a-button type="link" @click="openQc(record)">质控样</a-button>
            <a-button type="link" danger @click="remove(record)">删除</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑样品 -->
    <a-modal
      v-model:open="editOpen"
      :title="editForm.id ? '编辑样品' : '新增样品'"
      @ok="submitEdit"
      @cancel="editOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="采样记录ID" required>
          <a-input-number v-model:value="editForm.samplingId" :min="1" style="width:100%" />
        </a-form-item>
        <a-form-item label="样品名称" required>
          <a-input v-model:value="editForm.name" />
        </a-form-item>
        <a-form-item label="样品类型">
          <a-input v-model:value="editForm.type" placeholder="水样/气样/土壤等" />
        </a-form-item>
        <a-form-item label="样品来源">
          <a-input v-model:value="editForm.source" placeholder="监测点位" />
        </a-form-item>
        <a-form-item label="数量/规格">
          <a-input v-model:value="editForm.amount" />
        </a-form-item>
        <a-form-item label="容器">
          <a-input v-model:value="editForm.container" />
        </a-form-item>
        <a-form-item label="保存条件">
          <a-input v-model:value="editForm.preserve" placeholder="固定剂/保存温度" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="editForm.remark" rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 收样 -->
    <a-modal
      v-model:open="receiveOpen"
      title="登记收样"
      @ok="submitReceive"
      @cancel="receiveOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="收样人" required>
          <a-input v-model:value="receiveForm.receiveBy" />
        </a-form-item>
        <a-form-item label="收样时间">
          <a-date-picker v-model:value="receiveDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="receiveForm.remark" rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 留样 -->
    <a-modal
      v-model:open="retainOpen"
      title="登记留样"
      @ok="submitRetain"
      @cancel="retainOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="留样天数" required>
          <a-input-number v-model:value="retainForm.retainDays" :min="1" style="width:100%" />
        </a-form-item>
        <a-form-item label="留样人" required>
          <a-input v-model:value="retainForm.retainBy" />
        </a-form-item>
        <a-form-item label="留样日期">
          <a-date-picker v-model:value="retainDate" value-format="YYYY-MM-DD" style="width:100%" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="retainForm.remark" rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 质控样 -->
    <a-modal
      v-model:open="qcOpen"
      title="绑定质控样"
      @ok="submitQc"
      @cancel="qcOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品编号">
          <a-input v-model:value="qcForm.sampleNo" />
        </a-form-item>
        <a-form-item label="质控类型" required>
          <a-select v-model:value="qcForm.qcType" placeholder="请选择">
            <a-select-option v-for="t in qcTypes" :key="t" :value="t">{{ t }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="qcForm.remark" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情 -->
    <a-drawer v-model:open="detailOpen" title="样品详情" width="720" @close="detailOpen = false">
      <a-descriptions bordered :column="2" size="small" v-if="detail">
        <a-descriptions-item label="样品条码">{{ detail.sample?.barcode }}</a-descriptions-item>
        <a-descriptions-item label="样品名称">{{ detail.sample?.name }}</a-descriptions-item>
        <a-descriptions-item label="样品类型">{{ detail.sample?.type }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ detail.sample?.source }}</a-descriptions-item>
        <a-descriptions-item label="容器">{{ detail.sample?.container }}</a-descriptions-item>
        <a-descriptions-item label="数量/规格">{{ detail.sample?.amount }}</a-descriptions-item>
        <a-descriptions-item label="保存条件">{{ detail.sample?.preserve }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColor(detail.sample?.status)">{{ detail.sample?.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收样人">{{ detail.sample?.receiveBy }}</a-descriptions-item>
        <a-descriptions-item label="收样时间">{{ detail.sample?.receiveTime }}</a-descriptions-item>
        <a-descriptions-item label="留样到期">{{ detail.sample?.retainUntil }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">质控样</a-divider>
      <a-table
        v-if="detail.qcList && detail.qcList.length"
        :columns="qcColumns"
        :data-source="detail.qcList"
        size="small"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" danger @click="unbindQc(record)">解绑</a-button>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="无质控样" />

      <a-divider class="title-divider" orientation="left">操作日志</a-divider>
      <a-timeline v-if="detail.logs && detail.logs.length">
        <a-timeline-item v-for="log in detail.logs" :key="log.id">
          <b>{{ log.action }}</b> · {{ log.operator }} · {{ log.detail }}
          <span style="color:#999;">（{{ log.createTime }}）</span>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无日志" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getSamples,
  createSample,
  updateSample,
  receiveSample,
  retainSample,
  bindSampleQc,
  unbindSampleQc,
  getSampleDetail,
  deleteSample
} from '../../../api/ems'

const loading = ref(false)
const list = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const filters = reactive({ status: undefined, keyword: '' })
const statusOptions = ['待收样', '已收样', '留样中', '已处置']
const qcTypes = ['全程序空白', '现场空白', '平行样', '加标回收', '密码样']

const columns = [
  { title: '样品条码', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '来源', dataIndex: 'source', key: 'source' },
  { title: '保存条件', dataIndex: 'preserve', key: 'preserve', ellipsis: true },
  { title: '状态', key: 'status' },
  { title: '收样人', dataIndex: 'receiveBy', key: 'receiveBy' },
  { title: '留样到期', dataIndex: 'retainUntil', key: 'retainUntil' },
  { title: '操作', key: 'action', width: 220 }
]

const qcColumns = [
  { title: '样品编号', dataIndex: 'sampleNo', key: 'sampleNo' },
  { title: '质控类型', dataIndex: 'qcType', key: 'qcType' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 80 }
]

function statusColor(status) {
  return { '待收样': 'orange', '已收样': 'blue', '留样中': 'purple', '已处置': 'default' }[status] || 'default'
}

const editOpen = ref(false)
const editForm = reactive({
  id: null, samplingId: null, name: '', type: '', source: '',
  amount: '', container: '', preserve: '', remark: ''
})
const receiveOpen = ref(false)
const receiveForm = reactive({ receiveBy: '', remark: '' })
const receiveDate = ref(null)
const retainOpen = ref(false)
const retainForm = reactive({ retainDays: 30, retainBy: '', remark: '' })
const retainDate = ref(null)
const qcOpen = ref(false)
const qcForm = reactive({ sampleNo: '', qcType: '', remark: '' })
const detailOpen = ref(false)
const detail = ref(null)
const current = ref(null)
const submitting = ref(false)

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

function resetFilter() {
  filters.status = undefined
  filters.keyword = ''
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSamples({
      page: pagination.current,
      size: pagination.pageSize,
      status: filters.status,
      keyword: filters.keyword || undefined
    })
    const page = res.data || {}
    list.value = page.records || []
    pagination.total = page.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(editForm, {
    id: null, samplingId: null, name: '', type: '', source: '',
    amount: '', container: '', preserve: '', remark: ''
  })
  editOpen.value = true
}

async function submitEdit() {
  if (!editForm.samplingId) return message.warning('请填写采样记录ID')
  if (!editForm.name) return message.warning('请填写样品名称')
  submitting.value = true
  try {
    if (editForm.id) {
      await updateSample(editForm.id, editForm)
      message.success('保存成功')
    } else {
      await createSample(editForm)
      message.success('创建成功')
    }
    editOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openReceive(record) {
  current.value = record
  receiveForm.receiveBy = ''
  receiveForm.remark = ''
  receiveDate.value = null
  receiveOpen.value = true
}

async function submitReceive() {
  if (!receiveForm.receiveBy) return message.warning('请填写收样人')
  submitting.value = true
  try {
    await receiveSample(current.value.id, {
      receiveBy: receiveForm.receiveBy,
      receiveTime: receiveDate.value,
      remark: receiveForm.remark
    })
    message.success('收样登记成功')
    receiveOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openRetain(record) {
  current.value = record
  retainForm.retainDays = 30
  retainForm.retainBy = ''
  retainForm.remark = ''
  retainDate.value = null
  retainOpen.value = true
}

async function submitRetain() {
  if (!retainForm.retainDays || retainForm.retainDays <= 0) return message.warning('请填写有效的留样天数')
  if (!retainForm.retainBy) return message.warning('请填写留样人')
  submitting.value = true
  try {
    await retainSample(current.value.id, {
      retainDays: retainForm.retainDays,
      retainBy: retainForm.retainBy,
      retainTime: retainDate.value,
      remark: retainForm.remark
    })
    message.success('留样登记成功')
    retainOpen.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openQc(record) {
  current.value = record
  qcForm.sampleNo = record.barcode
  qcForm.qcType = ''
  qcForm.remark = ''
  qcOpen.value = true
}

async function submitQc() {
  if (!qcForm.qcType) return message.warning('请选择质控类型')
  submitting.value = true
  try {
    await bindSampleQc(current.value.id, qcForm)
    message.success('质控样绑定成功')
    qcOpen.value = false
    if (detailOpen.value) openDetail(current.value)
  } finally {
    submitting.value = false
  }
}

async function unbindQc(record) {
  await unbindSampleQc(record.id)
  message.success('已解绑')
  if (current.value) openDetail(current.value)
}

async function openDetail(record) {
  const res = await getSampleDetail(record.id)
  detail.value = res.data
  current.value = record
  detailOpen.value = true
}

async function remove(record) {
  await deleteSample(record.id)
  message.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 4px; }
.filter { margin-bottom: 12px; }
.code-link { color: #1677ff; cursor: pointer; }
.code-link:hover { text-decoration: underline; }
</style>
