<template>
  <a-form ref="formRef" layout="vertical" :model="form" :rules="rules">
    <a-form-item label="负责人" name="leadId">
      <a-select
        v-model:value="form.leadId"
        show-search
        placeholder="搜索并选择负责人"
        :options="employeeOptions"
        :filter-option="false"
        @search="onSearchEmployee"
        allow-clear
      />
    </a-form-item>
    <a-form-item label="组员（可多选）" name="empIds">
      <a-select
        v-model:value="form.empIds"
        mode="multiple"
        show-search
        placeholder="搜索并选择组员"
        :options="employeeOptions"
        :filter-option="false"
        @search="onSearchEmployee"
        allow-clear
      />
    </a-form-item>
    <a-row :gutter="16">
      <a-col :span="12">
        <a-form-item label="计划开始" name="planStart">
          <a-date-picker v-model:value="form.planStart" format="YYYY-MM-DD" style="width: 100%" @change="onPlanTimeChange" />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="计划结束" name="planEnd">
          <a-date-picker v-model:value="form.planEnd" format="YYYY-MM-DD" style="width: 100%" @change="onPlanTimeChange" />
        </a-form-item>
      </a-col>
    </a-row>
    <a-form-item label="车辆">
      <a-select
        v-model:value="form.vehicleId"
        show-search
        placeholder="搜索并选择车辆"
        :options="vehicleOptions"
        :filter-option="false"
        @search="onSearchVehicle"
        allow-clear
      />
      <div v-if="form.planStart && form.planEnd" class="vehicle-tip">
        <a-alert
          v-if="!vehicleOptions.length"
          type="warning"
          show-icon
          message="检车时间范围内没有可用车辆，请调整计划时间"
        />
        <a-alert v-else type="info" show-icon class="vehicle-filter-tip">
          <template #message>
            <span class="tip-label">已按时间过滤可用车辆</span>
            <span class="tip-range">
              {{ dayjs(form.planStart).format('MM-DD HH:mm') }} ~ {{ dayjs(form.planEnd).format('MM-DD HH:mm') }}
            </span>
            <a-tag color="blue" class="tip-count">{{ vehicleOptions.length }} 辆可用</a-tag>
          </template>
        </a-alert>
      </div>
    </a-form-item>
    <a-form-item label="设备（可多选）">
      <a-select
        v-model:value="form.instrumentIds"
        mode="multiple"
        show-search
        placeholder="搜索并选择设备"
        :options="instrumentOptions"
        :filter-option="false"
        @search="onSearchInstrument"
        allow-clear
      />
      <a-alert
        v-if="instrumentConflicts.length"
        type="warning"
        show-icon
        class="instrument-conflict-tip"
        message="所选设备在计划时间区间内被占用"
      >
        <template #description>
          <ul class="conflict-list">
            <li v-for="c in instrumentConflicts" :key="c.id">
              <span class="conflict-name">{{ c.label }}</span>
              <template v-for="r in c.ranges" :key="r.key">
                <a-tag color="orange" class="conflict-range">{{ r.type }}：{{ r.text }}</a-tag>
              </template>
            </li>
          </ul>
        </template>
      </a-alert>
    </a-form-item>

    <a-form-item label="备注">
      <a-textarea v-model:value="form.note" :rows="2" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { searchUsers, listVehicles, listInstruments, getAvailableVehicles, getInstrumentUsage } from '../../../api/ems'

const props = defineProps({
  form: { type: Object, required: true },
  employeeOptions: { type: Array, default: () => [] },
  vehicleOptions: { type: Array, default: () => [] },
  instrumentOptions: { type: Array, default: () => [] },
  // 编辑模式：排除本派单自身的占用区间
  excludeDispatchId: { type: Number, default: null }
})

const formRef = ref(null)
// 已选设备在计划区间内的占用明细（ranges/maintenances 展开后）
const instrumentConflicts = ref([])

const rules = {
  leadId: [{ required: true, message: '请选择负责人' }],
  empIds: [{ required: true, type: 'array', message: '请至少选择一名组员' }],
  planStart: [{ required: true, message: '请选择计划开始日期' }],
  planEnd: [
    { required: true, message: '请选择计划结束日期' },
    {
      validator: (_, value) => {
        if (value && props.form.planStart && dayjs(value).isBefore(dayjs(props.form.planStart), 'day')) {
          return Promise.reject('结束时间必须大于等于开始时间')
        }
        return Promise.resolve()
      },
      trigger: 'change'
    }
  ]
}

function onSearchEmployee(keyword) {
  searchUsers({ keyword: keyword || undefined, page: 1, size: 20 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    props.employeeOptions.length = 0
    list.forEach((u) => props.employeeOptions.push({
      label: `${u.realName || u.username}（${u.username}）`,
      value: u.id
    }))
  }).catch(() => {})
}

function onSearchVehicle(keyword) {
  listVehicles({ keyword: keyword || undefined, page: 1, size: 50 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    const all = list.map((v) => ({ label: `${v.plateNo}${v.model ? ' ' + v.model : ''}`, value: v.id }))
    if (props.form.planStart && props.form.planEnd) {
      refreshAvailableVehicles(all)
    } else {
      props.vehicleOptions.length = 0
      all.forEach((o) => props.vehicleOptions.push(o))
    }
  }).catch(() => {})
}

