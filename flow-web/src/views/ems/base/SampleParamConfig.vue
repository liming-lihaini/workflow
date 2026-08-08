<template>
  <div class="page-wrap">
    <div v-if="!detail" class="card-wrap">
      <div class="page-header">
        <span class="page-title">采样参数配置管理</span>
        <a-space wrap>
          <a-select
            v-model:value="query.type"
            placeholder="检测类别"
            :options="typeOptions"
            style="width: 160px"
            allow-clear
            @change="loadData"
          />
          <a-input-search
            v-model:value="query.keyword"
            placeholder="检测项目名称"
            style="width: 200px"
            allow-clear
            @search="loadData"
          />
          <a-button type="primary" @click="openCreate">新增配置</a-button>
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
          :scroll="{ x: 1300, y: scrollY }"
          :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sampleParams'">
              <a-tooltip v-if="record.sampleParams && record.sampleParams.length">
                <template #title>
                  <div v-for="p in record.sampleParams" :key="p.code" style="line-height: 1.6">
                    ({{ p.code }}) {{ p.name }} {{ p.unit }} <a-tag :color="p.required ? 'red' : 'default'" style="margin-left: 4px">{{ p.required ? '必填' : '选填' }}</a-tag>
                  </div>
                </template>
                <span>{{ record.sampleParams.map(p => `${p.name}${p.unit ? '(' + p.unit + ')' : ''}`).join('；') }}</span>
              </a-tooltip>
              <span v-else class="muted">—</span>
            </template>
            <template v-if="column.key === 'op'">
              <a-space>
                <a @click="openEdit(record)">编辑</a>
                <a-popconfirm title="确认删除该配置？" @confirm="handleDelete(record)">
                  <a class="danger-link">删除</a>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <!-- 新增 / 编辑 右侧抽屉 -->
    <a-drawer
      v-model:open="modalVisible"
      :title="isEdit ? '编辑采样参数配置' : '新增采样参数配置'"
      width="1000"
      placement="right"
      :mask-closable="false"
      @close="modalVisible = false"
    >
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检测类别" required>
              <a-select
                v-model:value="form.type"
                placeholder="请选择检测类别"
                :options="typeOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检测项目" required>
              <a-select
                v-model:value="form.item"
                placeholder="请选择监测项目（数据字典）"
                :options="itemOptions"
                show-search
                allow-clear
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">现场结构化必填采样参数</a-divider>
        <div class="param-list">
          <div v-for="(p, idx) in form.sampleParams" :key="idx" class="param-item">
            <a-row :gutter="8" align="bottom">
              <a-col :span="4">
                <label class="mini-label">参数编码</label>
                <a-input v-model:value="p.code" placeholder="如 flue_area" />
              </a-col>
              <a-col :span="4">
                <label class="mini-label">参数名称</label>
                <a-input v-model:value="p.name" placeholder="如 烟道截面积" />
              </a-col>
              <a-col :span="3">
                <label class="mini-label">参数类型</label>
                <a-select v-model:value="p.type" :options="paramTypeOptions" />
              </a-col>
              <a-col :span="3">
                <label class="mini-label">单位</label>
                <a-input v-model:value="p.unit" placeholder="如 m²" />
              </a-col>
              <a-col :span="3">
                <label class="mini-label">是否必填</label>
                <a-select v-model:value="p.required" :options="requiredOptions" />
              </a-col>
              <a-col :span="6">
                <label class="mini-label">提示备注</label>
                <a-input v-model:value="p.tip" placeholder="采集要求说明" />
              </a-col>
              <a-col :span="1">
                <a-button danger size="small" @click="removeParam(idx)"><delete-outlined /></a-button>
              </a-col>
            </a-row>
          </div>
          <a-button type="dashed" block style="margin-top: 8px" @click="addParam">
            <plus-outlined /> 添加参数
          </a-button>
        </div>

        <a-row :gutter="16" style="margin-top: 16px">
          <a-col :span="12">
            <a-form-item label="执行标准编号">
              <a-input v-model:value="form.standard" placeholder="如 GB 16297-1996" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标准限值 / 管控要求">
              <a-input v-model:value="form.limit" placeholder="如 根据排气筒高度执行对应排放限值" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="内控限制">
              <a-input v-model:value="form.innerLimit" placeholder="严于国标的企业内控限制，如 昼间 ≤ 55 dB(A)（内控）" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注说明">
              <a-textarea v-model:value="form.remark" :rows="2" placeholder="容器材质、固定剂、保存条件、避光、单独采样等" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="modalVisible = false">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSubmit">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import {
  getSampleParamConfigs,
  saveSampleParamConfig,
  deleteSampleParamConfig,
  batchDeleteSampleParamConfig,
  getDictItems
} from '../../../api/ems'

/* ============ 基础数据 ============ */
const typeOptions = [
  '有组织废气', '无组织废气', '废水/地表水', '地下水', '土壤', '厂界噪声', '室内空气', '固体废物'
].map(t => ({ label: t, value: t }))

// 监测项目（检测项目）下拉：取自数据字典 moni_monitor_factor
const itemOptions = ref([])

const paramTypeOptions = [
  { label: '数值', value: 'number' },
  { label: '文本', value: 'text' },
  { label: '是/否', value: 'bool' },
  { label: '日期时间', value: 'datetime' }
]

const requiredOptions = [
  { label: '是', value: true },
  { label: '否', value: false }
]

