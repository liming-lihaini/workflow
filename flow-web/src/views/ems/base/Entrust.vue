<template>
  <div class="page-wrap">
    <!-- 委托列表 -->
    <div v-if="!detailId" class="card-wrap">
      <div class="page-header">
        <span class="page-title">委托管理</span>
        <a-space wrap>
          <a-input-search
            v-model:value="searchText"
            placeholder="搜索委托名称"
            style="width: 220px"
            allow-clear
            @search="loadData"
          />
          <a-select
            v-model:value="formState.status"
            placeholder="委托状态"
            :options="statusOptions"
            style="width: 160px"
            allow-clear
            @change="loadData"
          />
          <a-button v-if="hasPerm('ems:entrust:create')" type="primary" @click="showDrawer()">新建委托</a-button>
          <a-button
            danger
            :disabled="!selectedRowKeys.length"
            @click="handleBatchDelete"
          >批量删除{{ selectedRowKeys.length ? `(${selectedRowKeys.length})` : '' }}</a-button>
        </a-space>
      </div>

      <div class="tbl-box">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100, y: scrollY }"
        :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'entrustName'">
            <span class="title-link" @click="openDetail(record.id)">{{ record.entrustName }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="showDrawer(record)">编辑</span>
            <a-divider type="vertical" />
            <a-popconfirm
              v-if="record.status === '草稿' || record.status === '已退回'"
              title="提交后进入待技术确认？"
              @confirm="handleSubmit(record)"
            >
              <span class="action-link">提交</span>
            </a-popconfirm>
            <a-divider v-if="record.status === '草稿' || record.status === '已退回'" type="vertical" />
            <a-popconfirm
              v-if="record.status === '待技术确认'"
              title="确认技术评审通过并拆单？"
              @confirm="handleTechConfirm(record)"
            >
              <span class="action-link">技术确认</span>
            </a-popconfirm>
            <a-divider v-if="record.status === '待技术确认'" type="vertical" />
            <a-popconfirm
              v-if="record.status === '待技术确认'"
              title="填写退回意见"
              @confirm="handleReject(record)"
            >
              <span class="action-link danger" @click.stop="rejectForm.record = record">退回</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      </div>
    </div>

    <!-- 委托详情（独立界面，主页内打开） -->
    <EntrustDetail v-else :id="detailId" @close="closeDetail" />

    <!-- 新建/编辑抽屉 -->

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="editingRecord ? '编辑委托' : '新建委托'"
      width="1200"
      :confirm-loading="submitLoading"
      @close="drawerVisible = false"
    >
      <a-form :model="formState" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="委托名称" required>
              <a-input v-model:value="formState.entrustName" placeholder="请输入委托名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户">
              <a-select v-model:value="formState.custId" show-search placeholder="选择客户" :options="customerOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="来源">
              <a-select v-model:value="formState.source" placeholder="选择来源" :options="sourceOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-input :value="editingRecord ? editingRecord.status : '草稿'" disabled />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="委托说明">
          <div class="editor-toolbar">
            <a-space size="small">
              <a-button size="small" @click="execCmd('bold')"><b>B</b></a-button>
              <a-button size="small" @click="execCmd('italic')"><i>I</i></a-button>
              <a-button size="small" @click="execCmd('underline')"><u>U</u></a-button>
              <a-button size="small" @click="execCmd('insertUnorderedList')">• 列表</a-button>
            </a-space>
          </div>
          <div
            ref="editorRef"
            class="rich-editor"
            contenteditable="true"
            placeholder="请输入委托说明（支持富文本）"
            @input="onEditorInput"
          ></div>
        </a-form-item>

        <a-divider orientation="left">监测点位（委托基础信息）</a-divider>
        <div class="point-toolbar">
          <a-button size="small" type="dashed" @click="addPoint">+ 添加点位</a-button>
        </div>
        <a-table
          :columns="pointColumns"
          :data-source="points"
          :pagination="false"
          size="small"
          row-key="rowKey"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'pointName'">
              <a-input v-model:value="record.pointName" placeholder="点位名称" />
            </template>
            <template v-else-if="column.key === 'longitude'">
              <a-input v-model:value="record.lng" placeholder="经度" />
            </template>
            <template v-else-if="column.key === 'latitude'">
              <a-input v-model:value="record.lat" placeholder="纬度" />
            </template>
            <template v-else-if="column.key === 'pointType'">
              <a-select v-model:value="record.pointType" placeholder="类型" :options="pointTypeOptions" style="width: 110px" />
            </template>
            <template v-else-if="column.key === 'factors'">
              <a-select
                v-model:value="record.factors"
                mode="multiple"
                placeholder="监测因子"
                :options="factorOptions"
                style="width: 190px"
              />
            </template>
            <template v-else-if="column.key === 'standard'">
              <a-select
                v-model:value="record.standardCode"
                placeholder="执行标准"
                :options="standardOptions"
                style="width: 190px"
                @change="(val) => { const o = standardOptions.find(s => s.value === val); record.standardName = o ? o.name : '' }"
              />
            </template>
            <template v-else-if="column.key === 'freq'">
              <a-select v-model:value="record.freq" placeholder="频次" :options="freqOptions" style="width: 120px" />
            </template>
            <template v-else-if="column.key === 'envCondition'">
              <a-textarea v-model:value="record.condition" placeholder="工况要求" :rows="1" auto-size />
            </template>
            <template v-else-if="column.key === 'op'">
              <a-popconfirm title="删除该点位？" @confirm="removePoint(index)">
                <span class="action-link danger">删除</span>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="drawerVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitLoading" @click="handleSave">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 退回意见弹窗 -->
    <a-modal v-model:open="rejectVisible" title="退回委托" @ok="confirmReject" ok-text="确认退回">
      <a-textarea v-model:value="rejectForm.opinion" placeholder="必填退回意见(BR-023-06)" :rows="3" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  getEntrusts, getEntrust, saveEntrust, submitEntrust, techConfirmEntrust, rejectEntrust, batchDeleteEntrusts, getCustomers, getDictItems
} from '../../../api/ems'
import EntrustDetail from './EntrustDetail.vue'
import { usePermission } from '../../../composables/usePermission'
import { useResizableColumns } from '../../../composables/useResizableTable'

