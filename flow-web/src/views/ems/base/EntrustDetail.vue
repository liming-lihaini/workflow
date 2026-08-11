<template>
  <div class="page-container">
    <div class="page-toolbar">
      <a-button @click="emit('close')" size="small">
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
        <a-descriptions-item label="开始日期">{{ vo.startDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ vo.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建人">{{ (vo.createName || vo.createBy) ? (vo.createName || '') + (vo.createBy ? '(' + vo.createBy + ')' : '') : '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ renderDate(vo.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新人">{{ (vo.updateName || vo.updateBy) ? (vo.updateName || '') + (vo.updateBy ? '(' + vo.updateBy + ')' : '') : '-' }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ renderDate(vo.updateTime) }}</a-descriptions-item>
      </a-descriptions>

      <a-divider class="title-divider" orientation="left">委托说明</a-divider>
      <div class="rich-view" v-html="vo.description || '<span style=\'color:#bfbfbf\'>暂无说明</span>'"></div>

      <a-divider class="title-divider" orientation="left">委托附件</a-divider>
      <div v-if="attachments.length" class="attach-grid">
        <div v-for="item in attachments" :key="item.id" class="attach-card">
          <div class="attach-icon" :class="'icon-' + fileKind(item.fileName)">
            <component :is="fileIcon(item.fileName)" />
          </div>
          <div class="attach-meta">
            <div class="attach-name" :title="item.fileName">{{ item.fileName }}</div>
            <div class="attach-size">{{ formatSize(item.size) }}</div>
          </div>
          <a class="attach-download" :href="attachmentUrl(item)" target="_blank" rel="noopener" title="下载">
            <DownloadOutlined />
          </a>
        </div>
      </div>
      <div v-else class="empty-tip">暂无附件</div>
    </a-card>

    <a-card title="监测点位" size="small" class="block">
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

    <!-- 关联流程信息：委托申请流程（Webhook 创建时写入 processInstanceId） -->
    <a-card v-if="vo.processInstanceId" title="关联流程信息" size="small" class="block">
      <a-descriptions v-if="procInfo" :column="2" bordered size="small" :label-style="{ width: '120px' }">
        <a-descriptions-item label="流程名称">{{ procInfo.processName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="流程编号">{{ procInfo.instanceNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="流程状态">
          <a-tag :color="procStatusColor(procInfo.status)">{{ procInfo.statusDesc || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="发起人">{{ procInfo.startUser || '-' }}</a-descriptions-item>
        <a-descriptions-item label="发起时间">{{ renderDateTime(procInfo.startTime) }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ renderDateTime(procInfo.endTime) }}</a-descriptions-item>
        <a-descriptions-item label="当前节点">{{ procInfo.currentNodeName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="当前办理人">{{ procInfo.currentAssignee || '-' }}</a-descriptions-item>
      </a-descriptions>
      <a-divider class="title-divider" orientation="left">办理记录</a-divider>
      <a-timeline v-if="procTasks.length">
        <a-timeline-item v-for="t in procTasks" :key="t.id" :color="taskTimelineColor(t)">
          <div class="hist-line">
            <span class="hist-action">{{ t.nodeName || t.nodeId || '-' }}</span>
            <a-tag v-if="t.taskActionDesc">{{ t.taskActionDesc }}</a-tag>
            <a-tag :color="taskTagColor(t.status)">{{ t.statusDesc || '-' }}</a-tag>
          </div>
          <div class="hist-meta">
            {{ t.assignee || t.candidateUsers || '未指派' }} · 完成时间：{{ renderDateTime(t.completeTime) }}
          </div>
        </a-timeline-item>
      </a-timeline>
      <div v-else class="empty-tip">暂无办理记录</div>
    </a-card>

    <!-- 操作记录：委托的新建/编辑/提交/技术确认/退回/收样等处置轨迹 -->
    <a-card title="操作记录" size="small" class="block">
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
      <div v-else class="empty-tip">暂无操作记录</div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileImageOutlined, FilePdfOutlined, FileExcelOutlined, FileWordOutlined,
  FileZipOutlined, FileOutlined, DownloadOutlined
} from '@ant-design/icons-vue'
import { getEntrust, getSamplingOrders, getFiles, getEntrustHistory } from '../../../api/ems'
import { getProcessInstance } from '../../../api/process'
import { getTasksByInstance } from '../../../api/task'

const props = defineProps({ id: { type: [Number, String], required: true } })
const emit = defineEmits(['close'])
const router = useRouter()

const vo = ref({})
const dispatchOrders = ref([])
const attachments = ref([])
// 操作记录（委托处置轨迹）与关联流程（委托申请流程实例 + 办理任务）
const histories = ref([])
const procInfo = ref(null)
const procTasks = ref([])

function attachmentUrl(file) {
  // 后端下载接口：GET /api/v1/attachments/download?path=&name=
  return `/api/v1/attachments/download?path=${encodeURIComponent(file.filePath)}&name=${encodeURIComponent(file.fileName)}`
}

const FILE_KINDS = {
  image: { exts: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'], icon: FileImageOutlined },
  pdf: { exts: ['pdf'], icon: FilePdfOutlined },
  excel: { exts: ['xls', 'xlsx', 'csv'], icon: FileExcelOutlined },
  word: { exts: ['doc', 'docx'], icon: FileWordOutlined },
  zip: { exts: ['zip', 'rar', '7z', 'tar', 'gz'], icon: FileZipOutlined }
}

function fileKind(name) {
  const ext = String(name || '').split('.').pop().toLowerCase()
  return Object.keys(FILE_KINDS).find((k) => FILE_KINDS[k].exts.includes(ext)) || 'other'
}

function fileIcon(name) {
  return FILE_KINDS[fileKind(name)]?.icon || FileOutlined
}

function formatSize(size) {
  if (!size && size !== 0) return ''
  if (size >= 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + ' MB'
  return (size / 1024).toFixed(1) + ' KB'
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

/** 日期时间展示：yyyy-MM-dd HH:mm */
function renderDateTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').substring(0, 16)
}

// 操作记录动作颜色
function historyColor(action) {
  return ({
    '新建': 'blue', '编辑': 'cyan', '提交': 'orange',
    '技术确认': 'green', '退回': 'red', '收样': 'purple', '删除': 'default'
  })[action] || 'default'
}

// 流程状态颜色（0-运行中/1-已完成/2-已终止等，按描述兜底）
function procStatusColor(status) {
  return ({ 0: 'blue', 1: 'green', 2: 'red', 3: 'orange' })[status] || 'default'
}

// 办理任务：时间线节点颜色与状态标签颜色（status：0-待处理，1-处理中，2-已完成）
function taskTimelineColor(t) {
  if (t.status === 2) return 'green'
  if (t.status === 1) return 'blue'
  return 'gray'
}
function taskTagColor(status) {
  return ({ 0: 'default', 1: 'blue', 2: 'green' })[status] || 'default'
}

function loadHistories() {
  getEntrustHistory(props.id).then((res) => {
    histories.value = res.data || res || []
  }).catch(() => {})
}

/** 关联流程：按委托的 processInstanceId 拉取实例信息与办理任务 */
function loadRelatedProcess(processInstanceId) {
  if (!processInstanceId) return
  getProcessInstance(processInstanceId).then((res) => {
    procInfo.value = res.data || res
  }).catch(() => {})
  getTasksByInstance(processInstanceId).then((res) => {
    procTasks.value = res.data || res || []
  }).catch(() => {})
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
    // 委托详情返回后按来源流程实例加载关联流程信息
    loadRelatedProcess(vo.value.processInstanceId)
  }).catch(() => {})
  loadDispatchOrders()
  loadAttachments()
  loadHistories()
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
/* 操作记录 / 办理记录时间线 */
.hist-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.hist-action {
  font-weight: 500;
  font-size: 13px;
}
.hist-content {
  font-size: 13px;
  color: #262626;
}
.hist-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #8c8c8c;
}
.link-text {
  color: #2563EB;
  cursor: pointer;
}
.link-text:hover {
  text-decoration: underline;
}
/* 委托附件卡片 */
.attach-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.attach-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.2s ease;
}
.attach-card:hover {
  border-color: #91caff;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.attach-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 20px;
}
.icon-image { color: #1677ff; background: #e6f4ff; }
.icon-pdf { color: #f5222d; background: #fff1f0; }
.icon-excel { color: #52c41a; background: #f6ffed; }
.icon-word { color: #2f54eb; background: #f0f5ff; }
.icon-zip { color: #fa8c16; background: #fff7e6; }
.icon-other { color: #8c8c8c; background: #f5f5f5; }
.attach-meta {
  flex: 1;
  min-width: 0;
}
.attach-name {
  font-size: 13px;
  font-weight: 500;
  color: #262626;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.attach-size {
  margin-top: 2px;
  font-size: 12px;
  color: #8c8c8c;
}
.attach-download {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: #8c8c8c;
  font-size: 14px;
  transition: all 0.2s ease;
}
.attach-download:hover {
  color: #1677ff;
  background: #e6f4ff;
}
</style>
