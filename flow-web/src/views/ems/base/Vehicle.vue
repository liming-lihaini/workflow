<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">车辆台账</span>
        <a-space wrap>
          <a-input-search v-model:value="kw" placeholder="搜索车牌/型号" style="width: 200px" allow-clear @search="load" />
          <a-button type="primary" @click="showDrawer()">新增车辆</a-button>
        </a-space>
      </div>
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="vehicleStatusColor(record.status)">{{ vehicleStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <span class="action-link" @click="showDrawer(record)">编辑</span>
            <a-divider type="vertical" />
            <a-popconfirm title="删除该车辆？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <a-drawer v-model:open="visible" :title="editing ? '编辑车辆' : '新增车辆'" width="520" @close="visible = false">
      <a-form :model="form" layout="vertical">
        <a-form-item label="车牌号" required>
          <a-input v-model:value="form.plateNo" placeholder="如 京A12345" />
        </a-form-item>
        <a-form-item label="型号">
          <a-input v-model:value="form.model" placeholder="如 依维柯采样车" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status" placeholder="选择状态" :options="statusOptions" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="visible = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSave">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listVehicles, createVehicle, updateVehicle, deleteVehicle, getDictItems } from '../../../api/ems'

const loading = ref(false)
const submitting = ref(false)
const visible = ref(false)
const editing = ref(null)
const kw = ref('')
const dataList = ref([])
const statusOptions = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '车牌号', dataIndex: 'plateNo', key: 'plateNo', sorter: true },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 120 }
]

const form = reactive({ id: undefined, plateNo: '', model: '', status: 1, remark: '' })

const STATUS_MAP = { 1: '可用', 2: '占用', 3: '维修' }
function vehicleStatusText(s) { return STATUS_MAP[s] !== undefined ? STATUS_MAP[s] : (s || '-') }
function vehicleStatusColor(s) { return { 1: 'green', 2: 'orange', 3: 'red' }[s] || 'default' }

function loadStatusOptions() {
  getDictItems('moni_vehicle_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      statusOptions.value = list.map((i) => ({ label: i.itemText, value: Number(i.itemValue) }))
    }
  }).catch(() => {})
}

function load() {
  loading.value = true
  listVehicles({ keyword: kw.value || undefined, page: pagination.current, size: pagination.pageSize })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.records || data.list || [])
      dataList.value = list
      pagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function showDrawer(record) {
  if (record) {
    editing.value = record
    Object.assign(form, {
      id: record.id, plateNo: record.plateNo, model: record.model,
      status: record.status, remark: record.remark
    })
  } else {
    editing.value = null
    Object.assign(form, { id: undefined, plateNo: '', model: '', status: 1, remark: '' })
  }
  visible.value = true
}

function handleSave() {
  if (!form.plateNo) { message.warning('请填写车牌号'); return }
  submitting.value = true
  const api = editing.value ? updateVehicle(form.id, form) : createVehicle(form)
  api.then(() => {
    message.success('保存成功')
    visible.value = false
    load()
  }).catch(() => {}).finally(() => { submitting.value = false })
}

function handleDelete(record) {
  deleteVehicle(record.id).then(() => { message.success('已删除'); load() }).catch(() => {})
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  load()
}

onMounted(() => { loadStatusOptions(); load() })
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }
</style>
