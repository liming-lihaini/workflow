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
        placeholder="搜索并选择设备（仅展示在用）"
        :options="instrumentOptions"
        :filter-option="false"
        @search="onSearchInstrument"
        allow-clear
      />
    </a-form-item>

    <a-form-item label="备注">
      <a-textarea v-model:value="form.note" :rows="2" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { searchUsers, listVehicles, listInstruments, getAvailableVehicles } from '../../../api/ems'

const props = defineProps({
  form: { type: Object, required: true },
  employeeOptions: { type: Array, default: () => [] },
  vehicleOptions: { type: Array, default: () => [] },
  instrumentOptions: { type: Array, default: () => [] }
})

const formRef = ref(null)

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
    props.vehicleOptions.length = 0
    base.filter((o) => ids.includes(o.value)).forEach((o) => props.vehicleOptions.push(o))
    if (!ids.length) {
      if (props.form.vehicleId) props.form.vehicleId = undefined
    }
  }).catch(() => {})
}

function onPlanTimeChange() {
  // 开始/结束任一变动的时，重新校验结束时间（>= 开始时间）
  if (props.form.planStart && props.form.planEnd) {
    formRef.value.validateFields(['planEnd']).catch(() => {})
  }
  onSearchVehicle('')
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
</style>
