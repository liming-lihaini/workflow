<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button @click="emit('close')">
        <template #icon><span class="btn-icon">←</span></template>
        返回
      </a-button>
      <span class="page-title">{{ vo.entrustName || '委托详情' }}<span class="page-sub">{{ vo.entrustNo }}</span></span>
    </div>

    <a-card title="基本信息" size="small" class="block">
      <a-descriptions :column="2" bordered size="small" :label-style="{ width: '120px' }">
        <a-descriptions-item label="委托名称">
          <a-tag v-if="vo.urgent" color="red" style="margin-right: 4px">紧急</a-tag>{{ vo.entrustName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="委托编号">{{ vo.entrustNo || '系统自动生成' }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ vo.custName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ vo.sourceName || vo.source || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ vo.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ (vo.createName || vo.createBy) ? (vo.createName || '') + (vo.createBy ? '(' + vo.createBy + ')' : '') : '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ renderDate(vo.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新人">{{ (vo.updateName || vo.updateBy) ? (vo.updateName || '') + (vo.updateBy ? '(' + vo.updateBy + ')' : '') : '-' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ renderDate(vo.updateTime) }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">委托说明</a-divider>
      <div class="rich-view" v-html="vo.description || '<span style=\'color:#bfbfbf\'>暂无说明</span>'"></div>

      <a-divider class="title-divider" orientation="left">委托附件</a-divider>
      <div v-if="attachments.length">
        <a-list :data-source="attachments" size="small" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <a :href="attachmentUrl(item)" target="_blank" rel="noopener" class="link-text">{{ item.fileName }}</a>
                </template>
                <template #description>
                  <span v-if="item.size">{{ (item.size / 1024).toFixed(1) }} KB</span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </div>
      <div v-else class="empty-tip">暂无附件</div>
    </a-card>

    <a-card title="监测点位（委托基础信息）" size="small" class="block">
      <a-table
        :columns="pointColumns"
        :data-source="vo.points || []"
        :pagination="false"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'seq'">{{ (vo.points || []).indexOf(record) + 1 }}</template>
          <template v-else-if="column.key === 'standard'">{{ record.standardCode || '' }}<span v-if="record.standardName"> {{ record.standardName }}</span></template>
        </template>
      </a-table>
    </a-card>

    <a-card
      v-if="dispatchOrders.length"
      title="委托派单"
      size="small"
      class="block"
    >
      <a-table
        :columns="dispatchColumns"
        :data-source="dispatchOrders"
        :pagination="false"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'orderNo'">
            <span class="link-text" @click="goDispatch(record)">{{ record.orderNo }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'planRange'">
            {{ (record.planStart || '') + (record.planEnd ? ' ~ ' + record.planEnd : '') }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ renderDate(record.createTime) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getEntrust, getSamplingOrders, getFiles } from '../../../api/ems'

const props = defineProps({ id: { type: [Number, String], required: true } })
const emit = defineEmits(['close'])
const router = useRouter()

const vo = ref({})
const dispatchOrders = ref([])
const attachments = ref([])

function attachmentUrl(file) {
  // 后端下载接口：GET /api/v1/attachments/download?path=&name=
  return `/api/v1/attachments/download?path=${encodeURIComponent(file.filePath)}&name=${encodeURIComponent(file.fileName)}`
}

function goDispatch(order) {
  router.push({ path: '/ems/base/dispatch-detail', query: { id: order.id } })
}
const pointColumns = [
  { title: '#', key: 'seq', width: 50 },
  { title: '点位编号', dataIndex: 'pointNo', key: 'pointNo', width: 120 },
  { title: '点位名称', dataIndex: 'pointName', key: 'pointName', width: 140 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 100 },
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 100 },
  { title: '介质类型', dataIndex: 'pointType', key: 'pointType', width: 120 },
  { title: '监测因子', dataIndex: 'factors', key: 'factors', width: 200 },
  { title: '执行标准(编号+全称)', key: 'standard', width: 220 },
  { title: '备注(工况要求)', dataIndex: 'condition', key: 'condition', width: 160 }
]

function statusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}

function renderDate(v) {
  if (!v) return '-'
  // 后端返回 yyyy-MM-ddTHH:mm:ss[.nnn]，直接取前 10 位（年月日），避免高精度小数导致 Date 解析失败
  const str = String(v)
  if (str.length >= 10) return str.substring(0, 10)
  return str
}

const dispatchColumns = [
  { title: '订单号', key: 'orderNo', width: 140 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '负责人', dataIndex: 'leadName', key: 'leadName', width: 90 },
  { title: '采样人', dataIndex: 'samplerNames', key: 'samplerNames', width: 120 },
  { title: '计划区间', key: 'planRange', width: 200 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 }
]

function loadDispatchOrders() {
  getSamplingOrders({ entrustId: props.id }).then((res) => {
    dispatchOrders.value = res.data || res || []
  }).catch(() => {})
}

function loadAttachments() {
  getFiles({ bizType: 'entrust', bizId: props.id }).then((res) => {
    const list = res.data || res || []
    attachments.value = list.map((f) => ({
      id: f.id,
      fileName: f.fileName,
      filePath: f.filePath,
      size: f.size
    }))
  }).catch(() => {})
}

onMounted(() => {
  getEntrust(props.id).then((res) => {
    vo.value = res.data || res
  }).catch(() => {})
  loadDispatchOrders()
  loadAttachments()
})
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
  padding: 16px 0;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.page-sub {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 400;
  color: #8c8c8c;
}
.btn-icon {
  display: inline-block;
}
.block {
  margin-bottom: 16px;
}
.rich-view {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 8px 12px;
  min-height: 60px;
  font-size: 14px;
  line-height: 1.6;
}
.empty-tip {
  color: #bfbfbf;
  font-size: 13px;
  padding: 8px 0;
}
.link-text {
  color: #2563EB;
  cursor: pointer;
}
.link-text:hover {
  text-decoration: underline;
}
</style>
