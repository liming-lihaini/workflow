<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">采样调度</span>
        <a-space>
          <a-radio-group v-model:value="mode" button-style="solid"  @change="onModeChange">
            <a-radio-button value="board">看板模式</a-radio-button>
            <a-radio-button value="table">表格模式</a-radio-button>
          </a-radio-group>
          <a-select
            v-model:value="groupBy"
            :disabled="mode === 'board'"
            placeholder="分组维度"
            style="width: 150px"
            
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
          <a-button  :loading="loading" @click="loadOrders">刷新</a-button>
        </a-space>
      </div>

      <!-- 筛选栏：订单号 / 负责人 / 状态 -->
      <div class="filter-bar">
        <a-input
          v-model:value="filters.orderNo"
          placeholder="订单号（模糊）"
          allow-clear
          style="width: 180px"
          @press-enter="loadOrders"
        />
        <a-input
          v-model:value="filters.leadName"
          placeholder="负责人（模糊）"
          allow-clear
          style="width: 160px"
          @press-enter="loadOrders"
        />
        <a-select
          v-model:value="filters.status"
          placeholder="状态"
          allow-clear
          style="width: 150px"
          @change="loadOrders"
        >
          <a-select-option v-for="s in ORDER_STATUS_CHAIN" :key="s" :value="s">{{ s }}</a-select-option>
        </a-select>
        <a-button type="primary"  :loading="loading" @click="loadOrders">查询</a-button>
        <a-button  @click="resetFilters">重置</a-button>
        <a-divider type="vertical" />
        <a-button
          type="primary"
          
          :disabled="!selectedOrderIds.length"
          @click="openBatchDispatch"
        >批量派单{{ selectedOrderIds.length ? `(${selectedOrderIds.length})` : '' }}</a-button>
      </div>

      <!-- 看板模式：固定按状态分组 -->
      <div v-show="mode === 'board'" class="board table-host">
        <div v-for="col in boardColumns" :key="col.status" class="board-col">
          <div class="board-col-title" :style="{ background: col.color }">{{ col.title }} ({{ boardGrouped[col.status]?.length || 0 }})</div>
          <div class="board-col-body">
            <a-card
              v-for="order in boardGrouped[col.status] || []"
              :key="order.id"
              
              class="order-card"
              hoverable
              @click="openDispatch(order)"
            >
              <div class="order-no link" @click.stop="openDetail(order)">{{ order.orderNo }}</div>
              <div class="order-meta">点位: {{ order.pointName || '—' }}</div>
              <div class="order-meta">计划: {{ order.planRange || '—' }}</div>
              <div class="order-meta">负责人: {{ order.leadName || '—' }}</div>
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
          :row-selection="rowSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <span class="status-chain">
                <template v-for="(s, idx) in statusSteps(record)" :key="s.label">
                  <span v-if="idx" class="chain-sep">-</span>
                  <span :class="['chain-node', 'chain-' + s.state]">{{ s.label }}</span>
                </template>
              </span>
            </template>
            <template v-else-if="column.key === 'op'">
              <a-space>
                <a-button v-if="record.status === '待派单' && hasPerm('ems:dispatch')" type="link"  @click="openDispatch(record)">派单</a-button>
                <a-button type="link"  @click="openDetail(record)">详情</a-button>
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
          
          class="group-card"
        >
          <template #title>
            <span class="group-title">{{ g.label }}</span>
            <a-tag :color="g.color || 'default'" style="margin-left: 8px">{{ g.list.length }}</a-tag>
          </template>
          <a-table
            :columns="orderCols"
            :data-source="g.list"
            
            :scroll="{ y: scrollY }"
            :pagination="groupPagination(g.key)"
            row-key="id"
            :row-selection="rowSelection"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'op'">
                <a-space>
                  <a-button v-if="record.status === '待派单' && hasPerm('ems:dispatch')" type="link"  @click="openDispatch(record)">派单</a-button>
                  <a-button type="link"  @click="openDetail(record)">详情</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
        <a-empty v-if="!tableGroups.length" description="暂无数据" :image="simpleImage" />
      </div>
    </div>

    <!-- 派单抽屉（单订单 / 批量共用） -->
    <a-drawer
      v-model:open="dispatchVisible"
      :title="isBatch ? `批量派单（共 ${selectedOrderIds.length} 个订单）` : '调度派单'"
      width="1000"
      :confirm-loading="dispatchLoading"
      @close="closeDispatch"
    >
      <template v-if="!isBatch && currentOrder">
        <a-alert :message="'订单：' + currentOrder.orderNo" type="info" style="margin-bottom: 12px" />
      </template>
      <template v-else>
        <a-alert :message="`批量派单：共选中 ${selectedOrderIds.length} 个「待派单」订单，将统一使用以下派单信息`" type="info" style="margin-bottom: 12px" />
      </template>
      <DispatchForm
        ref="dispatchFormRef"
        :form="dispatchForm"
        :employee-options="employeeOptions"
        :vehicle-options="vehicleOptions"
        :instrument-options="instrumentOptions"
      />
      <a-alert
        v-if="dispatchBlock"
        type="error"
        show-icon
        style="margin-top: 16px;"
        :message="`派单被阻断，请调整后重试（共 ${dispatchBlock.split('\n').filter(Boolean).length} 个订单无法派单）`"
        :description="h('div', { style: 'white-space: pre-line; line-height: 1.6;' }, dispatchBlock)"
      />
      <template #footer>
        <a-space>
          <a-button @click="closeDispatch">取消</a-button>
          <a-button type="primary" :loading="dispatchLoading" @click="submitDispatch">{{ isBatch ? '批量派单' : '派单' }}</a-button>
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
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { message } from 'ant-design-vue'
import { Empty } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  getDispatchBoardList, createDispatch, batchDispatch, searchUsers, listVehicles, listInstruments, getAvailableVehicles
} from '../../../api/ems'
import DispatchDetail from './DispatchDetail.vue'
import DispatchForm from './DispatchForm.vue'
import { usePermission } from '../../../composables/usePermission'

