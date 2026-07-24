<template>
  <div class="form-renderer">
    <a-spin :spinning="loading">
      <!-- 嵌套结构：sections > rows > cells > fields -->
      <template v-if="parsedSections.length > 0">
        <template v-for="(section, sIdx) in parsedSections" :key="section.id || sIdx">
          <a-divider v-if="section.title" orientation="left" style="margin: 8px 0 12px">
            {{ section.title }}
          </a-divider>
          <template v-for="(row, rIdx) in (section.children || [])" :key="row.id || rIdx">
            <a-row :gutter="row.columns === 2 ? 16 : 0" style="margin-bottom: 4px">
              <a-col
                v-for="(cell, cIdx) in (row.cells || [])"
                :key="cell.id || cIdx"
                :span="24 / (row.columns || 1)"
              >
                <template v-for="(field, fIdx) in (cell.fields || [])" :key="field.id || fIdx">
                  <!-- 子表表格：占满整行 -->
                  <template v-if="field.type === 'subTable'">
                    <a-divider orientation="left" style="margin: 4px 0 8px">{{ field.label || '子表' }}</a-divider>
                    <SubTableRenderer
                      :field="field"
                      :model-value="localValues[field.field || field.key] || []"
                      :mode="mode"
                      @update:model-value="(val) => { localValues[field.field || field.key] = val; emit('update:modelValue', { ...localValues }) }"
                    />
                  </template>
                  <a-form-item
                    v-else
                    :label="field.label || field.field"
                    :required="field.required"
                    style="margin-bottom: 12px"
                  >
                    <!-- 只读模式 -->
                    <template v-if="mode === 'readonly'">
                      <span class="readonly-value">{{ getDisplayValue(field) }}</span>
                    </template>
                    <!-- 可编辑模式 -->
                    <template v-else>
                      <!-- 计算控件：只读显示计算结果 -->
                      <div v-if="field.type === 'calculation'" class="calc-display">
                        {{ computeCalcValue(field) }}
                      </div>
                      <component
                        v-else
                        :is="getComponent(field)"
                        v-model:value="localValues[field.field || field.key]"
                        v-bind="getComponentProps(field)"
                      />
                    </template>
                  </a-form-item>
                </template>
              </a-col>
            </a-row>
          </template>
        </template>
      </template>

      <!-- 兼容旧结构：扁平 fields 列表 -->
      <template v-else-if="parsedFields.length > 0">
        <a-form layout="vertical" :model="localValues">
          <a-form-item
            v-for="field in parsedFields"
            :key="field.key || field.id"
            :label="field.label"
            :required="field.required"
          >
            <template v-if="mode === 'readonly'">
              <span class="readonly-value">{{ getDisplayValue(field) }}</span>
            </template>
            <template v-else>
              <div v-if="field.type === 'calculation'" class="calc-display">
                {{ computeCalcValue(field) }}
              </div>
              <component
                v-else
                :is="getComponent(field)"
                v-model:value="localValues[field.field || field.key]"
                v-bind="getComponentProps(field)"
              />
            </template>
          </a-form-item>
        </a-form>
      </template>

      <a-empty v-else-if="!loading" description="暂无表单字段" />
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, watch, h, markRaw, nextTick } from 'vue'
import {
  Input, Textarea, InputNumber, DatePicker, TimePicker,
  Select, Radio, Checkbox, TreeSelect, Cascader, Upload, Button, Tag
} from 'ant-design-vue'
import SubTableRenderer from './SubTableRenderer.vue'

