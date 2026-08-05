<template>
  <a-drawer
    :open="open"
    :title="'派单详情 - ' + (detail.orderNo || '')"
    width="1000"
    @close="$emit('close')"
  >
    <template v-if="loading">
      <a-skeleton active />
    </template>
    <template v-else>
      <a-card size="small" class="status-flow-card">
        <div class="status-flow">
          <template v-for="(s, idx) in statusSteps(detail)" :key="s.label">
            <span v-if="idx" class="flow-sep">→</span>
            <span :class="['flow-node', 'flow-' + s.state]">
              <span class="flow-dot"></span>{{ s.label }}
            </span>
          </template>
        </div>
      </a-card>

      <a-descriptions bordered :column="2" size="small" class="block">
        <a-descriptions-item label="订单编号">{{ detail.orderNo || '—' }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">{{ detail.orderStatus || '—' }}</a-descriptions-item>
        <a-descriptions-item label="委托单编号">
          <span v-if="detail.entrustId" class="link-text" @click="goEntrust">{{ detail.entrustNo || '—' }}</span>
          <span v-else>—</span>
        </a-descriptions-item>
        <a-descriptions-item label="委托单名称">
          <span v-if="detail.entrustId" class="link-text" @click="goEntrust">{{ detail.entrustName || '—' }}</span>
          <span v-else>—</span>
        </a-descriptions-item>
        <a-descriptions-item label="委托单状态">
          <a-tag :color="statusColor(detail.entrustStatus)">{{ detail.entrustStatus || '—' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="监测点位数">{{ detail.pointCount != null ? detail.pointCount : 0 }}</a-descriptions-item>
        <a-descriptions-item label="派单状态">
          <a-tag v-if="detail.dispatchId" color="blue">已派单</a-tag>
          <a-tag v-else color="default">未派单</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="计划开始">{{ detail.planStart || '—' }}</a-descriptions-item>
        <a-descriptions-item label="计划结束">{{ detail.planEnd || '—' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ detail.note || '—' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">负责人 / 组员</a-divider>
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

      <a-divider class="title-divider" orientation="left">车辆</a-divider>
      <a-descriptions bordered :column="1" size="small" v-if="detail.vehicle">
        <a-descriptions-item label="车牌 / 车型">{{ detail.vehicle.plateNo }} {{ detail.vehicle.model || '' }}</a-descriptions-item>
      </a-descriptions>
      <a-empty v-else description="未分配车辆" :image="simpleImage" />

      <a-divider class="title-divider" orientation="left">设备</a-divider>
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
import { ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getDispatchDetail } from '../../../api/ems'

const props = defineProps({
  open: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close'])
const router = useRouter()

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const detail = ref({})
const loading = ref(false)

const insColumns = [
  { title: '编号', dataIndex: 'code', key: 'code' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '校准到期', dataIndex: 'calibDue', key: 'calibDue' }
]

// 派单全周期状态步骤（订单状态链 + 派单状态）
const ORDER_STATUSES = ['待派单', '已派单', '采样中', '已完成']
function statusSteps(d) {
  const cur = d.orderStatus || '待派单'
  const dispatchDone = !!d.dispatchId
  return [
    { label: '订单', state: dispatchDone ? 'done' : 'active' },
    ...ORDER_STATUSES.map((s) => {
      let state = 'todo'
      if (s === cur) state = 'active'
      else if (ORDER_STATUSES.indexOf(s) < ORDER_STATUSES.indexOf(cur)) state = 'done'
      return { label: s, state }
    }),
    { label: '派单', state: dispatchDone ? 'done' : (cur === '待派单' ? 'active' : 'todo') }
  ]
}

function goEntrust() {
  if (detail.value.entrustId) {
    router.push({ path: '/ems/base/entrust', query: { detailId: detail.value.entrustId, tab: 'base' } })
  }
}

function statusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}

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

<style scoped>
.status-flow-card {
  margin-bottom: 16px;
}
.status-flow {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.flow-node {
  display: inline-flex;
  align-items: center;
  padding: 2px 12px;
  border-radius: 12px;
  font-size: 13px;
  border: 1px solid #d9d9d9;
  background: #fafafa;
  color: #8c8c8c;
}
.flow-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  background: #bfbfbf;
}
.flow-sep {
  color: #bfbfbf;
  margin: 0 2px;
}
.flow-done {
  color: #389e0d;
  border-color: #b7eb8f;
  background: #f6ffed;
}
.flow-done .flow-dot {
  background: #52c41a;
}
.flow-active {
  color: #096dd9;
  border-color: #91d5ff;
  background: #e6f7ff;
  font-weight: 600;
}
.flow-active .flow-dot {
  background: #1890ff;
}
.flow-todo {
  color: #8c8c8c;
}
.block {
  margin-bottom: 16px;
}
.link-text {
  color: #2563EB;
  cursor: pointer;
}
.link-text:hover {
  text-decoration: underline;
}
</style>