const { hasPerm } = usePermission()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const orders = ref([])
const loading = ref(false)
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchBlock = ref('') // 批量派单阻断提示（订单号+原因），有值时保留表单不关闭
const currentOrder = ref(null)
const isBatch = ref(false)
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

// 筛选条件：订单号 / 负责人 / 状态
const filters = reactive({ orderNo: '', leadName: '', status: undefined })
function resetFilters() {
  filters.orderNo = ''
  filters.leadName = ''
  filters.status = undefined
  loadOrders()
}

// 表格行多选（批量派单）
const selectedRowKeys = ref([])
const selectedOrderIds = computed(() => selectedRowKeys.value)
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys) => { selectedRowKeys.value = keys },
  // 仅允许「待派单」订单被选入批量派单
  getCheckboxProps: (record) => ({ disabled: record.status !== '待派单' })
}))
function clearSelection() {
  selectedRowKeys.value = []
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

// 采样订单状态全过程（A - B - C - D ...）
const ORDER_STATUS_CHAIN = [
  '待派单', '已派单', '采样执行中', '样品送检', '实验室检测中', '报告编制', '归档完成'
]
// 每个状态节点的展示态：done(已完成,绿色) / current(当前,蓝色) / todo(未到达,灰色)
function statusNodeState(step, current) {
  const ci = ORDER_STATUS_CHAIN.indexOf(current)
  const si = ORDER_STATUS_CHAIN.indexOf(step)
  if (ci < 0) return 'todo'
  if (si < ci) return 'done'
  if (si === ci) return 'current'
  return 'todo'
}
// 渲染状态全过程 A - B - C - D ...
function statusSteps(record) {
  return ORDER_STATUS_CHAIN.map((step) => ({ label: step, state: statusNodeState(step, record.status) }))
}

// 表格列
const orderCols = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 140 },
  { title: '点位名称', dataIndex: 'pointName', key: 'pointName', width: 150 },
  { title: '计划区间', dataIndex: 'planRange', key: 'planRange', width: 180 },
  { title: '负责人', dataIndex: 'leadName', key: 'leadName', width: 80 },
  { title: '状态', key: 'status', width: 460 },
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
      key = o.leadName || '未分配'
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

// 派单必填校验由 DispatchForm 内部 rules 负责，此处仅持有表单 ref
const dispatchFormRef = ref(null)

function loadOrders() {
  loading.value = true
  const params = {}
  if (filters.orderNo && filters.orderNo.trim()) params.orderNo = filters.orderNo.trim()
  if (filters.leadName && filters.leadName.trim()) params.leadName = filters.leadName.trim()
  if (filters.status) params.status = filters.status
  getDispatchBoardList(params).then((res) => {
    const data = res.data || res
    orders.value = Array.isArray(data) ? data : (data.list || [])
    clearSelection()
  }).catch(() => {}).finally(() => { loading.value = false; nextTick(syncTableHeight) })
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
  isBatch.value = false
  currentOrder.value = order
  Object.assign(dispatchForm, { leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: '' })
  dispatchVisible.value = true
}

// 批量派单：对当前选中的「待派单」订单统一派发同一组派单信息
function openBatchDispatch() {
  if (!hasPerm('ems:dispatch')) {
    message.warning('无派单权限')
    return
  }
  if (!selectedOrderIds.value.length) {
    message.warning('请先勾选「待派单」订单')
    return
  }
  isBatch.value = true
  currentOrder.value = null
  dispatchBlock.value = ''
  Object.assign(dispatchForm, { leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: '' })
  dispatchVisible.value = true
}

