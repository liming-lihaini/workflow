<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">采样任务</span>
        <a-space>
          <a-select
            v-model:value="groupBy"
            placeholder="分组维度"
            style="width: 150px"
            @change="onGroupChange"
          >
            <a-select-option value="none">不分组</a-select-option>
            <a-select-option value="status">按状态分组</a-select-option>
            <a-select-option value="lead">按负责人分组</a-select-option>
          </a-select>
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
        <a-select
          v-model:value="filters.leadName"
          placeholder="负责人（模糊）"
          allow-clear
          show-search
          style="width: 160px"
          :options="leadOptions"
          @change="loadOrders"
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
      </div>

      <!-- 状态统计卡片 + 逾期卡片 -->
      <div class="stat-cards">
        <div
          v-for="s in statusStats"
          :key="s.status"
          class="stat-card"
          :class="{ active: !overdueOnly && filters.status === s.status }"
          :style="{
            background: s.grad,
            borderColor: (!overdueOnly && filters.status === s.status) ? s.hexColor : 'transparent'
          }"
          @click="applyStatusFilter(s.status)"
        >
          <div class="stat-icon" :style="{ color: s.hexColor, background: s.iconBg }">
            <component :is="s.icon" />
          </div>
          <div class="stat-info">
            <div class="stat-num" :style="{ color: s.hexColor }">{{ s.count }}</div>
            <div class="stat-label">{{ s.status }}</div>
          </div>
        </div>
        <div
          class="stat-card overdue"
          :class="{ active: overdueOnly }"
          @click="toggleOverdue"
        >
          <div class="stat-icon">
            <WarningOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ overdueCount }}</div>
            <div class="stat-label">逾期订单</div>
          </div>
        </div>
      </div>

      <!-- 表格模式：不分组 -->
      <div v-show="groupBy === 'none'" class="tbl-box table-host">
        <a-table
          :columns="orderCols"
          :data-source="displayOrders"
          :loading="loading"
          :scroll="{ y: scrollY }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'orderNo'">
              <span class="link-text" @click="openDetail(record)">{{ record.orderNo }}</span>
            </template>
            <template v-else-if="column.key === 'entrustName'">
              <span class="link-text" @click="goEntrust(record)">{{ record.entrustName || '—' }}</span>
            </template>
            <template v-else-if="column.key === 'entrustNo'">
              <span class="link-text" @click="goEntrust(record)">{{ record.entrustNo || '—' }}</span>
            </template>
            <template v-else-if="column.key === 'pointCount'">
              {{ record.pointCount ?? 0 }}
            </template>
            <template v-else-if="column.key === 'createBy'">
              {{ record.createName || record.createBy || '—' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
            </template>
            <template v-else-if="column.key === 'op'">
              <a-space>
                <span v-if="record.status === '待派单' && hasPerm('ems:dispatch:assign')" class="action-link" @click="openDispatch(record)">派单</span>
                <span v-if="record.status === '已派单' && hasPerm('ems:sample:create')" class="action-link" @click="goCollect(record)">样品登记</span>
                <span v-if="canEdit(record) && hasPerm('ems:dispatch:update')" class="action-link" @click="openEdit(record)">编辑</span>
                <span v-if="canComplete(record) && hasPerm('ems:dispatch:update')" class="action-link" @click="openComplete(record)">完成</span>
                <span class="action-link" @click="openDetail(record)">详情</span>
                <a-popconfirm v-if="hasPerm('ems:dispatch:delete')" title="确认删除该采样任务？关联派单将一并清除。" @confirm="handleDeleteOrder(record)">
                  <span class="action-link danger" @click.stop>删除</span>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
          <template #pagination>
            <a-pagination
              v-model:current="tablePage.current"
              v-model:pageSize="tablePage.pageSize"
              :total="displayOrders.length"
              show-size-changer
              show-quick-jumper
              :page-size-options="['10','20','50']"
              style="margin: 8px 0 16px"
            />
          </template>
        </a-table>
      </div>

      <!-- 表格模式：分组（组内独立分页） -->
      <div v-show="groupBy !== 'none'" class="group-scroll table-host">
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
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'orderNo'">
                <span class="link-text" @click="openDetail(record)">{{ record.orderNo }}</span>
              </template>
              <template v-else-if="column.key === 'entrustName'">
                <span class="link-text" @click="goEntrust(record)">{{ record.entrustName || '—' }}</span>
              </template>
              <template v-else-if="column.key === 'entrustNo'">
                <span class="link-text" @click="goEntrust(record)">{{ record.entrustNo || '—' }}</span>
              </template>
              <template v-else-if="column.key === 'pointCount'">
                {{ record.pointCount ?? 0 }}
              </template>
              <template v-else-if="column.key === 'createBy'">
                {{ record.createName || record.createBy || '—' }}
              </template>
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.key === 'op'">
                <a-space>
                  <span v-if="record.status === '待派单' && hasPerm('ems:dispatch:assign')" class="action-link" @click="openDispatch(record)">派单</span>
                  <span v-if="record.status === '已派单' && hasPerm('ems:sample:create')" class="action-link" @click="goCollect(record)">样品登记</span>
                  <span v-if="canEdit(record) && hasPerm('ems:dispatch:update')" class="action-link" @click="openEdit(record)">编辑</span>
                  <span v-if="canComplete(record) && hasPerm('ems:dispatch:update')" class="action-link" @click="openComplete(record)">完成</span>
                  <span class="action-link" @click="openDetail(record)">详情</span>
                  <a-popconfirm v-if="hasPerm('ems:dispatch:delete')" title="确认删除该采样任务？关联派单将一并清除。" @confirm="handleDeleteOrder(record)">
                    <span class="action-link danger" @click.stop>删除</span>
                  </a-popconfirm>
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
      :title="editingDispatchId ? '编辑派单' : '调度派单'"
      width="1000"
      :confirm-loading="dispatchLoading"
      @close="closeDispatch"
    >
      <template v-if="currentOrder">
        <a-alert :message="'订单：' + currentOrder.orderNo" type="info" style="margin-bottom: 12px" />
      </template>
      <DispatchForm
        ref="dispatchFormRef"
        :form="dispatchForm"
        :employee-options="employeeOptions"
        :vehicle-options="vehicleOptions"
        :instrument-options="instrumentOptions"
        :exclude-dispatch-id="editingDispatchId"
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
          <a-button type="primary" :loading="dispatchLoading" @click="submitDispatch">{{ editingDispatchId ? '保存' : '派单' }}</a-button>
        </a-space>
      </template>
    </a-drawer>

    <!-- 完成确认弹窗：实际完成时间 + 完成描述（富文本） -->
    <a-modal
      v-model:open="completeVisible"
      title="完成确认"
      :width="800"
      :confirm-loading="completeLoading"
      ok-text="确定"
      cancel-text="取消"
      @ok="submitComplete"
      @cancel="completeVisible = false"
    >
      <template v-if="currentOrder">
        <a-alert :message="'订单：' + currentOrder.orderNo + (currentOrder.entrustName ? '（' + currentOrder.entrustName + '）' : '')" type="info" style="margin-bottom: 12px" />
      </template>
      <a-form :label-col="{ style: { width: '110px' } }">
        <a-form-item label="实际完成时间" required>
          <a-date-picker
            v-model:value="completeForm.actualFinishTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            placeholder="请选择实际完成时间"
          />
        </a-form-item>
        <a-form-item label="完成描述">
          <RichTextEditor v-model:value="completeForm.finishDesc" placeholder="请输入完成描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick, h } from 'vue'
import { message } from 'ant-design-vue'
import { Empty } from 'ant-design-vue'
import {
  ClockCircleOutlined, CaretRightOutlined, CheckCircleOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import {
  getDispatchBoardList, createDispatch, updateDispatch, getDispatchDetail, searchUsers, listVehicles, listInstruments, getAvailableVehicles, deleteSamplingOrder,
  completeSamplingOrder
} from '../../../api/ems'
import DispatchForm from './DispatchForm.vue'
import RichTextEditor from '../../../components/RichTextEditor.vue'
import { usePermission } from '../../../composables/usePermission'
import { useUserStore } from '../../../stores/user'

const { hasPerm } = usePermission()
const userStore = useUserStore()
const router = useRouter()
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

// 跳转委托单详情
function goEntrust(order) {
  if (order && order.entrustId) {
    router.push({ path: '/ems/base/entrust', query: { detailId: order.entrustId, tab: 'base' } })
  }
}

// 已派单任务跳转样品管理并自动打开登记表单（携带派单ID用于回填）
function goCollect(order) {
  router.push(`/ems/base/sample?open=collect&dispatchId=${order.id}`)
}

const orders = ref([])        // 当前展示的订单（受卡片筛选影响）
const allOrders = ref([])      // 全量订单（不受卡片筛选影响，用于卡片统计）
const loading = ref(false)
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchBlock = ref('') // 批量派单阻断提示（订单号+原因），有值时保留表单不关闭
const currentOrder = ref(null)
const editingDispatchId = ref(null) // 编辑模式：正在编辑的派单记录 id（null = 新建派单）
const employeeOptions = ref([])
const vehicleOptions = ref([])
const instrumentOptions = ref([])

// 分组维度（none | status | lead）
const groupBy = ref('none')

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

// 状态颜色映射（用于表格 tag）
const STATUS_COLOR = {
  '待派单': 'orange',
  '已派单': 'blue',
  '已完成': 'green',
}
function statusColor(s) {
  return STATUS_COLOR[s] || 'default'
}
// 统计卡片视觉规范（参考设计稿）：渐变底 + 圆形图标底 + 状态色数字
const STATUS_STYLE = {
  '待派单': { hexColor: '#FFB84C', grad: 'linear-gradient(90deg, #fffbeb, #fefce8)', iconBg: '#fef3c7' },
  '已派单': { hexColor: '#69b1ff', grad: 'linear-gradient(90deg, #f0f9ff, #eff6ff)', iconBg: '#e0f2fe' },
  '已完成': { hexColor: '#73d13d', grad: 'linear-gradient(90deg, #f0fdf4, #f7fee7)', iconBg: '#dcfce7' }
}
const DEFAULT_STATUS_STYLE = { hexColor: '#8c8c8c', grad: 'linear-gradient(90deg, #fafafa, #f5f5f5)', iconBg: '#f0f0f0' }
// 状态图标映射（统计卡片展示）
const STATUS_ICON = {
  '待派单': ClockCircleOutlined,
  '已派单': CaretRightOutlined,
  '已完成': CheckCircleOutlined
}

// 采样任务状态全过程（用于筛选下拉）
const ORDER_STATUS_CHAIN = [
  '待派单', '已派单', '已完成'
]

// 表格列
const orderCols = [
  { title: '任务单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '委托名称', dataIndex: 'entrustName', key: 'entrustName', width: 150 },
  { title: '委托单号', dataIndex: 'entrustNo', key: 'entrustNo', width: 120 },
  { title: '点位数', dataIndex: 'pointCount', key: 'pointCount', width: 80, align: 'center' },
  { title: '计划区间', dataIndex: 'planRange', key: 'planRange', width: 200 },
  { title: '负责人', dataIndex: 'leadName', key: 'leadName', width: 80 },
  { title: '创建人', key: 'createBy', width: 90 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'op', width: 200, fixed: 'right' }
]

// 表格分组
const tableGroups = computed(() => {
  if (groupBy.value === 'none') return []
  const map = new Map()
  for (const o of displayOrders.value) {
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

// 切换分组维度后重算高度，保证视图流畅稳定
watch([groupBy], () => {
  nextTick(syncTableHeight)
})

const dispatchForm = reactive({
  leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: ''
})

// 派单必填校验由 DispatchForm 内部 rules 负责，此处仅持有表单 ref
const dispatchFormRef = ref(null)

// 逾期判断：未归档完成且计划结束日期早于今天
function isOverdue(o) {
  if (!o || !o.planEnd) return false
  if (o.status === '归档完成' || o.status === '已完成') return false
  return o.planEnd < dayjs().format('YYYY-MM-DD')
}
const overdueOnly = ref(false)
// 统计口径始终基于全量 allOrders；卡片筛选仅控制展示，不影响卡片数字
const statusStats = computed(() =>
  ORDER_STATUS_CHAIN.map((s) => ({
    status: s,
    color: statusColor(s),
    icon: STATUS_ICON[s] || ClockCircleOutlined,
    ...(STATUS_STYLE[s] || DEFAULT_STATUS_STYLE),
    count: allOrders.value.filter((o) => o.status === s).length
  }))
)
const overdueCount = computed(() => allOrders.value.filter(isOverdue).length)
// 负责人下拉：基于全量订单中实际出现的真实负责人姓名去重（排除占位符）
const leadOptions = computed(() => {
  const set = new Set()
  for (const o of allOrders.value) {
    const ln = o.leadName
    if (ln && ln !== '—') set.add(ln)
  }
  return Array.from(set).map((n) => ({ value: n, label: n }))
})
// 展示数据：卡片筛选仅从全量中过滤，不影响卡片统计
const displayOrders = computed(() => {
  let list = allOrders.value
  if (overdueOnly.value) list = list.filter(isOverdue)
  if (filters.status) list = list.filter((o) => o.status === filters.status)
  return list
})

function applyStatusFilter(s) {
  overdueOnly.value = false
  filters.status = filters.status === s ? undefined : s
}
function toggleOverdue() {
  overdueOnly.value = !overdueOnly.value
  if (overdueOnly.value) filters.status = undefined
}

function loadOrders() {
  loading.value = true
  // 仅按订单号 / 负责人查询后端；状态筛选由前端卡片控制，避免影响卡片统计
  const params = {}
  if (filters.orderNo && filters.orderNo.trim()) params.orderNo = filters.orderNo.trim()
  if (filters.leadName && filters.leadName.trim()) params.leadName = filters.leadName.trim()
  getDispatchBoardList(params).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || [])
    allOrders.value = list
    orders.value = list
  }).catch(() => {}).finally(() => { loading.value = false; nextTick(syncTableHeight) })
}

function openDetail(order) {
  router.push({ name: 'EmsDispatchDetail', params: { id: order.id } })
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
  editingDispatchId.value = null
  Object.assign(dispatchForm, { leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: '' })
  dispatchVisible.value = true
}

function closeDispatch() {
  dispatchVisible.value = false
  currentOrder.value = null
  editingDispatchId.value = null
  dispatchBlock.value = ''
}

function handleDeleteOrder(order) {
  deleteSamplingOrder(order.id).then(() => {
    message.success('采样任务已删除')
    loadOrders()
  }).catch(() => {})
}

// ---------- 编辑派单（已有派单记录时可修改负责人/组员/车辆/设备/计划区间/备注） ----------
function canEdit(record) {
  return !!record.dispatchId && hasPerm('ems:dispatch')
}

// 选项去重补充：确保下拉中存在回填项（首次搜索列表可能未包含）
function ensureOption(options, opt) {
  if (opt.value == null) return
  if (!options.some((o) => o.value === opt.value)) options.push(opt)
}

function openEdit(record) {
  currentOrder.value = record
  editingDispatchId.value = record.dispatchId
  Object.assign(dispatchForm, { leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: '' })
  getDispatchDetail(record.id).then((res) => {
    const d = res.data || res || {}
    // 回填下拉选项，保证已选值能正常展示
    if (d.lead) ensureOption(employeeOptions.value, { label: `${d.lead.realName || d.lead.username}（${d.lead.username}）`, value: d.lead.userId })
    ;(d.members || []).forEach((m) => ensureOption(employeeOptions.value, { label: `${m.realName || m.username}（${m.username}）`, value: m.userId }))
    if (d.vehicle) ensureOption(vehicleOptions.value, { label: `${d.vehicle.plateNo}${d.vehicle.model ? ' ' + d.vehicle.model : ''}`, value: d.vehicle.id })
    ;(d.instruments || []).forEach((i) => ensureOption(instrumentOptions.value, { label: `${i.name}${i.model ? ' ' + i.model : ''}`, value: i.id }))
    Object.assign(dispatchForm, {
      leadId: d.lead ? d.lead.userId : undefined,
      empIds: (d.members || []).map((m) => m.userId),
      vehicleId: d.vehicle ? d.vehicle.id : undefined,
      instrumentIds: (d.instruments || []).map((i) => i.id),
      planStart: d.planStart ? dayjs(d.planStart) : null,
      planEnd: d.planEnd ? dayjs(d.planEnd) : null,
      note: d.note || ''
    })
    dispatchVisible.value = true
  }).catch(() => { editingDispatchId.value = null })
}

// ---------- 完成确认（仅「已派单」且当前用户为负责人/管理员可操作） ----------
const completeVisible = ref(false)
const completeLoading = ref(false)
const completeForm = reactive({ actualFinishTime: null, finishDesc: '' })

function canComplete(record) {
  if (record.status !== '已派单') return false
  // 负责人本人或管理员可确认完成（看板 leadName 为负责人真实姓名）
  return userStore.isAdmin || (userStore.realName && record.leadName === userStore.realName)
}

function openComplete(record) {
  currentOrder.value = record
  completeForm.actualFinishTime = dayjs()
  completeForm.finishDesc = ''
  completeVisible.value = true
}

function submitComplete() {
  if (!completeForm.actualFinishTime) {
    message.warning('请选择实际完成时间')
    return
  }
  completeLoading.value = true
  completeSamplingOrder(currentOrder.value.id, {
    actualFinishTime: dayjs(completeForm.actualFinishTime).format('YYYY-MM-DD HH:mm:ss'),
    finishDesc: completeForm.finishDesc
  }).then(() => {
    message.success('采样任务已完成')
    completeVisible.value = false
    currentOrder.value = null
    loadOrders()
  }).catch(() => {}).finally(() => { completeLoading.value = false })
}

async function submitDispatch() {
  // 必填校验：负责人、组员、计划开始、计划结束
  try {
    await dispatchFormRef.value.validate()
  } catch (e) {
    return
  }
  // 双保险：校验所选车辆是否在检车时间区间内可用（ISSUE-035）
  // 编辑模式下车辆可能被本订单自身派单占用（可用列表未排除自身），交由后端冲突检测（排除本订单）兜底
  if (!editingDispatchId.value && dispatchForm.vehicleId && dispatchForm.planStart && dispatchForm.planEnd) {
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
  const isEdit = !!editingDispatchId.value
  const editId = editingDispatchId.value
  dispatchLoading.value = true
  const req = isEdit
    ? updateDispatch(editId, params)
    : createDispatch({ ...params, orderId: currentOrder.value.id })
  req.then(() => {
    message.success(isEdit ? '派单信息已更新（变更已记入操作历史）' : '派单成功（已通过资质闸门与冲突校验）')
    closeDispatch()
    loadOrders()
  }).catch(() => {
    // 冲突等错误提示已由全局响应拦截器统一多行展示，此处仅做加载状态清理
  }).finally(() => { dispatchLoading.value = false })
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

.link-text { color: #2563EB; cursor: pointer; }
.link-text:hover { text-decoration: underline; }

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

/* 表格视图：状态统计卡片 + 逾期卡片（渐变底 + 圆形图标，参考设计稿） */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.stat-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
.stat-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  font-size: 22px;
}
.stat-info {
  min-width: 0;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}
.stat-label {
  margin-top: 2px;
  font-size: 13px;
  color: #4b5563;
}
.stat-card.overdue {
  background: linear-gradient(90deg, #fef2f2, #fff1f2);
  border-color: #fecaca;
}
.stat-card.overdue .stat-icon {
  color: #ff4d4f;
  background: #fee2e2;
}
.stat-card.overdue .stat-num {
  color: #ff4d4f;
}
.stat-card.overdue.active {
  border-color: #ff4d4f;
}

/* 状态全过程 A - B - C - D：当前蓝色、完成绿色、未到达灰色 */
.status-chain { display: inline-flex; align-items: center; flex-wrap: wrap; line-height: 20px; }
.chain-node { font-size: 12px; padding: 0 2px; border-radius: 4px; }
.chain-done { color: #52c41a; }      /* 已完成：绿色 */
.chain-current { color: #1677ff; font-weight: 600; }  /* 当前：蓝色 */
.chain-todo { color: #bbb; }          /* 未到达：灰色 */
.chain-sep { color: #ccc; margin: 0 2px; }
</style>
