<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">车辆台账</span>
        <a-space wrap>
          <a-radio-group v-model:value="viewMode" button-style="solid" @change="onViewChange">
            <a-radio-button value="table">车辆台账</a-radio-button>
            <a-radio-button value="calendar">使用日历</a-radio-button>
          </a-radio-group>
          <a-input-search v-if="viewMode === 'table'" v-model:value="kw" placeholder="搜索车牌/型号" style="width: 200px" allow-clear @search="load" />
          <a-button v-if="viewMode === 'table'" type="primary" @click="showDrawer()">新增车辆</a-button>
        </a-space>
      </div>

      <!-- 车辆台账表格 -->
      <a-table
        v-show="viewMode === 'table'"
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
            <span class="action-link" @click="openMaintenance(record)">维修保养</span>
            <a-divider type="vertical" />
            <span class="action-link" @click="openDetail(record)">详情</span>
            <a-divider type="vertical" />
            <a-popconfirm title="删除该车辆？" @confirm="handleDelete(record)">
              <span class="action-link danger">删除</span>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <!-- 车辆使用日历（ISSUE-035 / ISSUE-036） -->
      <div v-if="viewMode === 'calendar'" class="calendar-host">
        <a-space wrap class="cal-toolbar">
          <a-radio-group v-model:value="calUnit" button-style="solid" @change="onCalUnitChange">
            <a-radio-button value="week">周日历</a-radio-button>
            <a-radio-button value="month">月日历</a-radio-button>
          </a-radio-group>
          <a-range-picker
            v-model:value="calRange"
            :allow-clear="false"
            value-format="YYYY-MM-DD"
            @change="loadUsage"
          />
          <a-button @click="shiftRange(-1)">{{ calUnit === 'month' ? '‹ 上一月' : '‹ 上一周' }}</a-button>
          <a-button @click="shiftRange(1)">{{ calUnit === 'month' ? '下一月 ›' : '下一周 ›' }}</a-button>
          <span class="cal-tip">
            <i class="cal-dot cal-dot-dispatch" />派单占用
            <i class="cal-dot cal-dot-maint" />维修保养
          </span>
        </a-space>

        <div v-if="calendarLoading" class="cal-loading"><a-spin /></div>
        <div v-else class="cal-grid">
          <div class="cal-row cal-head">
            <div class="cal-label">车辆</div>
            <div class="cal-track">
              <div v-for="d in days" :key="d.key" class="cal-cell cal-cell-head">
                <div class="cal-dow">{{ d.dow }}</div>
                <div class="cal-date">{{ d.date }}</div>
              </div>
            </div>
          </div>
          <div v-for="v in usageList" :key="v.vehicleId" class="cal-row">
            <div class="cal-label" :title="v.plateNo">
              <div class="cal-plate">{{ v.plateNo }}</div>
              <div class="cal-model">{{ v.model || '' }}</div>
            </div>
            <div class="cal-track">
              <div v-for="d in days" :key="d.key" class="cal-cell">
                <template v-for="blk in blocksOf(v, d)" :key="blk.key">
                  <div
                    class="cal-block"
                    :class="[blk.kind === 'maint' ? 'cal-block-maint' : 'cal-block-dispatch', { 'cal-block-partial': !blk.fullDay }]"
                    :title="blk.tip"
                  >{{ blk.fullDay ? '' : (blk.kind === 'maint' ? '保' : '派') }}</div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑车辆 -->
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

    <!-- 维修保养添加（ISSUE-036） -->
    <a-drawer v-model:open="maintVisible" title="车辆维修保养" width="520" @close="maintVisible = false">
      <a-descriptions :column="1" size="small" bordered v-if="currentVehicle">
        <a-descriptions-item label="车辆">{{ currentVehicle.plateNo }}{{ currentVehicle.model ? ' / ' + currentVehicle.model : '' }}</a-descriptions-item>
      </a-descriptions>
      <a-form :model="maintForm" layout="vertical" style="margin-top: 16px">
        <a-form-item label="开始日期" required>
          <a-date-picker v-model:value="maintForm.startDate" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="disabledMaintDate" />
        </a-form-item>
        <a-form-item label="结束日期" required>
          <a-date-picker v-model:value="maintForm.endDate" value-format="YYYY-MM-DD" style="width: 100%" :disabled-date="disabledMaintDate" />
        </a-form-item>
        <a-form-item label="操作类型" required>
          <a-select v-model:value="maintForm.maintType" placeholder="选择操作类型" :options="maintTypeOptions" />
        </a-form-item>
        <a-form-item label="备注说明">
          <a-textarea v-model:value="maintForm.remark" :rows="4" placeholder="可填写保养/维修内容说明" />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-space>
          <a-button @click="maintVisible = false">取消</a-button>
          <a-button type="primary" :loading="maintSubmitting" @click="submitMaintenance">保存</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 车辆详情（ISSUE-036） -->
    <a-drawer v-model:open="detailVisible" title="车辆详情" width="1000" @close="detailVisible = false">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-divider class="title-divider" orientation="left">基本信息</a-divider>
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="车牌号">{{ detail.vehicle?.plateNo }}</a-descriptions-item>
            <a-descriptions-item label="型号">{{ detail.vehicle?.model || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="vehicleStatusColor(detail.vehicle?.status)">{{ vehicleStatusText(detail.vehicle?.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="备注">{{ detail.vehicle?.remark || '-' }}</a-descriptions-item>
          </a-descriptions>

          <a-divider class="title-divider" orientation="left">派单记录</a-divider>
          <a-table
            :columns="dispatchColumns"
            :data-source="detail.dispatches || []"
            row-key="dispatchId"
            size="small"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'time'">
                {{ fmt(record.planStart) }} ~ {{ fmt(record.planEnd) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag>{{ record.status }}</a-tag>
              </template>
            </template>
            <template #emptyText>暂无派单记录</template>
          </a-table>

          <a-divider class="title-divider" orientation="left">维修保养记录</a-divider>
          <a-table
            :columns="maintColumns"
            :data-source="detail.maintenances || []"
            row-key="id"
            size="small"
            :pagination="false"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'time'">
                {{ fmt(record.startDate) }} ~ {{ fmt(record.endDate) }}
              </template>
              <template v-else-if="column.key === 'type'">
                {{ maintTypeText(record.maintType) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popconfirm title="删除该维修保养记录？" @confirm="removeMaintenance(record)">
                  <span class="action-link danger">删除</span>
                </a-popconfirm>
              </template>
            </template>
            <template #emptyText>暂无维修保养记录</template>
          </a-table>
        </template>
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  listVehicles, createVehicle, updateVehicle, deleteVehicle, getDictItems, getVehicleUsage,
  createVehicleMaintenance, listVehicleMaintenances, deleteVehicleMaintenance, getVehicleDetail
} from '../../../api/ems'

const loading = ref(false)
const submitting = ref(false)
const visible = ref(false)
const editing = ref(null)
const kw = ref('')
const dataList = ref([])
const statusOptions = ref([])
const maintTypeOptions = ref([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// ISSUE-035 视图模式与日历状态
const viewMode = ref('table')
const calendarLoading = ref(false)
const calRange = ref([dayjs().startOf('week').format('YYYY-MM-DD'), dayjs().endOf('week').format('YYYY-MM-DD')])
const usageList = ref([])
const days = ref([])
const calUnit = ref('week')

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '车牌号', dataIndex: 'plateNo', key: 'plateNo', sorter: true, width: 180 },
  { title: '型号', dataIndex: 'model', key: 'model', width: 200 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 220 }
]

const dispatchColumns = [
  { title: '派单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '委托单名称', dataIndex: 'entrustName', key: 'entrustName', ellipsis: true },
  { title: '时间区间', key: 'time' },
  { title: '状态', key: 'status', width: 110 }
]
const maintColumns = [
  { title: '类型', key: 'type', width: 100 },
  { title: '时间区间', key: 'time' },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 80 }
]

const form = reactive({ id: undefined, plateNo: '', model: '', status: 1, remark: '' })

const STATUS_MAP = { 1: '可用', 2: '占用', 3: '维修保养中' }
function vehicleStatusText(s) { return STATUS_MAP[s] !== undefined ? STATUS_MAP[s] : (s || '-') }
function vehicleStatusColor(s) { return { 1: 'green', 2: 'orange', 3: 'red' }[s] || 'default' }
const MAINT_MAP = {}
function maintTypeText(v) { return MAINT_MAP[v] || v || '-' }

function loadStatusOptions() {
  getDictItems('moni_vehicle_status').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      statusOptions.value = list.map((i) => ({ label: i.itemText, value: Number(i.itemValue) }))
    }
  }).catch(() => {})
}

function loadMaintTypeOptions() {
  getDictItems('moni_vehicle_maint_type').then((res) => {
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || res.data || [])
    if (list.length) {
      maintTypeOptions.value = list.map((i) => ({ label: i.itemText, value: String(i.itemValue) }))
      list.forEach((i) => { MAINT_MAP[String(i.itemValue)] = i.itemText })
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

// ===== 维修保养（ISSUE-036）=====
const maintVisible = ref(false)
const maintSubmitting = ref(false)
const currentVehicle = ref(null)
const maintForm = reactive({ startDate: '', endDate: '', maintType: undefined, remark: '' })

function openMaintenance(record) {
  currentVehicle.value = record
  Object.assign(maintForm, { startDate: '', endDate: '', maintType: undefined, remark: '' })
  maintVisible.value = true
}

function disabledMaintDate(current) {
  // 不允许选择早于今天的日期
  return current && current < dayjs().startOf('day')
}

function submitMaintenance() {
  if (!maintForm.startDate || !maintForm.endDate) { message.warning('请选择开始与结束日期'); return }
  if (dayjs(maintForm.endDate).isBefore(maintForm.startDate)) { message.warning('结束日期须晚于开始日期'); return }
  if (!maintForm.maintType) { message.warning('请选择操作类型'); return }
  maintSubmitting.value = true
  const payload = {
    maintType: maintForm.maintType,
    startDate: maintForm.startDate + 'T00:00:00',
    endDate: maintForm.endDate + 'T23:59:59',
    remark: maintForm.remark
  }
  createVehicleMaintenance(currentVehicle.value.id, payload)
    .then(() => {
      message.success('已添加维修保养，车辆状态已更新')
      maintVisible.value = false
      load()
    })
    .catch(() => {})
    .finally(() => { maintSubmitting.value = false })
}

// ===== 车辆详情（ISSUE-036）=====
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailVehicleId = ref(null)

function openDetail(record) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  detailVehicleId.value = record.id
  getVehicleDetail(record.id)
    .then((res) => { detail.value = res.data || res })
    .catch(() => {})
    .finally(() => { detailLoading.value = false })
}

function removeMaintenance(record) {
  deleteVehicleMaintenance(record.id)
    .then(() => {
      message.success('已删除')
      if (detailVehicleId.value) openDetail({ id: detailVehicleId.value })
      load()
    })
    .catch(() => {})
}

function fmt(v) {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.format('YYYY-MM-DD') : String(v)
}

// ===== 车辆使用日历（ISSUE-035 / ISSUE-036）=====
function buildDays(start, end) {
  const list = []
  let cur = dayjs(start)
  const last = dayjs(end)
  if (!cur.isValid() || !last.isValid()) return list
  const dow = ['日', '一', '二', '三', '四', '五', '六']
  let idx = 0
  let guard = 0
  while ((cur.isBefore(last) || cur.isSame(last, 'day')) && guard < 366) {
    list.push({
      key: cur.format('YYYY-MM-DD'),
      date: cur.format('MM/DD'),
      dow: dow[cur.day()],
      start: cur.startOf('day'),
      end: cur.endOf('day'),
      idx: idx++
    })
    cur = cur.add(1, 'day')
    guard++
  }
  return list
}

function applyUnit() {
  if (calUnit.value === 'month') {
    const s = dayjs().startOf('month')
    const e = dayjs().endOf('month')
    calRange.value = [s.format('YYYY-MM-DD'), e.format('YYYY-MM-DD')]
  } else {
    const s = dayjs().startOf('week')
    const e = dayjs().endOf('week')
    calRange.value = [s.format('YYYY-MM-DD'), e.format('YYYY-MM-DD')]
  }
}

function onCalUnitChange() {
  applyUnit()
  days.value = buildDays(calRange.value[0], calRange.value[1])
  loadUsage()
}

function onViewChange() {
  if (viewMode.value === 'calendar') {
    days.value = buildDays(calRange.value[0], calRange.value[1])
    loadUsage()
  } else {
    load()
  }
}

function shiftRange(dir) {
  const start = calUnit.value === 'month'
    ? dayjs(calRange.value[0]).add(dir, 'month')
    : dayjs(calRange.value[0]).add(dir * 7, 'day')
  const end = calUnit.value === 'month'
    ? start.endOf('month')
    : start.add(6, 'day').endOf('day')
  calRange.value = [start.format('YYYY-MM-DD'), end.format('YYYY-MM-DD')]
  days.value = buildDays(calRange.value[0], calRange.value[1])
  loadUsage()
}

function loadUsage() {
  calendarLoading.value = true
  const start = dayjs(calRange.value[0]).startOf('day').format('YYYY-MM-DDTHH:mm:ss')
  const end = dayjs(calRange.value[1]).endOf('day').format('YYYY-MM-DDTHH:mm:ss')
  getVehicleUsage({ start, end })
    .then((res) => {
      const data = res.data || res || []
      usageList.value = Array.isArray(data) ? data : []
    })
    .catch(() => { usageList.value = [] })
    .finally(() => { calendarLoading.value = false })
}

// 某车在某天内的色块：派单(dispatch) + 维修保养(maint)
function blocksOf(vehicle, day) {
  const out = []
  const ranges = (vehicle.ranges || []).filter((r) => {
    const rs = dayjs(r.start); const re = dayjs(r.end)
    return rs.isBefore(day.end) && re.isAfter(day.start)
  })
  ranges.forEach((r) => {
    const rs = dayjs(r.start); const re = dayjs(r.end)
    const fullDay = !rs.isAfter(day.start) && !re.isBefore(day.end)
    out.push({
      key: 'd' + r.dispatchId,
      kind: 'dispatch',
      fullDay,
      tip: `派单#${r.dispatchId}（${r.status}）\n占用区间：${dayjs(r.start).format('MM-DD HH:mm')} ~ ${dayjs(r.end).format('MM-DD HH:mm')}`
    })
  })
  const maints = (vehicle.maintenances || []).filter((m) => {
    const ms = dayjs(m.start); const me = dayjs(m.end)
    return ms.isBefore(day.end) && me.isAfter(day.start)
  })
  maints.forEach((m) => {
    const ms = dayjs(m.start); const me = dayjs(m.end)
    const fullDay = !ms.isAfter(day.start) && !me.isBefore(day.end)
    out.push({
      key: 'm' + m.id,
      kind: 'maint',
      fullDay,
      tip: `维修保养（${maintTypeText(m.type)}）\n区间：${dayjs(m.start).format('MM-DD')} ~ ${dayjs(m.end).format('MM-DD')}`
    })
  })
  return out
}

onMounted(() => { loadStatusOptions(); loadMaintTypeOptions(); load() })
</script>

<style scoped>
.action-link { color: #1677ff; cursor: pointer; }
.action-link.danger { color: #ff4d4f; }

/* 车辆使用日历 */
.calendar-host { padding-top: 4px; }
.cal-toolbar { margin-bottom: 16px !important; }
.cal-tip { color: #888; font-size: 12px; display: inline-flex; align-items: center; gap: 4px; }
.cal-dot { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin: 0 4px 0 8px; vertical-align: middle; }
.cal-dot-dispatch { background: #409eff; }
.cal-dot-maint { background: #eb2f96; }
.cal-loading { padding: 40px 0; text-align: center; }
.cal-grid { border: 1px solid #f0f0f0; border-radius: 6px; overflow-x: auto; }
.cal-row { display: flex; border-bottom: 1px solid #f5f5f5; }
.cal-row:last-child { border-bottom: none; }
.cal-head { background: #fafafa; font-weight: 600; }
.cal-label {
  width: 140px; flex: 0 0 140px; padding: 6px 10px; border-right: 1px solid #f0f0f0;
  display: flex; flex-direction: column; justify-content: center;
  position: sticky; left: 0; z-index: 2; background: #fff;
}
.cal-head .cal-label { background: #fafafa; }
.cal-plate { font-weight: 600; }
.cal-model { font-size: 12px; color: #999; }
.cal-track { flex: 1 1 auto; display: flex; }
.cal-cell {
  flex: 1 1 0; min-width: 40px; border-right: 1px solid #f5f5f5; min-height: 40px;
  display: flex; align-items: center; justify-content: center; padding: 2px;
}
.cal-cell:last-child { border-right: none; }
.cal-cell-head { flex-direction: column; min-height: 44px; }
.cal-dow { font-size: 12px; color: #666; }
.cal-date { font-size: 13px; }
.cal-block {
  width: 100%; height: 28px; border-radius: 4px; color: #fff;
  font-size: 12px; display: flex; align-items: center; justify-content: center;
}
.cal-block-dispatch { background: #409eff; }
.cal-block-maint { background: #eb2f96; }
.cal-block-partial { opacity: 0.85; }
</style>