const { hasPerm } = usePermission()
const loading = ref(false)
const dataList = ref([])
const drawerVisible = ref(false)
const submitLoading = ref(false)
const editingRecord = ref(null)
const searchText = ref('')
const customerOptions = ref([])
const sourceOptions = ref([])
const pointTypeOptions = ref([])
const factorOptions = ref([])
const standardOptions = ref([])
const freqOptions = ref([])

const rejectVisible = ref(false)
const rejectForm = reactive({ record: null, opinion: '' })

const selectedRowKeys = ref([])
const scrollY = ref(420)
function onSelectChange(keys) {
  selectedRowKeys.value = keys
}
function syncTableHeight() {
  const box = document.querySelector('.page-wrap .tbl-box')
  if (!box) return
  const boxRect = box.getBoundingClientRect()
  const headerEl = box.querySelector('.ant-table-thead')
  const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40
  const pagEl = box.querySelector('.ant-table-pagination')
  let reservedBottom = 0
  if (pagEl) {
    const pagRect = pagEl.getBoundingClientRect()
    reservedBottom = boxRect.bottom - pagRect.top
  }
  if (!reservedBottom) reservedBottom = 80
  const h = boxRect.height - headerH - reservedBottom - 4
  scrollY.value = h > 200 ? Math.floor(h) : 200
}
let pageObserver = null

function handleBatchDelete() {
  if (!selectedRowKeys.value.length) return
  Modal.confirm({
    title: `确认删除选中的 ${selectedRowKeys.value.length} 条委托？`,
    content: '仅草稿/已退回状态可被删除，关联监测点位将一并清除。',
    okText: '删除',
    okType: 'danger',
    onOk: () => {
      const ids = [...selectedRowKeys.value]
      batchDeleteEntrusts(ids)
        .then(() => {
          message.success(`已删除 ${ids.length} 条`)
          selectedRowKeys.value = []
          loadData()
        })
        .catch(() => {})
    }
  })
}

