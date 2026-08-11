<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button @click="goBack">
        <template #icon><span class="btn-icon">←</span></template>
        返回
      </a-button>
      <span class="page-title">派单详情{{ detail.orderNo ? ' · ' + detail.orderNo : '' }}</span>
    </div>

    <template v-if="loading">
      <a-skeleton active />
    </template>
    <template v-else>
      <a-card class="block" size="small" title="订单信息">
        <a-descriptions bordered :column="2" size="small" :label-style="{ width: '120px' }">
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
          <a-descriptions-item label="计划开始">{{ detail.planStart || '—' }}</a-descriptions-item>
          <a-descriptions-item label="计划结束">{{ detail.planEnd || '—' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detail.note || '—' }}</a-descriptions-item>
        </a-descriptions>
      </a-card>

      <a-card class="block" size="small" title="负责人 / 组员">
        <a-descriptions bordered :column="1" size="small" :label-style="{ width: '120px' }" v-if="detail.lead || (detail.members && detail.members.length)">
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
      </a-card>

      <a-card class="block" size="small" title="车辆">
        <a-descriptions bordered :column="1" size="small" :label-style="{ width: '120px' }" v-if="detail.vehicle">
          <a-descriptions-item label="车牌 / 车型">{{ detail.vehicle.plateNo }} {{ detail.vehicle.model || '' }}</a-descriptions-item>
        </a-descriptions>
        <a-empty v-else description="未分配车辆" :image="simpleImage" />
      </a-card>

      <a-card class="block" size="small" title="设备">
        <a-table
          v-if="detail.instruments && detail.instruments.length"
          :dataSource="detail.instruments"
          :columns="insColumns"
          rowKey="id"
          size="small"
          :pagination="false"
        />
        <a-empty v-else description="未分配设备" :image="simpleImage" />
      </a-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Empty } from 'ant-design-vue'
import { getDispatchDetail } from '../../../api/ems'

const route = useRoute()
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

function goEntrust() {
  if (detail.value.entrustId) {
    router.push({ path: '/ems/base/entrust', query: { detailId: detail.value.entrustId, tab: 'base' } })
  }
}

function statusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}

function goBack() {
  router.back()
}

function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  detail.value = {}
  getDispatchDetail(id).then((res) => {
    detail.value = res.data || res || {}
  }).catch(() => {
    detail.value = {}
  }).finally(() => { loading.value = false })
}

onMounted(load)
</script>

<style scoped>
.page-container {
  padding: 16px;
  background-color: #FFF;
}
.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 10px;
  /* 信息头固定：吸附在滚动视口顶部，其余内容超长时由其下方滚动 */
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f0f4f9;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
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
.btn-icon {
  display: inline-block;
}
</style>