const props = defineProps({
  /** formJson 字符串或对象（sections 嵌套结构或旧 fields 结构） */
  formJson: { type: [String, Object], default: null },
  /** 旧接口：扁平 fields 数组 */
  fields: { type: Array, default: () => [] },
  /** 表单值 v-model */
  modelValue: { type: Object, default: () => ({}) },
  /** 模式：editable | readonly */
  mode: { type: String, default: 'editable' },
  /** 加载中 */
  loading: { type: Boolean, default: false },
  /** 字典数据缓存 { dictCode: [{value, label}] } */
  dictData: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue'])

const localValues = ref({})
let isExternalUpdate = false

watch(() => props.modelValue, (val) => {
  isExternalUpdate = true
  localValues.value = { ...val }
  nextTick(() => { isExternalUpdate = false })
}, { deep: true, immediate: true })

watch(localValues, (val) => {
  if (!isExternalUpdate) {
    emit('update:modelValue', { ...val })
  }
}, { deep: true })

// 解析 formJson → sections
const parsedSections = computed(() => {
  const fj = parseFormJson()
  if (!fj) return []
  if (fj.sections && Array.isArray(fj.sections)) return fj.sections
  return []
})

// 解析 formJson → flat fields（旧结构 fallback）
const parsedFields = computed(() => {
  const fj = parseFormJson()
  if (!fj) {
    // 没有 formJson，使用 props.fields
    return props.fields.map(f => normalizeField(f))
  }
  if (fj.sections) return [] // 有 sections，不走 flat
  if (fj.fields && Array.isArray(fj.fields)) return fj.fields.map(f => normalizeField(f))
  return []
})

function parseFormJson() {
  if (!props.formJson) return null
  if (typeof props.formJson === 'string') {
    try { return JSON.parse(props.formJson) } catch { return null }
  }
  return props.formJson
}

function normalizeField(f) {
  return {
    field: f.field || f.key || f.id,
    label: f.label || f.title || f.field || f.key,
    type: f.type || f.component || 'text',
    required: f.required || false,
    placeholder: f.placeholder || '',
    optionsText: f.optionsText || '',
    optionsSource: f.optionsSource || 'manual',
    dictCode: f.dictCode || '',
    modelField: f.modelField || ''
  }
}

// 根据字段类型返回 Ant Design 组件
function getComponent(field) {
  const type = field.type || 'text'
  const map = {
    text: Input,
    textarea: Textarea,
    number: InputNumber,
    amount: InputNumber,
    date: DatePicker,
    time: TimePicker,
    datetime: DatePicker,
    select: Select,
    user: Select,
    dept: TreeSelect,
    cascader: Cascader,
    radio: Radio.Group,
    checkbox: Checkbox.Group,
    file: Upload,
  }
  return markRaw(map[type] || Input)
}

// 返回组件的 props
function getComponentProps(field) {
  const type = field.type || 'text'
  const base = {
    placeholder: field.placeholder || `请输入${field.label || ''}`,
    allowClear: true,
  }
  if (type === 'textarea') return { ...base, rows: 3 }
  if (type === 'number') return { ...base, style: { width: '100%' }, min: 0 }
  if (type === 'amount') return { ...base, style: { width: '100%' }, min: 0, precision: 2 }
  if (type === 'date') return { ...base, style: { width: '100%' }, valueFormat: 'YYYY-MM-DD' }
  if (type === 'time') return { ...base, style: { width: '100%' }, valueFormat: 'HH:mm:ss' }
  if (type === 'datetime') return { ...base, style: { width: '100%' }, showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss' }
  if (type === 'select' || type === 'user') {
    const options = getFieldOptions(field)
    return { ...base, options: options.map(o => ({ value: o.value, label: o.text })), showSearch: true }
  }
  if (type === 'dept') return { ...base, placeholder: '选择部门', treeDefaultExpandAll: true }
  if (type === 'cascader') {
    let options = []
    try { options = JSON.parse(field.cascaderOptionsJson || '[]') } catch { options = [] }
    return { ...base, options }
  }
  if (type === 'radio') {
    const options = getFieldOptions(field)
    return { options: options.map(o => ({ value: o.value, label: o.text })) }
  }
  if (type === 'checkbox') {
    const options = getFieldOptions(field)
    return { options: options.map(o => ({ value: o.value, label: o.text })) }
  }
  if (type === 'file') return {}
  return base
}

// 获取字段选项
function getFieldOptions(field) {
  // 优先从字典数据获取
  if (field.dictCode && props.dictData[field.dictCode]) {
    return props.dictData[field.dictCode].map(item => ({
      value: item.value || item.dictValue,
      text: item.label || item.dictLabel || item.value || item.dictValue
    }))
  }
  // 手动选项
  if (field.optionsSource === 'manual' && field.optionsText) {
    return field.optionsText.split('\n').filter(Boolean).map(line => {
      const [v, ...r] = line.split(':')
      return { value: v.trim(), text: r.join(':').trim() || v.trim() }
    })
  }
  if (field.options && Array.isArray(field.options)) {
    return field.options.map(o => typeof o === 'string' ? { value: o, text: o } : o)
  }
  return []
}

// 只读模式显示值
function getDisplayValue(field) {
  if (field.type === 'subTable') {
    const arr = localValues.value[field.field || field.key]
    return Array.isArray(arr) ? `${arr.length} 行数据` : '-'
  }
  if (field.type === 'calculation') return computeCalcValue(field)
  const key = field.field || field.key
  const val = localValues.value[key]
  if (val === undefined || val === null || val === '') return '-'
  // 如果是 select/radio，显示标签
  if (['select', 'radio', 'user'].includes(field.type)) {
    const opts = getFieldOptions(field)
    const opt = opts.find(o => o.value === val)
    if (opt) return opt.text
  }
  if (Array.isArray(val)) return val.join(', ')
  return String(val)
}

/** 计算控件：解析公式并实时计算结果 */
function computeCalcValue(field) {
  const formula = field.formula
  if (!formula) return '未配置公式'
  try {
    const unit = field.calcUnit || 'day'
    const refs = formula.match(/\$\{(\w+)\}/g) || []
    if (refs.length === 0) return formula

    const refKeys = refs.map(r => r.match(/\$\{(\w+)\}/)[1])
    const values = refKeys.map(k => localValues.value[k])

    // 日期差计算
    if (values.every(v => v && isDateStr(String(v)))) {
      if (refKeys.length === 2 && formula.includes('-')) {
        const d1 = new Date(values[0])
        const d2 = new Date(values[1])
        const diffMs = Math.abs(d2 - d1)
        if (unit === 'hour') return Math.round(diffMs / 3600000) + ' 小时'
        if (unit === 'minute') return Math.round(diffMs / 60000) + ' 分钟'
        return Math.round(diffMs / 86400000) + ' 天'
      }
    }

    // 数值运算
    let expr = formula
    refs.forEach(ref => {
      const key = ref.match(/\$\{(\w+)\}/)[1]
      const val = parseFloat(localValues.value[key]) || 0
      expr = expr.replace(ref, val)
    })
    if (/^[\d\s+\-*/().]+$/.test(expr)) {
      const result = Function('"use strict"; return (' + expr + ')')()
      return Number.isFinite(result) ? Math.round(result * 100) / 100 : '计算错误'
    }
    return expr
  } catch {
    return '公式错误'
  }
}

function isDateStr(str) {
  return /^\d{4}-\d{2}-\d{2}/.test(str) || /^\d{4}\/\d{2}\/\d{2}/.test(str)
}
</script>

<style scoped>
.form-renderer { padding: 0; }
.readonly-value {
  color: #333;
  font-size: 14px;
  padding: 4px 0;
  display: inline-block;
  line-height: 1.6;
}
.calc-display {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #f9f0ff;
  border: 1px solid #d3adf7;
  border-radius: 4px;
  font-size: 14px;
  color: #722ed1;
  font-weight: 500;
  min-height: 32px;
}
</style>
