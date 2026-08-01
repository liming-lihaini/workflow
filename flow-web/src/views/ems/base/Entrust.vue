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
          <a-button v-if="hasPerm('ems:entrust:create')" type="primary" @click="showDrawer()">新建委托</a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1100 }"
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

    <!-- 委托详情（独立界面，主页内打开） -->
    <EntrustDetail v-else :id="detailId" @close="closeDetail" />

    <!-- 新建/编辑抽屉 -->

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="editingRecord ? '编辑委托' : '新建委托'"
      width="1000"
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
            <template v-if="column.key === 'seq'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'pointNo'">
              <a-input v-model:value="record.pointNo" placeholder="点位编号" />
            </template>
            <template v-else-if="column.key === 'pointName'">
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
            <template v-else-if="column.key === 'envCondition'">
              <a-textarea v-model:value="record.condition" placeholder="环境说明" :rows="1" auto-size />
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getEntrusts, getEntrust, saveEntrust, submitEntrust, techConfirmEntrust, rejectEntrust, getCustomers, getDictItems
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

const rejectVisible = ref(false)
const rejectForm = reactive({ record: null, opinion: '' })

const editorRef = ref(null)
const points = ref([])
const detailId = ref(null)  // 非空时主页展示详情界面

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` })

const { columns } = useResizableColumns([
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60, sorter: true },
  { title: '委托名称', dataIndex: 'entrustName', key: 'entrustName', sorter: true },
  { title: '客户', dataIndex: 'custName', key: 'custName', width: 140 },
  { title: '来源', dataIndex: 'sourceName', key: 'sourceName', width: 110 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120, customRender: ({ text }) => renderDate(text) },
  { title: '操作', key: 'action', width: 240 }
])

const pointColumns = [
  { title: '#', key: 'seq', width: 50 },
  { title: '点位编号', key: 'pointNo', width: 130 },
  { title: '点位名称', key: 'pointName', width: 150 },
  { title: '经度', key: 'longitude', width: 110 },
  { title: '纬度', key: 'latitude', width: 110 },
  { title: '类型', key: 'pointType', width: 120 },
  { title: '环境说明', key: 'envCondition', width: 160 },
  { title: '操作', key: 'op', width: 70 }
]

const formState = reactive({ id: undefined, entrustName: '', custId: undefined, source: undefined })

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
}

function loadData() {
  loading.value = true
  getEntrusts({ page: pagination.current, size: pagination.pageSize, keyword: searchText.value || undefined })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.list || [])
      dataList.value = list
      pagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function addPoint() {
  points.value.push({
    rowKey: `rp_${Date.now()}_${points.value.length}`,
    pointNo: '', pointName: '', lng: '', lat: '', pointType: undefined, condition: ''
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
})
</script>

<style scoped>
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
