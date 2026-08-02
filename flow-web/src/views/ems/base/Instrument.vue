<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">设备台账（仪器设备全生命周期）</span>
        <a-space wrap>
          <a-input-search v-model:value="kw" placeholder="搜索编号/名称/型号" style="width: 220px" allow-clear @search="load" />
          <a-select v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 120px" :options="statusFilterOptions" @change="load" />
          <a-button type="primary" @click="showDrawer()">新增设备</a-button>
        </a-space>
      </div>
      <a-alert
        v-if="expiring.length"
        type="warning"
        show-icon
        style="margin-bottom: 12px"
        :message="`校准预警：${expiring.length} 台设备临近到期或已停用`"
      />
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <span class="code-link" @click="openDetail(record)">{{ record.code }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="instStatusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-if="column.key === 'calibDue'">
            <span :style="{ color: isExpiring(record) ? '#fa8c16' : 'inherit' }">{{ record.calibDue || '-' }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="showDrawer(record)">编辑</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="showCalibrate(record)">校准登记</span>
            <a-divider type="vertical" />
            <a-popconfirm title="删除该设备？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑设备 -->
    <a-drawer v-model:open="visible" :title="editing ? '编辑设备' : '新增设备'" width="1000" @close="visible = false">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="仪器编号"><a-input v-model:value="form.code" placeholder="YQ001" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="仪器名称" required><a-input v-model:value="form.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="型号"><a-input v-model:value="form.model" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="生产厂商"><a-input v-model:value="form.manufacturer" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="购置日期"><a-date-picker v-model:value="form.purchaseDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准到期日"><a-date-picker v-model:value="form.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="form.status" :options="statusOptions" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准证书号"><a-input v-model:value="form.certNo" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="2" /></a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="visible = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSave">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 校准登记 -->
    <a-modal v-model:open="calibVisible" title="校准登记" @ok="submitCalibrate" ok-text="登记">
      <a-form layout="vertical">
        <a-form-item label="校准日期"><a-date-picker v-model:value="calibForm.calibDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="下次校准到期日" required><a-date-picker v-model:value="calibForm.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="校准证书编号"><a-input v-model:value="calibForm.certNo" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 设备详情 -->
    <InstrumentDetail :open="detailVisible" :instrument-id="detailId" @close="detailVisible = false" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listInstruments, createInstrument, updateInstrument, deleteInstrument, calibrateInstrument, expiringInstruments, getDictItems } from '../../../api/ems'
import InstrumentDetail from './InstrumentDetail.vue'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const visible = ref(false)
const calibVisible = ref(false)
const editing = ref(null)
const detailVisible = ref(false)
const detailId = ref(null)
const kw = ref('')
const statusFilter = ref(undefined)
const dataList = ref([])
const expiring = ref([])
const statusOptions = ref([])
const statusFilterOptions = [
  { label: '在用', value: '在用' }, { label: '临期', value: '临期' },
  { label: '停用', value: '停用' }, { label: '维修', value: '维修' }, { label: '报废', value: '报废' }
]
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '编号', dataIndex: 'code', key: 'code', width: 100 },
  { title: '名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '厂商', dataIndex: 'manufacturer', key: 'manufacturer' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '校准到期', key: 'calibDue', dataIndex: 'calibDue', width: 120 },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action', width: 240 }
]

const form = reactive({ id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
const calibForm = reactive({ id: undefined, calibDate: null, calibDue: null, certNo: '' })

function instStatusColor(s) {
  return { '在用': 'green', '临期': 'orange', '停用': 'red', '维修': 'blue', '报废': 'default' }[s] || 'default'
}
function isExpiring(r) {
  if (r.status === '停用' || r.status === '临期') return true
  if (r.calibDue) {
    const diff = dayjs(r.calibDue).diff(dayjs(), 'day')
    return diff <= 30 && diff >= 0
  }
  return false
}

function loadDict() {
  getDictItems('moni_instrument_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    // 字典未初始化时使用内置五态
    statusOptions.value = list.length
      ? list.map((i) => ({ label: i.itemText, value: i.itemText }))
      : statusFilterOptions
  }).catch(() => { statusOptions.value = statusFilterOptions })
}

function load() {
  loading.value = true
  listInstruments({ keyword: kw.value || undefined, status: statusFilter.value, page: pagination.current, size: pagination.pageSize })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.records || data.list || [])
      dataList.value = list
      pagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
  loadExpiring()
}

function loadExpiring() {
  expiringInstruments().then((res) => {
    const data = res.data || res
    expiring.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {})
}

function openDetail(record) {
  detailId.value = record.id
  detailVisible.value = true
}

function showDrawer(record) {
  if (record) {
    editing.value = record
    Object.assign(form, {
      id: record.id, code: record.code, name: record.name, model: record.model,
      manufacturer: record.manufacturer, purchaseDate: record.purchaseDate,
      calibDue: record.calibDue, status: record.status, certNo: record.certNo, remark: record.remark
    })
  } else {
    editing.value = null
    Object.assign(form, { id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
  }
  visible.value = true
}

function handleSave() {
  if (!form.name) { message.warning('请填写仪器名称'); return }
  submitting.value = true
  const api = editing.value ? updateInstrument(form.id, form) : createInstrument(form)
  api.then(() => { message.success('保存成功'); visible.value = false; load() })
    .catch(() => {}).finally(() => { submitting.value = false })
}

function showCalibrate(record) {
  Object.assign(calibForm, { id: record.id, calibDate: dayjs().format('YYYY-MM-DD'), calibDue: null, certNo: record.certNo })
  calibVisible.value = true
}

function submitCalibrate() {
  if (!calibForm.calibDue) { message.warning('请填写下次校准到期日'); return }
  calibrateInstrument(calibForm.id, {
    calibDate: calibForm.calibDate, calibDue: calibForm.calibDue, certNo: calibForm.certNo
  }).then(() => { message.success('校准登记成功，状态已重算'); calibVisible.value = false; load() })
    .catch(() => {})
}

function handleDelete(record) {
  deleteInstrument(record.id).then(() => { message.success('已删除'); load() }).catch(() => {})
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  load()
}

onMounted(() => { loadDict(); load() })
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }
.code-link { color: #2563EB; cursor: pointer; font-weight: 600; }
.code-link:hover { text-decoration: underline; }
</style>