function refreshAvailableVehicles(allOptions) {
  if (!props.form.planStart || !props.form.planEnd) {
    props.vehicleOptions.length = 0
    ;(allOptions || []).forEach((o) => props.vehicleOptions.push(o))
    return
  }
  const ps = dayjs(props.form.planStart).format('YYYY-MM-DDTHH:mm:ss')
  const pe = dayjs(props.form.planEnd).format('YYYY-MM-DDTHH:mm:ss')
  getAvailableVehicles({ planStart: ps, planEnd: pe }).then((res) => {
    const ids = res.data || res || []
    const base = allOptions || props.vehicleOptions
    const isEdit = !!props.excludeDispatchId
    props.vehicleOptions.length = 0
    base.filter((o) => ids.includes(o.value)).forEach((o) => props.vehicleOptions.push(o))
    if (isEdit) {
      // 编辑模式：可用列表未排除本派单自身占用，保留已选车辆选项防止误判，冲突由后端（排除本订单）兜底
      const selected = base.find((o) => o.value === props.form.vehicleId)
      if (selected && !ids.includes(selected.value)) props.vehicleOptions.push(selected)
      return
    }
    // 已选车辆在新时间区间内不可用：提示并清除选择
    if (props.form.vehicleId && !ids.includes(props.form.vehicleId)) {
      const selected = base.find((o) => o.value === props.form.vehicleId)
      message.warning(`车辆「${selected ? selected.label : props.form.vehicleId}」在 ${dayjs(props.form.planStart).format('MM-DD HH:mm')} ~ ${dayjs(props.form.planEnd).format('MM-DD HH:mm')} 区间内已被占用或维修保养，请重新选择`)
      props.form.vehicleId = undefined
    }
    if (!ids.length) {
      if (props.form.vehicleId) props.form.vehicleId = undefined
    }
  }).catch(() => {})
}

// 时间区间变化后，检测已选设备的占用情况（派单占用 + 校准停用期），明细到被占用的具体区间
function checkInstrumentConflicts() {
  instrumentConflicts.value = []
  if (!props.form.planStart || !props.form.planEnd) return
  if (dayjs(props.form.planEnd).isBefore(dayjs(props.form.planStart), 'day')) return
  const selected = props.form.instrumentIds || []
  if (!selected.length) return
  const ps = dayjs(props.form.planStart).format('YYYY-MM-DDTHH:mm:ss')
  const pe = dayjs(props.form.planEnd).format('YYYY-MM-DDTHH:mm:ss')
  getInstrumentUsage({ start: ps, end: pe }).then((res) => {
    const list = res.data || res || []
    const conflicts = []
    list.filter((it) => selected.includes(it.instrumentId)).forEach((it) => {
      const ranges = []
      // 派单占用：编辑模式排除本派单自身
      ;(it.ranges || []).forEach((r) => {
        if (props.excludeDispatchId && r.dispatchId === props.excludeDispatchId) return
        ranges.push({
          key: 'd_' + r.dispatchId,
          type: '派单占用',
          text: `${dayjs(r.start).format('YYYY-MM-DD HH:mm')} ~ ${dayjs(r.end).format('YYYY-MM-DD HH:mm')}`
        })
      })
      // 校准停用期
      ;(it.maintenances || []).forEach((m) => {
        ranges.push({
          key: 'm_' + m.id,
          type: '校准停用',
          text: `${dayjs(m.start).format('YYYY-MM-DD')} ~ ${dayjs(m.end).format('YYYY-MM-DD')}`
        })
      })
      if (ranges.length) {
        conflicts.push({ id: it.instrumentId, label: `${it.name}${it.model ? ' ' + it.model : ''}`, ranges })
      }
    })
    instrumentConflicts.value = conflicts
    if (conflicts.length) {
      message.warning(`${conflicts.length} 台所选设备在计划时间区间内被占用，详见设备下方提示`)
    }
  }).catch(() => {})
}

function onPlanTimeChange() {
  // 开始/结束任一变动的时，重新校验结束时间（>= 开始时间）
  if (props.form.planStart && props.form.planEnd) {
    formRef.value.validateFields(['planEnd']).catch(() => {})
  }
  // 时间变化后重新检测所选车辆/设备在新区间内是否可用
  onSearchVehicle('')
  checkInstrumentConflicts()
}

function onSearchInstrument(keyword) {
  // 仅查询在用设备（后端 status 为惰性重算，需再按校准到期日过滤掉实际已过期的）
  listInstruments({ keyword: keyword || undefined, status: '在用', page: 1, size: 50 }).then((res) => {
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.list || data.records || [])
    const usable = list.filter((i) => !i.calibDue || !dayjs(i.calibDue).isBefore(dayjs(), 'day'))
    props.instrumentOptions.length = 0
    usable.forEach((i) => props.instrumentOptions.push({ label: `${i.name}${i.model ? ' ' + i.model : ''}`, value: i.id }))
  }).catch(() => {})
}

defineExpose({ formRef, validate: () => formRef.value.validate(), reloadVehicles: () => onSearchVehicle('') })

// 打开抽屉即加载全部选项（负责人/车辆/设备），之后由搜索/时间区间联动刷新
onMounted(() => {
  onSearchEmployee('')
  onSearchVehicle('')
  onSearchInstrument('')
})
</script>

<style scoped>
.vehicle-tip {
  margin-top: 8px;
}
.vehicle-filter-tip :deep(.ant-alert-message) {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tip-label {
  color: rgba(0, 0, 0, 0.85);
}
.tip-range {
  color: rgba(0, 0, 0, 0.65);
  font-family: monospace;
}
.tip-count {
  margin: 0;
  margin-left: auto;
}
.instrument-conflict-tip {
  margin-top: 8px;
}
.conflict-list {
  margin: 4px 0 0;
  padding-left: 0;
  list-style: none;
}
.conflict-list li {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}
.conflict-name {
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}
.conflict-range {
  margin: 0;
  font-family: monospace;
}
</style>
