<template>
  <a-drawer
    :open="open"
    :title="'设备详情 - ' + (detail.code || '')"
    width="1000"
    @close="$emit('close')"
  >
    <template v-if="loading">
      <a-skeleton active />
    </template>
    <template v-else>
      <a-divider class="title-divider" orientation="left">基本信息</a-divider>
      <a-descriptions bordered :column="2" size="small">
        <a-descriptions-item label="仪器编号">{{ detail.code || '—' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="instStatusColor(detail.status)">{{ detail.status || '—' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="名称">{{ detail.name || '—' }}</a-descriptions-item>
        <a-descriptions-item label="型号">{{ detail.model || '—' }}</a-descriptions-item>
        <a-descriptions-item label="生产厂商">{{ detail.manufacturer || '—' }}</a-descriptions-item>
        <a-descriptions-item label="购置日期">{{ detail.purchaseDate || '—' }}</a-descriptions-item>
        <a-descriptions-item label="上次校准日期">{{ detail.lastCalibDate || '—' }}</a-descriptions-item>
        <a-descriptions-item label="校准到期日">{{ detail.calibDue || '—' }}</a-descriptions-item>
        <a-descriptions-item label="证书编号">{{ detail.certNo || '—' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">校准记录</a-divider>
      <a-table
        v-if="detail.calibRecords && detail.calibRecords.length"
        :dataSource="detail.calibRecords"
        :columns="calibColumns"
        rowKey="createTime"
        size="small"
        :pagination="false"
      />
      <a-empty v-else description="暂无校准记录" :image="simpleImage" />

      <a-divider class="title-divider" orientation="left">关联采样任务</a-divider>
      <a-table
        v-if="detail.samplingTasks && detail.samplingTasks.length"
        :dataSource="detail.samplingTasks"
        :columns="taskColumns"
        rowKey="orderId"
        size="small"
        :pagination="false"
      />
      <a-empty v-else description="暂无关联采样任务" :image="simpleImage" />
    </template>
  </a-drawer>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import { getInstrumentDetail } from '../../../api/ems'

const props = defineProps({
  open: { type: Boolean, default: false },
  instrumentId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close'])

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const detail = ref({})
const loading = ref(false)

const calibColumns = [
  { title: '校准日期', dataIndex: 'calibDate', key: 'calibDate' },
  { title: '下次到期', dataIndex: 'calibDue', key: 'calibDue' },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '登记时间', dataIndex: 'createTime', key: 'createTime' }
]
const taskColumns = [
  { title: '采样单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '状态', dataIndex: 'orderStatus', key: 'orderStatus' },
  { title: '计划开始', dataIndex: 'planStart', key: 'planStart' },
  { title: '计划结束', dataIndex: 'planEnd', key: 'planEnd' }
]

function instStatusColor(s) {
  return { '在用': 'green', '临期': 'orange', '停用': 'red', '维修': 'blue', '报废': 'default' }[s] || 'default'
}

watch(() => [props.open, props.instrumentId], ([o, id]) => {
  if (o && id) {
    loading.value = true
    detail.value = {}
    getInstrumentDetail(id).then((res) => {
      detail.value = res.data || res || {}
    }).catch(() => { detail.value = {} })
      .finally(() => { loading.value = false })
  }
}, { immediate: true })
</script>
