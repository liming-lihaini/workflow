import { ref } from 'vue'

/**
 * 表格列拖拽宽度 composable
 * 用法：
 *   const { columns, handleResizeColumn } = useResizableColumns(initialColumns)
 *
 * 模板中：
 *   <a-table :columns="columns" :resize-column="true" @resizeColumn="handleResizeColumn" ... />
 */
export function useResizableColumns(initialColumns) {
  const columns = ref(initialColumns.map(c => ({ ...c })))

  function handleResizeColumn(width, column, index) {
    columns.value[index].width = width
  }

  return { columns, handleResizeColumn }
}

/**
 * 前端排序辅助：对 dataList 按 sorter 排序
 * @param {Array} list - 原始数据
 * @param {Object} sorter - { field, order } from table change event
 * @returns {Array} 排序后的数据
 */
export function sortClientData(list, sorter) {
  if (!sorter || !sorter.field || !sorter.order) return list
  const { field, order } = sorter
  const sorted = [...list].sort((a, b) => {
    const va = a[field], vb = b[field]
    if (va == null && vb == null) return 0
    if (va == null) return 1
    if (vb == null) return -1
    if (typeof va === 'number' && typeof vb === 'number') return va - vb
    return String(va).localeCompare(String(vb))
  })
  return order === 'descend' ? sorted.reverse() : sorted
}
