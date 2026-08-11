<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">设备台账（仪器设备全生命周期）</span>
        <a-space wrap>
          <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
            <a-radio-button value="list">列表</a-radio-button>
            <a-radio-button value="calendar">使用日历</a-radio-button>
          </a-radio-group>
          <a-input-search v-if="viewMode === 'list'" v-model:value="kw" placeholder="搜索编号/名称/型号" style="width: 220px" allow-clear @search="load" />
          <a-select v-if="viewMode === 'list'" v-model:value="statusFilter" placeholder="状态" allow-clear style="width: 120px" :options="statusFilterOptions" @change="load" />
          <template v-if="viewMode === 'list'">
            <a-button type="primary" @click="showDrawer()">新增设备</a-button>
          </template>
        </a-space>
      </div>
      <!-- 列表模式 -->
      <template v-if="viewMode === 'list'">
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
              <span class="action-link" @click="openDetail(record)">申请详情</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showDrawer(record)">编辑</span>
              <a-divider type="vertical" />
              <span class="action-link" @click="showCalibrate(record)">校准登记</span>
              <template v-if="record.status !== '报废'">
                <a-divider type="vertical" />
                <span class="action-link danger" @click="startScrapProcess(record)">报废</span>
              </template>
              <a-divider type="vertical" />
              <a-popconfirm title="删除该设备？" @confirm="handleDelete(record)">
                <span class="action-link danger">删除</span>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </template>

      <!-- 使用日历模式 -->
      <div v-if="viewMode === 'calendar'" class="cal-wrap">
        <div class="cal-toolbar">
          <a-space>
            <a-button size="small" @click="() => changeMonth(-1)">上个月</a-button>
            <span class="cal-title">{{ calYear }} 年 {{ calMonth }} 月</span>
            <a-button size="small" @click="() => changeMonth(1)">下个月</a-button>
            <a-button size="small" @click="goToday">今天</a-button>
            <a-button size="small" type="primary" :loading="calLoading" @click="loadInstrumentUsage">刷新</a-button>
          </a-space>
        </div>
        <a-calendar v-model:value="calValue" :cell-render="dateCellRender">
          <template #headerRender="{ value }">
            <span class="cal-title">{{ value.year() }} 年 {{ value.month() + 1 }} 月</span>
          </template>
        </a-calendar>
        <div class="cal-side">
          <div class="cal-legend">
            <span class="dot inst"></span>派单占用（蓝）
            <span class="dot maint"></span>校准占用（橙）
          </div>
          <div class="cal-list">
            <div class="cal-list-title">本月设备占用（{{ instrumentUsages.length }} 台）</div>
            <div v-for="(v, i) in instrumentUsages" :key="v.instrumentId" class="cal-item">
              <div class="cal-item-head">
                <span class="cal-item-name">{{ v.code }} {{ v.name }}</span>
                <a-tag :color="instStatusColor(v.status)">{{ v.status }}</a-tag>
              </div>
              <div v-if="(v.ranges && v.ranges.length) || (v.maintenances && v.maintenances.length)">
                <div v-for="(r, ri) in v.ranges" :key="'r' + ri" class="cal-range inst">{{ fmt(r.start) }} ~ {{ fmt(r.end) }} 派单#{{ r.dispatchId }}（{{ r.status }}）</div>
                <div v-for="(m, mi) in v.maintenances" :key="'m' + mi" class="cal-range maint">{{ fmt(m.start) }} ~ {{ fmt(m.end) }} 校准{{ m.certNo ? '（' + m.certNo + '）' : '' }}</div>
              </div>
              <div v-else class="cal-empty">本月无占用</div>
            </div>
            <a-empty v-if="!instrumentUsages.length" description="暂无设备占用数据" />
          </div>
        </div>
      </div>
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
import { ref, reactive, onMounted, computed, h, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listInstruments, createInstrument, updateInstrument, deleteInstrument, calibrateInstrument, expiringInstruments, getDictItems, getInstrumentUsage } from '../../../api/ems'
import InstrumentDetail from './InstrumentDetail.vue'
import dayjs from 'dayjs'

const router = useRouter()

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

// ===== 使用日历 =====
const viewMode = ref('list')
const calLoading = ref(false)
const calYear = ref(dayjs().year())
const calMonth = ref(dayjs().month() + 1)
const calValue = ref(dayjs())
const instrumentUsages = ref([])

