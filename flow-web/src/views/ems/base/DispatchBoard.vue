<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">采样调度</span>
        <a-space>
          <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="onModeChange">
            <a-radio-button value="board">看板模式</a-radio-button>
            <a-radio-button value="table">表格模式</a-radio-button>
          </a-radio-group>
          <a-select
            v-model:value="groupBy"
            :disabled="mode === 'board'"
            placeholder="分组维度"
            style="width: 150px"
            size="small"
            @change="onGroupChange"
          >
            <a-select-option value="none">不分组</a-select-option>
            <a-select-option value="status">按状态分组</a-select-option>
            <a-select-option value="lead">按负责人分组</a-select-option>
          </a-select>
          <a-tooltip v-if="mode === 'board'">
            <template #title>看板模式已固定按状态分组，切换至表格模式可选择其他分组维度</template>
            <span class="hint">提示</span>
          </a-tooltip>
          <a-button size="small" @click="loadOrders">刷新</a-button>
        </a-space>
      </div>

      <!-- 看板模式：固定按状态分组 -->
      <div v-show="mode === 'board'" class="board table-host">
        <div v-for="col in boardColumns" :key="col.status" class="board-col">
          <div class="board-col-title" :style="{ background: col.color }">{{ col.title }} ({{ boardGrouped[col.status]?.length || 0 }})</div>
          <div class="board-col-body">
            <a-card
              v-for="order in boardGrouped[col.status] || []"
              :key="order.id"
              size="small"
              class="order-card"
              hoverable
              @click="openDispatch(order)"
            >
              <div class="order-no link" @click.stop="openDetail(order)">{{ order.orderNo }}</div>
              <div class="order-meta">点位ID: {{ order.pointId || '—' }}</div>
              <div class="order-meta">计划: {{ order.planDate || '—' }}</div>
              <div class="order-meta">负责人: {{ order.samplerLead || '—' }}</div>
            </a-card>
            <a-empty v-if="!(boardGrouped[col.status] || []).length" description="无" :image="simpleImage" />
          </div>
        </div>
      </div>

      <!-- 表格模式：不分组 -->
      <div v-show="mode === 'table' && groupBy === 'none'" class="tbl-box table-host">
        <a-table
          :columns="orderCols"
          :data-source="orders"
          :loading="loading"
          :scroll="{ y: scrollY }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
            </template>
            <template v-else-if="column.key === 'op'">
              <a-space>
                <a-button v-if="record.status === '待派单' && hasPerm('ems:dispatch')" type="link" size="small" @click="openDispatch(record)">派单</a-button>
                <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
              </a-space>
            </template>
          </template>
          <template #pagination>
            <a-pagination
              v-model:current="tablePage.current"
              v-model:pageSize="tablePage.pageSize"
              :total="orders.length"
              show-size-changer
              show-quick-jumper
              :page-size-options="['10','20','50']"
              style="margin: 8px 0 16px"
            />
          </template>
        </a-table>
      </div>

      <!-- 表格模式：分组（组内独立分页） -->
      <div v-show="mode === 'table' && groupBy !== 'none'" class="group-scroll table-host">
        <a-card
          v-for="g in tableGroups"
          :key="g.key"
          size="small"
          class="group-card"
        >
          <template #title>
            <span class="group-title">{{ g.label }}</span>
            <a-tag :color="g.color || 'default'" style="margin-left: 8px">{{ g.list.length }}</a-tag>
          </template>
          <a-table
            :columns="orderCols"
            :data-source="g.list"
            size="small"
            :scroll="{ y: scrollY }"
            :pagination="groupPagination(g.key)"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'op'">
                <a-space>
                  <a-button v-if="record.status === '待派单' && hasPerm('ems:dispatch')" type="link" size="small" @click="openDispatch(record)">派单</a-button>
                  <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
        <a-empty v-if="!tableGroups.length" description="暂无数据" :image="simpleImage" />
      </div>
    </div>

    <!-- 派单抽屉 -->
    <a-drawer
      v-model:open="dispatchVisible"
      title="调度派单"
      width="520"
      :confirm-loading="dispatchLoading"
      @close="dispatchVisible = false"
    >
      <template v-if="currentOrder">
        <a-alert :message="'订单：' + currentOrder.orderNo" type="info" style="margin-bottom: 12px" />
        <a-form layout="vertical">
          <a-form-item label="负责人（后台人员）">
            <a-select
              v-model:value="dispatchForm.leadId"
              show-search
              placeholder="搜索并选择负责人"
              :options="employeeOptions"
              :filter-option="false"
              @search="onSearchEmployee"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="组员（后台人员，可多选）">
            <a-select
              v-model:value="dispatchForm.empIds"
              mode="multiple"
              show-search
              placeholder="搜索并选择组员"
              :options="employeeOptions"
              :filter-option="false"
              @search="onSearchEmployee"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="车辆">
            <a-select
              v-model:value="dispatchForm.vehicleId"
              show-search
              placeholder="搜索并选择车辆"
              :options="vehicleOptions"
              :filter-option="false"
              @search="onSearchVehicle"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="设备（可多选）">
            <a-select
              v-model:value="dispatchForm.instrumentIds"
              mode="multiple"
              show-search
              placeholder="搜索并选择设备"
              :options="instrumentOptions"
              :filter-option="false"
              @search="onSearchInstrument"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="计划开始">
            <a-date-picker v-model:value="dispatchForm.planStart" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </a-form-item>
          <a-form-item label="计划结束">
            <a-date-picker v-model:value="dispatchForm.planEnd" show-time format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="dispatchForm.note" :rows="2" />
          </a-form-item>
        </a-form>
      </template>
      <template #footer>
        <a-space>
          <a-button @click="dispatchVisible = false">取消</a-button>
          <a-button type="primary" :loading="dispatchLoading" @click="submitDispatch">派单</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 派单详情 -->
    <DispatchDetail
      :open="detailVisible"
      :order-id="detailOrderId"
      @close="detailVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { Empty } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  getSamplingOrders, createDispatch, searchUsers, listVehicles, listInstruments
} from '../../../api/ems'
import DispatchDetail from './DispatchDetail.vue'
import { usePermission } from '../../../composables/usePermission'