const editorRef = ref(null)
const points = ref([])
const detailId = ref(null)  // 非空时主页展示详情界面

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` })

const { columns } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '委托名称', dataIndex: 'entrustName', key: 'entrustName', sorter: true },
  { title: '客户', dataIndex: 'custName', key: 'custName', width: 240 },
  { title: '来源', dataIndex: 'sourceName', key: 'sourceName', width: 110 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: ({ text }) => renderDate(text) },
  { title: '操作', key: 'action', width: 220 }
])

const pointColumns = [
  { title: '点位名称', key: 'pointName', width: 140 },
  { title: '经度', key: 'longitude', width: 100 },
  { title: '纬度', key: 'latitude', width: 100 },
  { title: '介质类型', key: 'pointType', width: 120 },
  { title: '监测因子', key: 'factors', width: 200 },
  { title: '执行标准', key: 'standard', width: 200 },
  { title: '监测频次', key: 'freq', width: 130 },
  { title: '备注(工况要求)', key: 'envCondition', width: 160 },
  { title: '操作', key: 'op', width: 70 }
]

const formState = reactive({ id: undefined, entrustName: '', custId: undefined, source: undefined, status: undefined })
const statusOptions = ref([])

function renderDate(v) {
  if (!v) return '-'
  // 后端返回 yyyy-MM-ddTHH:mm:ss[.nnn]
  const d = new Date(v.replace(' ', 'T'))
  if (isNaN(d.getTime())) return v
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function statusColor(s) {
  return { '草稿': 'default', '待技术确认': 'orange', '已确认': 'green', '已退回': 'red' }[s] || 'default'
}

function loadCustomers() {
  getCustomers({}).then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    customerOptions.value = list.map((c) => ({ label: c.custName, value: c.id }))
  }).catch(() => {})
}

function loadDicts() {
  // 来源下拉（moni_entrust_source）
  getDictItems('moni_entrust_source').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      sourceOptions.value = list.map((i) => ({ label: i.itemText, value: i.itemValue }))
    }
  }).catch(() => {})
  // 点位类型下拉（moni_point_type）
  getDictItems('moni_point_type').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      pointTypeOptions.value = list.map((i) => ({ label: i.itemText, value: i.itemValue }))
    }
  }).catch(() => {})
  // 监测因子（moni_monitor_factor）
  getDictItems('moni_monitor_factor').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      factorOptions.value = list.map((i) => ({ label: i.itemText, value: i.itemValue }))
    }
  }).catch(() => {})
  // 执行标准（moni_exec_standard）：label 显示"编号 全称"，额外保存 name
  getDictItems('moni_exec_standard').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      standardOptions.value = list.map((i) => ({
        label: `${i.itemValue} ${i.itemText}`,
        value: i.itemValue,
        name: i.itemText
      }))
    }
  }).catch(() => {})
  // 监测频次（moni_monitor_freq）
  getDictItems('moni_monitor_freq').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      freqOptions.value = list.map((i) => ({ label: i.itemText, value: i.itemValue }))
    }
  }).catch(() => {})
  // 委托状态（moni_entrust_status）：状态值为中文文本，value 用 itemText
  getDictItems('moni_entrust_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      statusOptions.value = list.map((i) => ({ label: i.itemText, value: i.itemText }))
    }
  }).catch(() => {})
}

function loadData() {
  loading.value = true
  getEntrusts({ page: pagination.current, size: pagination.pageSize, keyword: searchText.value || undefined, status: formState.status })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.list || [])
      dataList.value = list
      pagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { loading.value = false; nextTick(syncTableHeight) })
}

function addPoint() {
  points.value.push({
    rowKey: `rp_${Date.now()}_${points.value.length}`,
    pointNo: '', pointName: '', lng: '', lat: '', pointType: undefined,
    factors: [], standardCode: undefined, standardName: '', freq: undefined, condition: ''
  })
}

function removePoint(index) {
  points.value.splice(index, 1)
}

function setEditorHtml(html) {
  if (editorRef.value) editorRef.value.innerHTML = html || ''
}
function getEditorHtml() {
  return editorRef.value ? editorRef.value.innerHTML : ''
}
function onEditorInput() {
  formState.description = getEditorHtml()
}
function execCmd(cmd) {
  document.execCommand(cmd, false, null)
  editorRef.value && editorRef.value.focus()
  formState.description = getEditorHtml()
}

function openDetail(id) {
  detailId.value = id
}
function closeDetail() {
  detailId.value = null
  loadData()  // 返回列表时刷新（状态可能已变更）
}

function showDrawer(record) {
  if (record) {
    editingRecord.value = record
    loadEntrustDetail(record.id)
  } else {
    editingRecord.value = null
    Object.assign(formState, { id: undefined, entrustName: '', custId: undefined, source: undefined, description: '' })
    setEditorHtml('')
    points.value = []
    drawerVisible.value = true
  }
}

function loadEntrustDetail(id) {
  getEntrust(id).then((res) => {
    const vo = res.data || res
    Object.assign(formState, {
      id: vo.id,
      entrustName: vo.entrustName,
      custId: vo.custId,
      source: vo.source,
      description: vo.description || ''
    })
    setEditorHtml(vo.description || '')
    points.value = (vo.points || []).map((p) => ({
      rowKey: `rp_${p.id || Date.now()}_${Math.random()}`,
      id: p.id,
      pointNo: p.pointNo || '',
      pointName: p.pointName || '',
      lng: p.lng ?? '',
      lat: p.lat ?? '',
      pointType: p.pointType || undefined,
      factors: p.factors ? String(p.factors).split(',') : [],
      standardCode: p.standardCode || undefined,
      standardName: p.standardName || '',
      freq: p.freq || undefined,
      condition: p.condition || ''
    }))
    drawerVisible.value = true
  }).catch(() => { message.error('加载委托详情失败') })
}

function handleSave() {
  if (!formState.entrustName) {
    message.warning('请填写委托名称')
    return
  }
  submitLoading.value = true
  const entrust = {
    id: formState.id,
    entrustName: formState.entrustName,
    custId: formState.custId,
    source: formState.source,
    description: getEditorHtml(),
    status: editingRecord.value ? editingRecord.value.status : '草稿'
  }
  const payload = {
    entrust,
    points: points.value.map((p) => ({
      id: p.id,
      pointNo: p.pointNo,
      pointName: p.pointName,
      lng: p.lng,
      lat: p.lat,
      pointType: p.pointType,
      factors: Array.isArray(p.factors) ? p.factors.join(',') : (p.factors || ''),
      standardCode: p.standardCode,
      standardName: p.standardName,
      freq: p.freq,
      condition: p.condition
    }))
  }
  saveEntrust(payload)
    .then(() => {
      message.success('保存成功')
      drawerVisible.value = false
      loadData()
    })
    .catch(() => {})
    .finally(() => { submitLoading.value = false })
}

function handleSubmit(record) {
  submitEntrust(record.id, 'current-user').then(() => {
    message.success('已提交，待技术确认')
    loadData()
  }).catch(() => {})
}

function handleTechConfirm(record) {
  techConfirmEntrust(record.id, 1, '方法适用、能力满足').then(() => {
    message.success('技术确认通过，已拆单生成采样订单')
    loadData()
  }).catch(() => {})
}

function handleReject(record) {
  rejectForm.record = record
  rejectForm.opinion = ''
  rejectVisible.value = true
}

function confirmReject() {
  if (!rejectForm.opinion) {
    message.warning('退回必填意见')
    return
  }
  rejectEntrust(rejectForm.record.id, 1, rejectForm.opinion).then(() => {
    message.success('已退回')
    rejectVisible.value = false
    loadData()
  }).catch(() => {})
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

onMounted(() => {
  loadCustomers()
  loadDicts()
  loadData()
  nextTick(() => {
    syncTableHeight()
    const wrap = document.querySelector('.page-wrap')
    if (wrap && 'ResizeObserver' in window) {
      pageObserver = new ResizeObserver(() => syncTableHeight())
      pageObserver.observe(wrap)
    }
    window.addEventListener('resize', syncTableHeight)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', syncTableHeight)
  if (pageObserver) pageObserver.disconnect()
})
</script>

<style scoped>
.page-wrap {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
  box-sizing: border-box;
}
.card-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-sizing: border-box;
  overflow: hidden;
}
.page-wrap :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tbl-box {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-table-wrapper) {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-spin-nested-loading) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.tbl-box :deep(.ant-table) {
  flex: 1;
  min-height: 0;
}
.tbl-box :deep(.ant-table-pagination) {
  margin: 8px 0 16px !important;
  flex: 0 0 auto;
}
.rich-editor {
  min-height: 120px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 8px 12px;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
}
.rich-editor:focus {
  border-color: #4096ff;
  box-shadow: 0 0 0 2px rgba(5, 145, 255, 0.1);
}
.rich-editor:empty::before {
  content: attr(placeholder);
  color: #bfbfbf;
}
.editor-toolbar {
  margin-bottom: 6px;
}
.point-toolbar {
  margin-bottom: 8px;
}
.title-link {
  color: #1677ff;
  cursor: pointer;
}
.title-link:hover {
  text-decoration: underline;
}
</style>