function loadInstrumentUsage() {
  calLoading.value = true
  const s = dayjs(`${calYear.value}-${calMonth.value}-01`).subtract(1, 'month').startOf('month')
  const e = dayjs(`${calYear.value}-${calMonth.value}-01`).add(1, 'month').endOf('month')
  getInstrumentUsage({
    start: s.format('YYYY-MM-DDTHH:mm:ss'),
    end: e.format('YYYY-MM-DDTHH:mm:ss')
  }).then((res) => {
    const data = res.data || res
    instrumentUsages.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {}).finally(() => { calLoading.value = false })
}

function changeMonth(delta) {
  const d = dayjs(`${calYear.value}-${calMonth.value}-01`).add(delta, 'month')
  calYear.value = d.year()
  calMonth.value = d.month() + 1
  calValue.value = d
  loadInstrumentUsage()
}
function goToday() {
  calYear.value = dayjs().year()
  calMonth.value = dayjs().month() + 1
  calValue.value = dayjs()
  loadInstrumentUsage()
}
function fmt(v) {
  if (!v) return '-'
  return dayjs(String(v).replace(' ', 'T')).format('MM-DD')
}

function dateCellRender(current) {
  const day = dayjs(current).format('YYYY-MM-DD')
  const events = []
  for (const v of instrumentUsages.value) {
    const ranges = v.ranges || []
    for (const r of ranges) {
      if (r.start && r.end) {
        const s = dayjs(String(r.start).replace(' ', 'T')).format('YYYY-MM-DD')
        const e = dayjs(String(r.end).replace(' ', 'T')).format('YYYY-MM-DD')
        if (day >= s && day <= e) events.push({ type: 'inst', text: `${v.code} ${v.name}` })
      }
    }
    const ms = v.maintenances || []
    for (const m of ms) {
      if (m.start && m.end) {
        const s = dayjs(String(m.start).replace(' ', 'T')).format('YYYY-MM-DD')
        const e = dayjs(String(m.end).replace(' ', 'T')).format('YYYY-MM-DD')
        if (day >= s && day <= e) events.push({ type: 'maint', text: `${v.code} ${v.name} 校准` })
      }
    }
  }
  if (!events.length) return null
  return events.slice(0, 4).map((ev) => h('div', { class: `cal-badge ${ev.type}` }, ev.text))
}

const columns = [
  { title: '编号', dataIndex: 'code', key: 'code', width: 100 },
  { title: '名称', dataIndex: 'name', key: 'name', sorter: true },
  { title: '型号', dataIndex: 'model', key: 'model' },
  { title: '厂商', dataIndex: 'manufacturer', key: 'manufacturer' },
  { title: '状态', key: 'status', dataIndex: 'status', width: 90 },
  { title: '校准到期', key: 'calibDue', dataIndex: 'calibDue', width: 120 },
  { title: '证书号', dataIndex: 'certNo', key: 'certNo' },
  { title: '操作', key: 'action', width: 350 }
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

// 发起资产报废申请(ZCBFSQ)：跳转流程发起详情页，预填资产类型/资产ID/名称/编号；
// 申请人需在表单中说明报废原因与处置方式，审批通过后由 Webhook 更新台账状态为报废
function startScrapProcess(record) {
  const query = { processKey: 'ZCBFSQ', assetType: '设备', assetId: record.id }
  if (record.name) query.name = record.name
  if (record.code) query.spec = record.code
  router.push({ path: '/task/start-detail', query })
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  load()
}

onMounted(() => { loadDict(); load() })

// 切换至使用日历时加载占用数据
watch(viewMode, (v) => { if (v === 'calendar') loadInstrumentUsage() })
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }
.code-link { color: #2563EB; cursor: pointer; font-weight: 600; }
.code-link:hover { text-decoration: underline; }

/* 使用日历 */
.cal-wrap { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
.cal-toolbar { width: 100%; display: flex; justify-content: space-between; margin-bottom: 8px; }
.cal-title { font-weight: 600; }
.cal-side { width: 320px; flex-shrink: 0; border: 1px solid #f0f0f0; border-radius: 8px; padding: 12px; max-height: 640px; overflow: auto; }
.cal-legend { font-size: 12px; color: #666; margin-bottom: 10px; display: flex; gap: 12px; align-items: center; }
.cal-legend .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
.cal-legend .dot.inst { background: #1677ff; }
.cal-legend .dot.maint { background: #fa8c16; }
.cal-list-title { font-weight: 600; margin-bottom: 8px; }
.cal-item { border-bottom: 1px dashed #eee; padding: 8px 0; }
.cal-item-head { display: flex; justify-content: space-between; align-items: center; }
.cal-item-name { font-weight: 600; }
.cal-range { font-size: 12px; margin-top: 4px; padding: 2px 6px; border-radius: 4px; }
.cal-range.inst { background: #e6f4ff; color: #1677ff; }
.cal-range.maint { background: #fff7e6; color: #d46b08; }
.cal-empty { color: #999; font-size: 12px; margin-top: 4px; }
.cal-badge { font-size: 11px; line-height: 1.5; padding: 0 4px; border-radius: 3px; margin: 1px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cal-badge.inst { background: #e6f4ff; color: #1677ff; }
.cal-badge.maint { background: #fff7e6; color: #d46b08; }
</style>