/* ============ 列表与检索 ============ */
const query = reactive({ type: undefined, keyword: '' })
const dataList = ref([])
const loading = ref(false)
const selectedRowKeys = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` })

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70, fixed: 'left' },
  { title: '检测类别', dataIndex: 'type', key: 'type', width: 120 },
  { title: '检测项目', dataIndex: 'item', key: 'item', width: 140 },
  { title: '结构化采样必填参数清单', key: 'sampleParams', width: 360 },
  { title: '执行标准编号', dataIndex: 'standard', key: 'standard', width: 140 },
  { title: '标准限值/管控要求', dataIndex: 'limit', key: 'limit', width: 200, ellipsis: true },
  { title: '内控限制', dataIndex: 'innerLimit', key: 'innerLimit', width: 200, ellipsis: true },
  { title: '采样备注', dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true },
  { title: '操作', key: 'op', width: 110, fixed: 'right' }
]

const scrollY = computed(() => Math.max(window.innerHeight - 360, 240))

function loadData() {
  loading.value = true
  getSampleParamConfigs({ type: query.type, keyword: query.keyword })
    .then(res => {
      const list = (res?.data || []).map(d => ({
        ...d,
        limit: d.limit,
        sampleParams: d.sampleParams || []
      }))
      pagination.total = list.length
      const start = (pagination.current - 1) * pagination.pageSize
      dataList.value = list.slice(start, start + pagination.pageSize)
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function handleTableChange(pag) {
  Object.assign(pagination, { current: pag.current, pageSize: pag.pageSize })
  loadData()
}

function onSelectChange(keys) { selectedRowKeys.value = keys }

/* ============ 弹窗与表单 ============ */
const modalVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const form = reactive({
  type: undefined,
  item: '',
  sampleParams: [],
  standard: '',
  limit: '',
  innerLimit: '',
  remark: ''
})

function emptyParam() {
  return { code: '', name: '', type: 'number', unit: '', required: true, tip: '' }
}

function resetForm() {
  form.type = undefined
  form.item = ''
  form.sampleParams = [emptyParam()]
  form.standard = ''
  form.limit = ''
  form.innerLimit = ''
  form.remark = ''
  editingId.value = null
}

function loadItemOptions() {
  getDictItems('moni_monitor_factor')
    .then(res => {
      const list = Array.isArray(res?.data) ? res.data : (res?.data?.list || res?.data || [])
      itemOptions.value = list.map(i => ({ label: i.itemText, value: i.itemValue }))
    })
    .catch(() => {})
}

function openCreate() {
  isEdit.value = false
  resetForm()
  modalVisible.value = true
}

function openEdit(record) {
  isEdit.value = true
  editingId.value = record.id
  form.type = record.type
  form.item = record.item
  form.sampleParams = (record.sampleParams || []).map(p => ({
    code: p.code,
    name: p.name,
    type: p.paramType || p.type,
    unit: p.unit,
    required: p.required === 1 || p.required === true,
    tip: p.tip
  }))
  if (!form.sampleParams.length) form.sampleParams = [emptyParam()]
  form.standard = record.standard
  form.limit = record.limit
  form.innerLimit = record.innerLimit || ''
  form.remark = record.remark
  modalVisible.value = true
}

function addParam() { form.sampleParams.push(emptyParam()) }
function removeParam(idx) { form.sampleParams.splice(idx, 1) }

const submitting = ref(false)

function handleSubmit() {
  if (!form.type) { message.warning('请选择检测类别'); return }
  if (!form.item) { message.warning('请选择监测项目'); return }
  submitting.value = true
  const payload = {
    id: isEdit.value ? editingId.value : null,
    type: form.type,
    item: form.item,
    sampleParams: form.sampleParams.map(p => ({
      code: p.code,
      name: p.name,
      paramType: p.type,
      unit: p.unit,
      required: p.required === true || p.required === 'true',
      tip: p.tip
    })),
    standard: form.standard,
    limit: form.limit,
    innerLimit: form.innerLimit,
    remark: form.remark
  }
  saveSampleParamConfig(payload)
    .then(() => {
      message.success(isEdit.value ? '更新成功' : '保存成功')
      modalVisible.value = false
      loadData()
    })
    .catch(() => {})
    .finally(() => { submitting.value = false })
}

/* ============ 删除 ============ */
function handleDelete(record) {
  deleteSampleParamConfig(record.id)
    .then(() => {
      selectedRowKeys.value = selectedRowKeys.value.filter(k => k !== record.id)
      message.success('已删除')
      loadData()
    })
    .catch(() => {})
}

function handleBatchDelete() {
  Modal.confirm({
    title: `确认删除选中的 ${selectedRowKeys.value.length} 条配置？`,
    okText: '删除',
    okType: 'danger',
    onOk: () => {
      batchDeleteSampleParamConfig(selectedRowKeys.value)
        .then(() => {
          message.success('批量删除成功')
          selectedRowKeys.value = []
          loadData()
        })
        .catch(() => {})
    }
  })
}

onMounted(() => {
  loadData()
  loadItemOptions()
})
</script>

<style scoped>
.page-wrap { height: 100%; }
.card-wrap { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.05); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 18px; font-weight: 600; }
.tbl-box { overflow: auto; }
.muted { color: #bbb; }
.danger-link { color: #ff4d4f; }
.mini-label { display: block; font-size: 12px; color: #888; margin-bottom: 2px; }
.param-item { border: 1px solid #dee2e6; border-radius: 6px; padding: 10px 12px; margin-bottom: 12px; background: #fff; }
.param-item .ant-input, .param-item .ant-select { min-height: 32px; }
</style>