const { hasPerm } = usePermission()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const orders = ref([])
const loading = ref(false)
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const currentOrder = ref(null)
const detailVisible = ref(false)
const detailOrderId = ref(null)
const employeeOptions = ref([])
const vehicleOptions = ref([])
const instrumentOptions = ref([])

// 模式与分组维度（默认表格模式，便于分组选择）
const mode = ref('table') // board | table
const groupBy = ref('none') // none | status | lead

function onModeChange() {
  nextTick(syncTableHeight)
}
function onGroupChange() {
  nextTick(syncTableHeight)
}

// 看板列（固定按状态）
const boardColumns = [
  { status: '待派单', title: '待派单', color: '#F59E0B' },
  { status: '已派单', title: '已派单', color: '#2563EB' },
  { status: '采样执行中', title: '执行中', color: '#0EA5E9' },
  { status: '样品送检', title: '已送检', color: '#16A34A' }
]

// 状态颜色映射（用于表格 tag）
const STATUS_COLOR = {
  '待派单': 'orange',
  '已派单': 'blue',
  '采样执行中': 'cyan',
  '样品送检': 'green',
  '实验室检测中': 'purple',
  '报告编制': 'geekblue',
  '归档完成': 'default'
}
function statusColor(s) {
  return STATUS_COLOR[s] || 'default'
}

// 表格列
const orderCols = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '点位ID', dataIndex: 'pointId', key: 'pointId', width: 100 },
  { title: '计划采样日', dataIndex: 'planDate', key: 'planDate', width: 130 },
  { title: '负责人', dataIndex: 'samplerLead', key: 'samplerLead', width: 120 },
  { title: '状态', key: 'status', width: 130 },
  { title: '操作', key: 'op', width: 120, fixed: 'right' }
]

// 看板分组
const boardGrouped = computed(() => {
  const g = {}
  for (const col of boardColumns) g[col.status] = []
  for (const o of orders.value) {
    if (g[o.status]) g[o.status].push(o)
  }
  return g
})

// 表格分组
const tableGroups = computed(() => {
  if (groupBy.value === 'none') return []
  const map = new Map()
  for (const o of orders.value) {
    let key
    if (groupBy.value === 'status') {
      key = o.status || '未知'
    } else {
      key = o.samplerLead || '未分配'
    }
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(o)
  }
  return Array.from(map.entries()).map(([key, list]) => ({
    key,
    label: groupBy.value === 'status' ? (key) : (`负责人：${key}`),
    color: groupBy.value === 'status' ? statusColor(key) : 'blue',
    list
  }))
})

// 表格单表分页
const tablePage = reactive({ current: 1, pageSize: 10 })

// 分组组内分页：current 状态按组 key 维护
const groupCurrent = reactive({})
function groupPagination(key) {
  if (!groupCurrent[key]) groupCurrent[key] = 1
  const list = (tableGroups.value.find((g) => g.key === key) || {}).list || []
  return {
    current: groupCurrent[key],
    pageSize: 10,
    total: list.length,
    showSizeChanger: false,
    showQuickJumper: false,
    onChange: (p) => { groupCurrent[key] = p }
  }
}

// 表格动态高度（单表与分组模式共用）
const scrollY = ref(420)
function syncTableHeight() {
  // 多个视图都用 .table-host（v-show 共存），需找到当前可见的那个
  const hosts = document.querySelectorAll('.page-wrap .table-host')
  let box = null
  hosts.forEach((el) => { if (el.offsetParent !== null) box = el })
  if (!box) return // 无可见表格视图
  const boxRect = box.getBoundingClientRect()
  const headerEl = box.querySelector('.ant-table-thead')
  const headerH = headerEl ? headerEl.getBoundingClientRect().height : 40
  const pagEl = box.querySelector('.ant-table-pagination:not([style*="display: none"])')
  const pagH = pagEl ? pagEl.getBoundingClientRect().height : 32
  // 表格内容区 = 容器高度 - 表头 - 分页 - 间距，精确填满，底部无留白
  const h = boxRect.height - headerH - pagH - 12
  scrollY.value = h > 200 ? Math.floor(h) : 200
}
let pageObserver = null

