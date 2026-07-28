<template>
  <div class="subtable-renderer">
    <a-table
      :columns="tableColumns"
      :data-source="tableData"
      :pagination="false"
      :bordered="true"
      size="small"
      :row-key="(_, idx) => idx"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === '_action'">
          <a-button v-if="!readonly" type="text" danger size="small" @click="removeRow(index)">
            删除
          </a-button>
        </template>
        <template v-else>
          <template v-if="readonly">
            <span>{{ getCellDisplayValue(column, record[column.dataIndex]) }}</span>
          </template>
          <template v-else>
            <a-input v-if="['text', 'textarea'].includes(column.fieldType)"
              :value="record[column.dataIndex]" size="small"
              @change="(e) => updateCell(index, column.dataIndex, e.target.value)"
              :placeholder="column.title" />
            <a-input-number v-else-if="['number', 'amount'].includes(column.fieldType)"
              :value="record[column.dataIndex]" size="small" style="width: 100%"
              @change="(v) => updateCell(index, column.dataIndex, v)"
              :min="0" :precision="column.fieldType === 'amount' ? 2 : 0" />
            <a-date-picker v-else-if="column.fieldType === 'date'"
              :value="record[column.dataIndex]" size="small" style="width: 100%"
              @change="(_, ds) => updateCell(index, column.dataIndex, ds)"
              value-format="YYYY-MM-DD" />
            <a-select v-else-if="column.fieldType === 'select'"
              :value="record[column.dataIndex]" size="small" style="width: 100%"
              :mode="column.selectMode === 'multiple' ? 'multiple' : undefined"
              :options="optionsCache[column.dataIndex] || []"
              :placeholder="column.title" allow-clear show-search option-filter-prop="label"
              @change="(v) => updateCell(index, column.dataIndex, v)" />
            <a-input v-else
              :value="record[column.dataIndex]" size="small"
              @change="(e) => updateCell(index, column.dataIndex, e.target.value)" />
          </template>
        </template>
      </template>
    </a-table>
    <a-button v-if="!readonly" type="dashed" size="small" block style="margin-top: 8px" @click="addRow">
      + 添加行
    </a-button>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { getDictItemsByCode } from '../api/dict'
import request from '../api/request'

const props = defineProps({
  field: { type: Object, required: true },
  modelValue: { type: Array, default: () => [] },
  mode: { type: String, default: 'editable' }
})

const emit = defineEmits(['update:modelValue'])

const readonly = computed(() => props.mode === 'readonly')

const tableColumns = computed(() => {
  const cols = (props.field.columns || []).map(col => ({
    title: col.label || col.fieldKey,
    dataIndex: col.fieldKey,
    key: col.fieldKey,
    fieldType: col.type || 'text',
    selectMode: col.selectMode || 'single',
    width: col.width || undefined
  }))
  if (!readonly.value) {
    cols.push({ title: '操作', key: '_action', width: 60, align: 'center' })
  }
  return cols
})

// --- 下拉列选项加载：{ fieldKey: [{value, label}] } ---
const optionsCache = reactive({})

function parseCustomOptions(text) {
  return (text || '').split('\n').filter(Boolean).map(line => {
    const [v, ...r] = line.split(':')
    return { value: v.trim(), label: r.join(':').trim() || v.trim() }
  })
}

function resolvePath(obj, path) {
  if (!obj || !path) return obj
  let cur = obj
  for (const p of path.split('.')) {
    if (cur === null || cur === undefined) return undefined
    cur = cur[p]
  }
  return cur
}

async function loadColumnOptions(col) {
  const source = col.optionsSource || 'custom'
  if (source === 'custom') {
    optionsCache[col.fieldKey] = parseCustomOptions(col.optionsText)
  } else if (source === 'dict' && col.dictCode) {
    try {
      const res = await getDictItemsByCode(col.dictCode)
      const items = res.data || res
      optionsCache[col.fieldKey] = (Array.isArray(items) ? items : []).map(it => ({
        value: it.itemValue ?? it.value, label: it.itemText ?? it.label ?? it.itemValue
      }))
    } catch { optionsCache[col.fieldKey] = [] }
  } else if (source === 'api' && col.api?.url) {
    try {
      const method = (col.api.method || 'GET').toUpperCase()
      const res = await request({ url: col.api.url, method })
      let data = resolvePath(res, col.api.dataPath || 'data')
      if (data === undefined) data = resolvePath(res, 'data') ?? res
      const valueField = col.api.valueField || 'value'
      const labelField = col.api.labelField || 'label'
      optionsCache[col.fieldKey] = (Array.isArray(data) ? data : []).map(it => ({
        value: it[valueField], label: it[labelField] ?? String(it[valueField])
      }))
    } catch { optionsCache[col.fieldKey] = [] }
  } else {
    optionsCache[col.fieldKey] = []
  }
}

watch(() => props.field.columns, (cols) => {
  for (const col of (cols || [])) {
    if (col.type === 'select' && col.fieldKey) loadColumnOptions(col)
  }
}, { immediate: true, deep: true })

const tableData = computed(() => {
  return Array.isArray(props.modelValue) ? props.modelValue : []
})

function updateCell(rowIdx, fieldKey, value) {
  const newData = [...tableData.value]
  newData[rowIdx] = { ...newData[rowIdx], [fieldKey]: value }
  emit('update:modelValue', newData)
}

function addRow() {
  const emptyRow = {}
  for (const col of (props.field.columns || [])) {
    // 多选下拉列默认值为数组
    if (col.type === 'select' && col.selectMode === 'multiple') {
      emptyRow[col.fieldKey] = []
    } else {
      emptyRow[col.fieldKey] = col.defaultValue || ''
    }
  }
  emit('update:modelValue', [...tableData.value, emptyRow])
}

function removeRow(idx) {
  const newData = [...tableData.value]
  newData.splice(idx, 1)
  emit('update:modelValue', newData)
}

function getCellDisplayValue(column, val) {
  if (val === undefined || val === null || val === '' || (Array.isArray(val) && val.length === 0)) return '-'
  // 下拉列：值转译为选项标签
  if (column.fieldType === 'select') {
    const opts = optionsCache[column.dataIndex] || []
    const toLabel = (v) => opts.find(o => String(o.value) === String(v))?.label ?? String(v)
    return Array.isArray(val) ? val.map(toLabel).join(', ') : toLabel(val)
  }
  if (Array.isArray(val)) return val.join(', ')
  return String(val)
}
</script>

<style scoped>
.subtable-renderer { padding: 0; }
</style>
