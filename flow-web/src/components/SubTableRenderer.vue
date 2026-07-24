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
import { computed, watch } from 'vue'

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
    width: col.width || undefined
  }))
  if (!readonly.value) {
    cols.push({ title: '操作', key: '_action', width: 60, align: 'center' })
  }
  return cols
})

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
    emptyRow[col.fieldKey] = col.defaultValue || ''
  }
  emit('update:modelValue', [...tableData.value, emptyRow])
}

function removeRow(idx) {
  const newData = [...tableData.value]
  newData.splice(idx, 1)
  emit('update:modelValue', newData)
}

function getCellDisplayValue(column, val) {
  if (val === undefined || val === null || val === '') return '-'
  return String(val)
}
</script>

<style scoped>
.subtable-renderer { padding: 0; }
</style>
