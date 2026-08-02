<template>
  <div class="page-wrap">
    <a-tabs v-model:activeKey="tab">
      <!-- ===== 设备台账 ===== -->
      <a-tab-pane key="instrument" tab="设备台账">
        <div class="card-wrap">
          <div class="page-header">
            <span class="page-title">仪器设备（全生命周期）</span>
            <a-space wrap>
              <a-input-search v-model:value="kw" placeholder="搜索编号/名称/型号" style="width: 220px" allow-clear @search="loadInstruments" />
              <a-select v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 120px" :options="statusOptions" @change="loadInstruments" />
              <a-button type="primary" @click="showInstDrawer()">新增设备</a-button>
            </a-space>
          </div>
          <a-alert
            v-if="expiring.length"
            type="warning"
            show-icon
            style="margin-bottom: 12px"
            :message="`校准预警：${expiring.length} 台设备临近到期或已停用`"
          />
          <div class="tbl-box">
          <a-table
            :columns="instColumns"
            :data-source="instList"
            :loading="instLoading"
            :pagination="instPagination"
            row-key="id"
            :scroll="{ y: scrollY }"
            @change="handleInstTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="code-link" @click="openInstDetail(record)">{{ record.code }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="instStatusColor(record.status)">{{ record.status }}</a-tag>
              </template>
              <template v-if="column.key === 'calibDue'">
                <span :style="{ color: isExpiring(record) ? '#fa8c16' : 'inherit' }">{{ record.calibDue || '-' }}</span>
              </template>
              <template v-if="column.key === 'action'">
                <span class="action-link" @click="showInstDrawer(record)">编辑</span>
                <a-divider type="vertical" />
                <span class="action-link" @click="showCalibrate(record)">校准登记</span>
                <a-divider type="vertical" />
                <a-popconfirm title="删除该设备？" @confirm="handleInstDelete(record)">
                  <span class="action-link danger">删除</span>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
          </div>
        </div>
      </a-tab-pane>

      <!-- ===== 标准物质 ===== -->
      <a-tab-pane key="material" tab="标准物质">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openMaterial()">+ 新增标物</a-button>
            <a-input-search v-model:value="mk" placeholder="名称搜索" style="width:200px" @search="loadMaterials" allow-clear />
            <a-select v-model:value="mStatus" style="width:140px" @change="loadMaterials">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
            <a-button @click="checkGate">效期闸门校验</a-button>
          </a-space>
          <div class="tbl-box">
          <a-table :columns="materialCols" :data-source="materials" row-key="id" :pagination="mp" :loading="ml" :scroll="{ y: scrollY }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="cell-link" @click="openMaterialDetail(record)">{{ record.lotNo }}</span>
              </template>
              <template v-else-if="column.key === 'name'">
                <span class="cell-link" @click="openMaterialDetail(record)">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a @click="openMaterial(record)">编辑</a>
              </template>
            </template>
          </a-table>
          </div>
        </a-card>
      </a-tab-pane>

      <!-- ===== 耗材 ===== -->
      <a-tab-pane key="consumable" tab="耗材">
        <a-card :bordered="false">
          <a-space style="margin-bottom:16px">
            <a-button type="primary" @click="openConsumable()">+ 新增耗材</a-button>
            <a-input-search v-model:value="ck" placeholder="名称搜索" style="width:200px" @search="loadConsumables" allow-clear />
            <a-select v-model:value="cStatus" style="width:140px" @change="loadConsumables">
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option value="在库">在库</a-select-option>
              <a-select-option value="临期">临期</a-select-option>
              <a-select-option value="过期">过期</a-select-option>
            </a-select>
          </a-space>
          <div class="tbl-box">
          <a-table :columns="consumableCols" :data-source="consumables" row-key="id" :pagination="cp" :loading="cl" :scroll="{ y: scrollY }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <span class="cell-link" @click="openConsumableDetail(record)">{{ record.id ? 'HC' + String(record.id).padStart(4, '0') : '' }}</span>
              </template>
              <template v-else-if="column.key === 'name'">
                <span class="cell-link" @click="openConsumableDetail(record)">{{ record.name }}</span>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status==='过期'?'red':(record.status==='临期'?'orange':'green')">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a @click="openConsumable(record)">编辑</a>
              </template>
            </template>
          </a-table>
          </div>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 设备新增/编辑 -->
    <a-drawer v-model:open="instVisible" :title="instEditing ? '编辑设备' : '新增设备'" width="560" @close="instVisible = false">
      <a-form :model="instForm" layout="vertical">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="仪器编号"><a-input v-model:value="instForm.code" placeholder="YQ001" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="仪器名称" required><a-input v-model:value="instForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="型号"><a-input v-model:value="instForm.model" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="生产厂商"><a-input v-model:value="instForm.manufacturer" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="购置日期"><a-date-picker v-model:value="instForm.purchaseDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准到期日"><a-date-picker v-model:value="instForm.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="状态"><a-select v-model:value="instForm.status" :options="statusOptions" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="校准证书号"><a-input v-model:value="instForm.certNo" /></a-form-item></a-col>
        </a-row>
        <a-form-item label="备注"><a-textarea v-model:value="instForm.remark" :rows="2" /></a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="instVisible = false">取消</a-button>
          <a-button type="primary" :loading="instSubmitting" @click="handleInstSave">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 设备校准登记 -->
    <a-modal v-model:open="calibVisible" title="校准登记" @ok="submitCalibrate" ok-text="登记">
      <a-form layout="vertical">
        <a-form-item label="校准日期"><a-date-picker v-model:value="calibForm.calibDate" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="下次校准到期日" required><a-date-picker v-model:value="calibForm.calibDue" value-format="YYYY-MM-DD" style="width:100%" /></a-form-item>
        <a-form-item label="校准证书编号"><a-input v-model:value="calibForm.certNo" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 设备详情 -->
    <InstrumentDetail :open="instDetailVisible" :instrument-id="instDetailId" @close="instDetailVisible = false" />

    <!-- 标物新增/编辑抽屉 -->
    <a-drawer v-model:open="mVisible" :title="mForm.id ? '编辑标准物质' : '新增标准物质'" width="1000" @close="mVisible = false">
      <a-form :model="mForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="名称"><a-input v-model:value="mForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="批号"><a-input v-model:value="mForm.lotNo" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="规格"><a-input v-model:value="mForm.spec" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="效期"><a-date-picker v-model:value="mExpire" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="库存"><a-input-number v-model:value="mForm.stock" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="证书编号"><a-input v-model:value="mForm.certNo" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="mVisible = false">取消</a-button>
          <a-button type="primary" :loading="mSubmitting" @click="submitMaterial">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 标物详情抽屉 -->
    <a-drawer v-model:open="mDetailVisible" title="标准物质详情" width="1000" @close="mDetailVisible = false">
      <a-descriptions :column="2" bordered size="middle" v-if="mDetail">
        <a-descriptions-item label="编号">{{ mDetail.lotNo }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ mDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ mDetail.spec }}</a-descriptions-item>
        <a-descriptions-item label="效期">{{ mDetail.expireDate }}</a-descriptions-item>
        <a-descriptions-item label="库存">{{ mDetail.stock }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="mDetail.status==='过期'?'red':(mDetail.status==='临期'?'orange':'green')">{{ mDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="证书编号">{{ mDetail.certNo }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ mDetail.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>

    <!-- 耗材新增/编辑抽屉 -->
    <a-drawer v-model:open="cVisible" :title="cForm.id ? '编辑耗材' : '新增耗材'" width="1000" @close="cVisible = false">
      <a-form :model="cForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12"><a-form-item label="名称"><a-input v-model:value="cForm.name" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="规格"><a-input v-model:value="cForm.spec" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="数量"><a-input-number v-model:value="cForm.qty" :min="0" style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="效期"><a-date-picker v-model:value="cExpire" style="width:100%" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="cVisible = false">取消</a-button>
          <a-button type="primary" :loading="cSubmitting" @click="submitConsumable">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 耗材详情抽屉 -->
    <a-drawer v-model:open="cDetailVisible" title="耗材详情" width="1000" @close="cDetailVisible = false">
      <a-descriptions :column="2" bordered size="middle" v-if="cDetail">
        <a-descriptions-item label="编号">{{ cDetail.id ? 'HC' + String(cDetail.id).padStart(4, '0') : '' }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ cDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ cDetail.spec }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ cDetail.qty }}</a-descriptions-item>
        <a-descriptions-item label="效期">{{ cDetail.expireDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="cDetail.status==='过期'?'red':(cDetail.status==='临期'?'orange':'green')">{{ cDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ cDetail.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  listInstruments, createInstrument, updateInstrument, deleteInstrument,
  calibrateInstrument, expiringInstruments, getDictItems,
  saveMaterial, getMaterials, saveConsumable, getConsumables, checkMaterialGate
} from '../../../api/ems'
import InstrumentDetail from './InstrumentDetail.vue'

const tab = ref('instrument')

/* ============ 表格铺满页面高度（精确测量 tbl-box，避免撑出主页滚动条） ============ */
const scrollY = ref(420)
function syncTableHeight() {
  // 测量当前激活标签页内 .tbl-box 的可用高度
  // scroll.y = tbl-box 高度 - 表头 - 分页(含margin) - 安全余量
  const pane = document.querySelector('.ant-tabs-tabpane-active')
  const box = pane && pane.querySelector('.tbl-box')
  if (!box) return
  const boxRect = box.getBoundingClientRect()
  const headerEl = box.querySelector('.ant-table-thead')
  const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40
  const pagEl = box.querySelector('.ant-table-pagination')
  let reservedBottom = 0
  if (pagEl) {
    // 分页底部相对 box 底部的距离（含 margin）
    const pagRect = pagEl.getBoundingClientRect()
    reservedBottom = boxRect.bottom - pagRect.top
  }
  // 若无分页或未渲染，兜底预留 80px
  if (!reservedBottom) reservedBottom = 80
  const h = boxRect.height - headerH - reservedBottom - 4
  scrollY.value = h > 200 ? Math.floor(h) : 200
}

/* ============ 设备台账 ============ */
const instLoading = ref(false)
const instSubmitting = ref(false)
const instVisible = ref(false)
const calibVisible = ref(false)
const instEditing = ref(null)
const instDetailVisible = ref(false)
const instDetailId = ref(null)
const kw = ref('')
const statusFilter = ref(undefined)
const instList = ref([])
const expiring = ref([])
const statusOptions = ref([])
const instStatusFallback = [
  { label: '在用', value: '在用' }, { label: '临期', value: '临期' },
  { label: '停用', value: '停用' }, { label: '维修', value: '维修' }, { label: '报废', value: '报废' }
]
const instPagination = reactive({ current: 1, pageSize: 10, total: 0 })

const instColumns = [
  { title: '编号', dataIndex: 'code', key: 'code', width: 100 },
  { title: '名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '厂商', dataIndex: 'manufacturer', key: 'manufacturer' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '校准到期', key: 'calibDue', dataIndex: 'calibDue', width: 120 },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action', width: 240, fixed: 'right' }
]

const instForm = reactive({ id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
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

function loadInstDict() {
  getDictItems('moni_instrument_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    statusOptions.value = list.length
      ? list.map((i) => ({ label: i.itemText, value: i.itemText }))
      : instStatusFallback
  }).catch(() => { statusOptions.value = instStatusFallback })
}

function loadInstruments() {
  instLoading.value = true
  listInstruments({ keyword: kw.value || undefined, status: statusFilter.value, page: instPagination.current, size: instPagination.pageSize })
    .then((res) => {
      const data = res.data || res
      const list = Array.isArray(data) ? data : (data.records || data.list || [])
      instList.value = list
      instPagination.total = Array.isArray(data) ? list.length : (data.total || list.length)
    })
    .catch(() => {})
    .finally(() => { instLoading.value = false })
  loadExpiring()
}

function loadExpiring() {
  expiringInstruments().then((res) => {
    const data = res.data || res
    expiring.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {})
}

function openInstDetail(record) {
  instDetailId.value = record.id
  instDetailVisible.value = true
}

function showInstDrawer(record) {
  if (record) {
    instEditing.value = record
    Object.assign(instForm, {
      id: record.id, code: record.code, name: record.name, model: record.model,
      manufacturer: record.manufacturer, purchaseDate: record.purchaseDate,
      calibDue: record.calibDue, status: record.status, certNo: record.certNo, remark: record.remark
    })
  } else {
    instEditing.value = null
    Object.assign(instForm, { id: undefined, code: '', name: '', model: '', manufacturer: '', purchaseDate: null, calibDue: null, status: '在用', certNo: '', remark: '' })
  }
  instVisible.value = true
}

function handleInstSave() {
  if (!instForm.name) { message.warning('请填写仪器名称'); return }
  instSubmitting.value = true
  const api = instEditing.value ? updateInstrument(instForm.id, instForm) : createInstrument(instForm)
  api.then(() => { message.success('保存成功'); instVisible.value = false; loadInstruments() })
    .catch(() => {}).finally(() => { instSubmitting.value = false })
}

function showCalibrate(record) {
  Object.assign(calibForm, { id: record.id, calibDate: dayjs().format('YYYY-MM-DD'), calibDue: null, certNo: record.certNo })
  calibVisible.value = true
}

function submitCalibrate() {
  if (!calibForm.calibDue) { message.warning('请填写下次校准到期日'); return }
  calibrateInstrument(calibForm.id, {
    calibDate: calibForm.calibDate, calibDue: calibForm.calibDue, certNo: calibForm.certNo
  }).then(() => { message.success('校准登记成功，状态已重算'); calibVisible.value = false; loadInstruments() })
    .catch(() => {})
}

function handleInstDelete(record) {
  deleteInstrument(record.id).then(() => { message.success('已删除'); loadInstruments() }).catch(() => {})
}

function handleInstTableChange(pag) {
  instPagination.current = pag.current
  instPagination.pageSize = pag.pageSize
  loadInstruments()
}

/* ============ 标准物质 ============ */
const ml = ref(false)
const mk = ref(''), mStatus = ref('')
const materials = ref([])
const mp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { mp.current = p; loadMaterials() } })
const materialCols = [
  { title: '编号', dataIndex: 'lotNo', key: 'code', width: 140 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '库存', dataIndex: 'stock', key: 'stock' },
  { title: '状态', key: 'status' },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const mVisible = ref(false)
const mSubmitting = ref(false)
const mDetailVisible = ref(false)
const mDetail = ref(null)
const mForm = reactive({ id: null, name: '', lotNo: '', spec: '', expireDate: '', stock: 0, certNo: '' })
const mExpire = ref(null)

function toStr(d) { return d ? (d.$d ? d.format('YYYY-MM-DD') : d) : null }
function openMaterial(r) {
  if (r) { Object.assign(mForm, r); mExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(mForm).forEach(k => mForm[k] = k === 'stock' ? 0 : null); mExpire.value = null }
  mVisible.value = true
}
function openMaterialDetail(r) {
  mDetail.value = r
  mDetailVisible.value = true
}
async function submitMaterial() {
  if (!mForm.name) { message.warning('请填写名称'); return }
  mSubmitting.value = true
  mForm.expireDate = toStr(mExpire.value)
  await saveMaterial({ ...mForm }); message.success('已保存'); mVisible.value = false; loadMaterials()
  mSubmitting.value = false
}
async function loadMaterials() {
  ml.value = true
  try {
    const res = await getMaterials({ keyword: mk.value, status: mStatus.value, page: mp.current, size: mp.pageSize })
    const p = res.data || res; materials.value = p.records || p.list || []; mp.total = p.total || materials.value.length
  } finally { ml.value = false }
}

/* ============ 耗材 ============ */
const cl = ref(false)
const ck = ref(''), cStatus = ref('')
const consumables = ref([])
const cp = reactive({ current: 1, pageSize: 20, total: 0, onChange: (p) => { cp.current = p; loadConsumables() } })
const consumableCols = [
  { title: '编号', key: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '规格', dataIndex: 'spec', key: 'spec' },
  { title: '数量', dataIndex: 'qty', key: 'qty' },
  { title: '效期', dataIndex: 'expireDate', key: 'expireDate' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]
const cVisible = ref(false)
const cSubmitting = ref(false)
const cDetailVisible = ref(false)
const cDetail = ref(null)
const cForm = reactive({ id: null, name: '', spec: '', qty: 0, expireDate: '' })
const cExpire = ref(null)

function openConsumable(r) {
  if (r) { Object.assign(cForm, r); cExpire.value = r.expireDate ? dayjs(r.expireDate) : null }
  else { Object.keys(cForm).forEach(k => cForm[k] = k === 'qty' ? 0 : null); cExpire.value = null }
  cVisible.value = true
}
function openConsumableDetail(r) {
  cDetail.value = r
  cDetailVisible.value = true
}
async function submitConsumable() {
  if (!cForm.name) { message.warning('请填写名称'); return }
  cSubmitting.value = true
  cForm.expireDate = toStr(cExpire.value)
  await saveConsumable({ ...cForm }); message.success('已保存'); cVisible.value = false; loadConsumables()
  cSubmitting.value = false
}
async function loadConsumables() {
  cl.value = true
  try {
    const res = await getConsumables({ keyword: ck.value, status: cStatus.value, page: cp.current, size: cp.pageSize })
    const p = res.data || res; consumables.value = p.records || p.list || []; cp.total = p.total || consumables.value.length
  } finally { cl.value = false }
}
async function checkGate() {
  const res = await checkMaterialGate()
  const g = res.data || res
  if (g.pass) message.success('物资效期闸门通过')
  else message.warning(`有 ${g.blocked.length} 项标物临近/已过期`)
}

let pageObserver = null
onMounted(() => {
  loadInstDict()
  loadInstruments()
  loadMaterials()
  loadConsumables()
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

// 切换标签页后重新测量（不同标签页工具栏/预警条高度不同）
watch(tab, () => { nextTick(syncTableHeight) })

onUnmounted(() => {
  window.removeEventListener('resize', syncTableHeight)
  if (pageObserver) pageObserver.disconnect()
})
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }
.code-link { color: #2563EB; cursor: pointer; font-weight: 600; }
.code-link:hover { text-decoration: underline; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.page-title { font-size: 16px; font-weight: 600; }
.page-wrap { height: 100%; display: flex; flex-direction: column; overflow: hidden; padding: 16px; box-sizing: border-box; }
.page-wrap :deep(.ant-tabs) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.page-wrap :deep(.ant-tabs-content-holder) { flex: 1; min-height: 0; }
.page-wrap :deep(.ant-tabs-content) { height: 100%; }
.page-wrap :deep(.ant-tabs-tabpane) { height: 100%; }
.card-wrap { flex: 1; min-height: 0; display: flex; flex-direction: column; background: #fff; padding: 16px; border-radius: 8px; box-sizing: border-box; overflow: hidden; }
.page-wrap :deep(.ant-card) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.page-wrap :deep(.ant-card-body) { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 8px 0; }
/* tbl-box 撑满卡片剩余空间，由 JS 测量其高度设置 scroll.y */
.tbl-box { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-wrapper) { flex: 1; min-height: 0; height: 100%; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-nested-loading) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-container) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-pagination) { margin: 8px 0 16px !important; flex: 0 0 auto; }
.cell-link { color: #1677ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
</style>