function closeDispatch() {
  dispatchVisible.value = false
  isBatch.value = false
  currentOrder.value = null
}

async function submitDispatch() {
  // 必填校验：负责人、组员、计划开始、计划结束
  try {
    await dispatchFormRef.value.validate()
  } catch (e) {
    return
  }
  // 双保险：校验所选车辆是否在检车时间区间内可用（ISSUE-035）
  if (dispatchForm.vehicleId && dispatchForm.planStart && dispatchForm.planEnd) {
    const ps = dayjs(dispatchForm.planStart).format('YYYY-MM-DDTHH:mm:ss')
    const pe = dayjs(dispatchForm.planEnd).format('YYYY-MM-DDTHH:mm:ss')
    const availablePromise = getAvailableVehicles({ planStart: ps, planEnd: pe })
    dispatchLoading.value = true
    availablePromise.then((res) => {
      const ids = res.data || res || []
      if (!ids.includes(dispatchForm.vehicleId)) {
        dispatchLoading.value = false
        message.error('所选车辆在检车时间区间内已被占用，请在时间范围内选择可用车辆')
        if (dispatchFormRef.value && dispatchFormRef.value.reloadVehicles) dispatchFormRef.value.reloadVehicles()
        return
      }
      doSubmitDispatch()
    }).catch(() => { dispatchLoading.value = false })
    return
  }
  dispatchLoading.value = true
  doSubmitDispatch()
}

// 组装派单参数（单订单 / 批量共用）
function buildDispatchParams() {
  const empIds = (dispatchForm.empIds || []).filter((id) => id !== dispatchForm.leadId)
  return {
    vehicleId: dispatchForm.vehicleId,
    leadId: dispatchForm.leadId,
    empIds,
    instrumentIds: dispatchForm.instrumentIds,
    planStart: dispatchForm.planStart ? dayjs(dispatchForm.planStart).format('YYYY-MM-DDTHH:mm:ss') : null,
    planEnd: dispatchForm.planEnd ? dayjs(dispatchForm.planEnd).format('YYYY-MM-DDTHH:mm:ss') : null,
    note: dispatchForm.note
  }
}

function doSubmitDispatch() {
  const params = buildDispatchParams()
  dispatchLoading.value = true
  if (isBatch.value) {
    // 批量派单：同一组派单信息依次派发到所有选中的「待派单」订单
    batchDispatch({ ...params, orderIds: selectedOrderIds.value }).then((res) => {
      // 仅当全部成功时关闭表单（后端此时返回 code=200）
      const data = res.data || res
      const success = data.successCount || 0
      message.success(`批量派单成功，共 ${success} 个订单`)
      dispatchBlock.value = ''
      closeDispatch()
      loadOrders()
    }).catch((err) => {
      // 后端监测到阻断（资源冲突/资质闸门/维修保养等）时返回异常状态值，
      // 并携带 failList 明细（[{orderId, reason}]）。此时保留表单，展示阻断提示，不关闭。
      const failList = err && err.res && err.res.data ? err.res.data.failList : null
      if (failList && failList.length) {
        const lines = failList.map((f) => `订单 ${f.orderId}：${f.reason}`)
        dispatchBlock.value = lines.join('\n')
      } else {
        // 其他系统错误：由全局拦截器已提示，阻断区展示通用信息
        dispatchBlock.value = (err && err.res && err.res.message) || (err && err.message) || '派单失败，请重试'
      }
      // 注意：阻断时务必不要调用 closeDispatch()，保持表单打开
    }).finally(() => { dispatchLoading.value = false })
  } else {
    createDispatch({ ...params, orderId: currentOrder.value.id }).then(() => {
      message.success('派单成功（已通过资质闸门与冲突校验）')
      closeDispatch()
      loadOrders()
    }).catch(() => {
      // 冲突等错误提示已由全局响应拦截器统一多行展示，此处仅做加载状态清理
    }).finally(() => { dispatchLoading.value = false })
  }
}

onMounted(() => {
  loadOrders()
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

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

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
.vehicle-tip { margin-top: 6px; }
.vehicle-tip .tip-text { color: #52c41a; font-size: 12px; }

/* 状态全过程 A - B - C - D：当前蓝色、完成绿色、未到达灰色 */
.status-chain { display: inline-flex; align-items: center; flex-wrap: wrap; line-height: 20px; }
.chain-node { font-size: 12px; padding: 0 2px; border-radius: 4px; }
.chain-done { color: #52c41a; }      /* 已完成：绿色 */
.chain-current { color: #1677ff; font-weight: 600; }  /* 当前：蓝色 */
.chain-todo { color: #bbb; }          /* 未到达：灰色 */
.chain-sep { color: #ccc; margin: 0 2px; }
</style>