// 切换模式/分组维度后重算高度，保证视图流畅稳定
watch([mode, groupBy], () => {
  nextTick(syncTableHeight)
})

const dispatchForm = reactive({
  leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: ''
})

function loadOrders() {
  loading.value = true
  getSamplingOrders({}).then((res) => {
    const data = res.data || res
    orders.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {}).finally(() => { loading.value = false; nextTick(syncTableHeight) })
}

function loadResources() {
  onSearchEmployee('')
  onSearchVehicle('')
  onSearchInstrument('')
}

// 负责人/组员：从后台用户管理远程检索
function onSearchEmployee(keyword) {
  searchUsers({ keyword: keyword || undefined, page: 1, size: 20 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    employeeOptions.value = list.map((u) => ({
      label: `${u.realName || u.username}（${u.username}）`,
      value: u.id
    }))
  }).catch(() => {})
}

function onSearchVehicle(keyword) {
  listVehicles({ keyword: keyword || undefined, page: 1, size: 50 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    vehicleOptions.value = list.map((v) => ({ label: `${v.plateNo}${v.model ? ' ' + v.model : ''}`, value: v.id }))
  }).catch(() => {})
}

function onSearchInstrument(keyword) {
  listInstruments({ keyword: keyword || undefined, page: 1, size: 50 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    instrumentOptions.value = list.map((i) => ({ label: `${i.name}${i.model ? ' ' + i.model : ''}`, value: i.id }))
  }).catch(() => {})
}

function openDetail(order) {
  detailOrderId.value = order.id
  detailVisible.value = true
}

function openDispatch(order) {
  if (!hasPerm('ems:dispatch')) {
    message.warning('无派单权限')
    return
  }
  if (order.status !== '待派单') {
    message.info('仅「待派单」状态可派单')
    return
  }
  currentOrder.value = order
  Object.assign(dispatchForm, { leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: '' })
  dispatchVisible.value = true
}

function submitDispatch() {
  if (!dispatchForm.leadId) {
    message.warning('请选择负责人')
    return
  }
  const empIds = (dispatchForm.empIds || []).filter((id) => id !== dispatchForm.leadId)
  dispatchLoading.value = true
  createDispatch({
    orderId: currentOrder.value.id,
    vehicleId: dispatchForm.vehicleId,
    leadId: dispatchForm.leadId,
    empIds,
    instrumentIds: dispatchForm.instrumentIds,
    planStart: dispatchForm.planStart ? dayjs(dispatchForm.planStart).format('YYYY-MM-DDTHH:mm:ss') : null,
    planEnd: dispatchForm.planEnd ? dayjs(dispatchForm.planEnd).format('YYYY-MM-DDTHH:mm:ss') : null,
    note: dispatchForm.note
  }).then(() => {
    message.success('派单成功（已通过资质闸门与冲突校验）')
    dispatchVisible.value = false
    loadOrders()
  }).catch((e) => {
    message.error((e?.response?.data?.message) || '派单失败')
  }).finally(() => { dispatchLoading.value = false })
}

onMounted(() => {
  loadOrders()
  loadResources()
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
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.page-title { font-size: 16px; font-weight: 600; }

.board { display: flex; gap: 12px; overflow-x: auto; flex: 1; min-height: 0; }
.board-col { flex: 1; min-width: 200px; background: #fafafa; border-radius: 8px; padding: 8px; display: flex; flex-direction: column; }
.board-col-title { color: #fff; padding: 6px 10px; border-radius: 6px; font-weight: 600; flex: 0 0 auto; }
.board-col-body { margin-top: 8px; min-height: 120px; overflow-y: auto; flex: 1; }
.order-card { margin-bottom: 8px; }
.order-no { font-weight: 600; }
.order-no.link { color: #2563EB; cursor: pointer; }
.order-no.link:hover { text-decoration: underline; }
.order-meta { color: #888; font-size: 12px; }

.tbl-box { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.tbl-box :deep(.ant-table-wrapper) { flex: 1 1 auto; min-height: 0; height: 100%; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-nested-loading) { flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-spin-container) { flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table) { flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-container) { flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column; }
.tbl-box :deep(.ant-table-body) { flex: 1 1 auto; min-height: 0; }
.tbl-box :deep(.ant-table-pagination) { margin: 8px 0 0 !important; flex: 0 0 auto; }

.group-scroll { flex: 1; min-height: 0; overflow-y: auto; padding-right: 4px; }
.group-card { margin-bottom: 12px; }
.group-title { font-weight: 600; }
.hint { color: #999; font-size: 12px; border: 1px dashed #d9d9d9; border-radius: 4px; padding: 1px 6px; }
.table-host { animation: viewFade 0.18s ease; }
@keyframes viewFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
