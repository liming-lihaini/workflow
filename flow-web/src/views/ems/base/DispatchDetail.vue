<template>
  <a-drawer
    :open="open"
    :title="'派单详情 - ' + (detail.orderNo || '')"
    width="520"
    @close="$emit('close')"
  >
    <template v-if="loading">
      <a-skeleton active />
    </template>
    <template v-else>
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="订单编号">{{ detail.orderNo || '—' }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">{{ detail.orderStatus || '—' }}</a-descriptions-item>
        <a-descriptions-item label="派单状态">
          <a-tag v-if="detail.dispatchId" color="blue">已派单</a-tag>
          <a-tag v-else color="default">未派单</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="计划开始">{{ detail.planStart || '—' }}</a-descriptions-item>
        <a-descriptions-item label="计划结束">{{ detail.planEnd || '—' }}</a-descriptions-item>
        <a-descriptions-item label="备注">{{ detail.note || '—' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider orientation="left">负责人 / 组员</a-divider>
      <a-descriptions bordered :column="1" size="small" v-if="detail.lead || (detail.members && detail.members.length)">
        <a-descriptions-item v-if="detail.lead" label="负责人">
          {{ detail.lead.realName || detail.lead.username }}（{{ detail.lead.username }}）
        </a-descriptions-item>
        <a-descriptions-item v-if="detail.members && detail.members.length" label="组员">
          <div v-for="m in detail.members" :key="m.userId">
            {{ m.realName || m.username }}（{{ m.username }}）
          </div>
        </a-descriptions-item>
      </a-descriptions>
      <a-empty v-else description="暂无人员" :image="simpleImage" />

      <a-divider orientation="left">车辆</a-divider>
      <a-descriptions bordered :column="1" size="small" v-if="detail.vehicle">
        <a-descriptions-item label="车牌 / 车型">{{ detail.vehicle.plateNo }} {{ detail.vehicle.model || '' }}</a-descriptions-item>
      </a-descriptions>
      <a-empty v-else description="未分配车辆" :image="simpleImage" />

      <a-divider orientation="left">设备</a-divider>
      <a-table
        v-if="detail.instruments && detail.instruments.length"
        :dataSource="detail.instruments"
        :columns="insColumns"
        rowKey="id"
        size="small"
        :pagination="false"
      />
      <a-empty v-else description="未分配设备" :image="simpleImage" />
    </template>
  </a-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { Empty } from 'ant-design-vue'
import { getDispatchDetail } from '../../../api/ems'

const props = defineProps({
  open: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close'])

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const detail = ref({})
const loading = ref(false)

const insColumns = [
  { title: '编号', dataIndex: 'code', key: 'code' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '校准到期', dataIndex: 'calibDue', key: 'calibDue' }
]

watch(() => [props.open, props.orderId], ([o, id]) => {
  if (o && id) {
    loading.value = true
    detail.value = {}
    getDispatchDetail(id).then((res) => {
      detail.value = res.data || res || {}
    }).catch(() => {
      detail.value = {}
    }).finally(() => { loading.value = false })
  }
}, { immediate: true })
</script>
