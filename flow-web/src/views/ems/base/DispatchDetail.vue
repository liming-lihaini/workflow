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
            {{ formatPerson(detail.lead) }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.members && detail.members.length" label="组员">
            <div v-for="m in detail.members" :key="m.userId">
              {{ formatPerson(m) }}
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

      <!-- 操作历史：新建/派单/编辑/完成等全部处置轨迹 -->
      <a-card class="block" size="small" title="操作历史">
        <a-timeline v-if="histories.length">
          <a-timeline-item v-for="h in histories" :key="h.id" :color="historyColor(h.action)">
            <div class="hist-line">
              <a-tag :color="historyColor(h.action)">{{ h.action }}</a-tag>
              <span class="hist-content">{{ h.content }}</span>
            </div>
            <div class="hist-meta">
              {{ h.operatorName ? h.operatorName + (h.operatorId ? '(' + h.operatorId + ')' : '') : (h.operatorId || '系统') }}
              · {{ renderDateTime(h.createTime) }}
            </div>
          </a-timeline-item>
        </a-timeline>
        <a-empty v-else description="暂无操作记录" :image="simpleImage" />
      </a-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Empty } from 'ant-design-vue'
import { getDispatchDetail, getSamplingOrderHistory } from '../../../api/ems'

const route = useRoute()
const router = useRouter()

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const detail = ref({})
const loading = ref(false)
// 操作历史（新建/派单/编辑/完成等处置轨迹，倒序）
const histories = ref([])

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

// 操作历史动作颜色
function historyColor(action) {
  return ({
    '新建': 'blue', '派单': 'orange', '编辑': 'cyan', '完成': 'green'
  })[action] || 'default'
}

/** 日期时间展示：yyyy-MM-dd HH:mm */
function renderDateTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').substring(0, 16)
}

function goBack() {
  router.back()
}

/** 人员展示格式：姓名(账号)-[资质1、资质2]，无资质时省略资质段 */
function formatPerson(p) {
  if (!p) return '-'
  let text = `${p.realName || p.username || '-'}(${p.username || '-'})`
  if (p.qualNames && p.qualNames.length) {
    text += `-资质[${p.qualNames.join('、')}]`
  }
  return text
}

function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  detail.value = {}
  histories.value = []
  getDispatchDetail(id).then((res) => {
    detail.value = res.data || res || {}
  }).catch(() => {
    detail.value = {}
  }).finally(() => { loading.value = false })
  // 操作历史独立加载，失败不影响主信息展示
  getSamplingOrderHistory(id).then((res) => {
    const data = res.data || res
    histories.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => { histories.value = [] })
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
/* 操作历史时间线 */
.hist-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.hist-content {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  word-break: break-all;
}
.hist-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
