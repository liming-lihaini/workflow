<template>
  <div class="page">
    <a-card :bordered="false">
      <template #title>收样工作台</template>
      <template #extra>
        <a-badge :count="stats.pending" :number-style="{ backgroundColor: '#fa8c16' }" />
        <span style="margin-left:8px;color:#888;">待收样样品</span>
        <a-button type="primary" style="margin-left:16px;" @click="loadData">刷新</a-button>
      </template>

      <a-alert
        v-if="stats.pending > 0"
        type="warning"
        show-icon
        style="margin-bottom:16px;"
        :message="`当前有 ${stats.pending} 个样品待收样，请核对保存条件后登记收样`"
      />

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag color="orange">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openReceive(record)">登记收样</a-button>
            <a-button type="link" @click="openDetail(record)">详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 收样弹窗 -->
    <a-modal
      v-model:open="receiveOpen"
      title="登记收样"
      @ok="submitReceive"
      @cancel="receiveOpen = false"
      :confirm-loading="submitting"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="样品条码">
          <span>{{ current?.barcode }}</span>
        </a-form-item>
        <a-form-item label="样品名称">
          <span>{{ current?.name }}</span>
        </a-form-item>
        <a-form-item label="保存条件">
          <span>{{ current?.preserve || '—' }}</span>
        </a-form-item>
        <a-form-item label="收样人" required>
          <a-input v-model:value="receiveForm.receiveBy" placeholder="请输入收样人" />
        </a-form-item>
        <a-form-item label="收样时间">
          <a-date-picker
            v-model:value="receiveDate"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="receiveForm.remark" rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 样品详情抽屉 -->
    <a-drawer
      v-model:open="detailOpen"
      title="样品详情"
      width="720"
      @close="detailOpen = false"
    >
      <a-descriptions bordered :column="2" size="small" v-if="detail">
        <a-descriptions-item label="样品条码">{{ detail.sample?.barcode }}</a-descriptions-item>
        <a-descriptions-item label="样品名称">{{ detail.sample?.name }}</a-descriptions-item>
        <a-descriptions-item label="样品类型">{{ detail.sample?.type }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ detail.sample?.source }}</a-descriptions-item>
        <a-descriptions-item label="容器">{{ detail.sample?.container }}</a-descriptions-item>
        <a-descriptions-item label="数量/规格">{{ detail.sample?.amount }}</a-descriptions-item>
        <a-descriptions-item label="保存条件">{{ detail.sample?.preserve }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag color="orange">{{ detail.sample?.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收样人">{{ detail.sample?.receiveBy }}</a-descriptions-item>
        <a-descriptions-item label="收样时间">{{ detail.sample?.receiveTime }}</a-descriptions-item>
      </a-descriptions>

      <a-divider orientation="left">质控样</a-divider>
      <a-table
        v-if="detail.qcList"
        :columns="qcColumns"
        :data-source="detail.qcList"
        size="small"
        row-key="id"
        :pagination="false"
      />
      <a-empty v-else description="无质控样" />

      <a-divider orientation="left">操作日志</a-divider>
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
  getReceiveWorkbench,
  receiveSample,
  getSampleDetail
} from '../../../api/ems'

const loading = ref(false)
const list = ref([])
const stats = reactive({ pending: 0 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '样品条码', dataIndex: 'barcode', key: 'barcode' },
  { title: '样品名称', dataIndex: 'name', key: 'name' },
  { title: '样品类型', dataIndex: 'type', key: 'type' },
  { title: '来源', dataIndex: 'source', key: 'source' },
  { title: '保存条件', dataIndex: 'preserve', key: 'preserve', ellipsis: true },
  { title: '采样记录ID', dataIndex: 'samplingId', key: 'samplingId' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 160 }
]

const qcColumns = [
  { title: '样品编号', dataIndex: 'sampleNo', key: 'sampleNo' },
  { title: '质控类型', dataIndex: 'qcType', key: 'qcType' },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
]

const receiveOpen = ref(false)
const submitting = ref(false)
const current = ref(null)
const receiveForm = reactive({ receiveBy: '', remark: '' })
const receiveDate = ref(null)

const detailOpen = ref(false)
const detail = ref(null)

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getReceiveWorkbench({ page: pagination.current, size: pagination.pageSize })
    const data = res.data || {}
    list.value = data.pendingSamples || []
    stats.pending = data.pendingStatusCount || 0
    pagination.total = data.pendingTotal || 0
  } finally {
    loading.value = false
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
  if (!receiveForm.receiveBy) {
    message.warning('请填写收样人')
    return
  }
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

async function openDetail(record) {
  const res = await getSampleDetail(record.id)
  detail.value = res.data
  detailOpen.value = true
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 4px; }
</style>
