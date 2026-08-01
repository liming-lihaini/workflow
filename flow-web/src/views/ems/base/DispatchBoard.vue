<template>
  <div class="page-wrap">
    <div class="card-wrap">
      <div class="page-header">
        <span class="page-title">采样调度看板</span>
        <a-button @click="loadOrders">刷新</a-button>
      </div>

      <div class="board">
        <div v-for="col in columns" :key="col.status" class="board-col">
          <div class="board-col-title" :style="{ background: col.color }">{{ col.title }} ({{ grouped[col.status]?.length || 0 }})</div>
          <div class="board-col-body">
            <a-card
              v-for="order in grouped[col.status] || []"
              :key="order.id"
              size="small"
              class="order-card"
              hoverable
              @click="openDispatch(order)"
            >
              <div class="order-no link" @click.stop="openDetail(order)">{{ order.orderNo }}</div>
              <div class="order-meta">点位ID: {{ order.pointId || '—' }}</div>
              <div class="order-meta">计划: {{ order.planDate || '—' }}</div>
            </a-card>
            <a-empty v-if="!(grouped[col.status] || []).length" description="无" :image="simpleImage" />
          </div>
        </div>
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
import { ref, reactive, computed, onMounted } from 'vue'
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
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const currentOrder = ref(null)
const detailVisible = ref(false)
const detailOrderId = ref(null)
const employeeOptions = ref([])
const vehicleOptions = ref([])
const instrumentOptions = ref([])

const columns = [
  { status: '待派单', title: '待派单', color: '#F59E0B' },
  { status: '已派单', title: '已派单', color: '#2563EB' },
  { status: '采样执行中', title: '执行中', color: '#2563EB' },
  { status: '样品送检', title: '已送检', color: '#2563EB' }
]

const grouped = computed(() => {
  const g = {}
  for (const col of columns) g[col.status] = []
  for (const o of orders.value) {
    if (g[o.status]) g[o.status].push(o)
  }
  return g
})

const dispatchForm = reactive({
  leadId: undefined, empIds: [], vehicleId: undefined, instrumentIds: [], planStart: null, planEnd: null, note: ''
})

function loadOrders() {
  getSamplingOrders({}).then((res) => {
    const data = res.data || res
    orders.value = Array.isArray(data) ? data : (data.list || [])
  }).catch(() => {})
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
})
</script>

<style scoped>
.board { display: flex; gap: 12px; overflow-x: auto; }
.board-col { flex: 1; min-width: 200px; background: #fafafa; border-radius: 8px; padding: 8px; }
.board-col-title { color: #fff; padding: 6px 10px; border-radius: 6px; font-weight: 600; }
.board-col-body { margin-top: 8px; min-height: 120px; }
.order-card { margin-bottom: 8px; }
.order-no { font-weight: 600; }
.order-no.link { color: #2563EB; cursor: pointer; }
.order-no.link:hover { text-decoration: underline; }
.order-meta { color: #888; font-size: 12px; }
</style>
