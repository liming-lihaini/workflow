/**
 * 格式化日期为 YYYY-MM-DD 格式
 * 支持 LocalDateTime 字符串、Date 对象等
 */
export function formatDate(val) {
  if (!val) return '-'
  const str = String(val)
  // 取前10个字符 "2026-07-23" 部分
  if (str.length >= 10) return str.substring(0, 10)
  return str
}

/**
 * 用于 Ant Design Vue Table 的 customRender
 * 用法: { ...colDef, customRender: ({ text }) => formatDate(text) }
 */
export function renderDate({ text }) {
  return formatDate(text)
}
