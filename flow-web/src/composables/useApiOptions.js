import { ref, reactive, watch } from 'vue'
import request from '../api/request'

/**
 * 下拉选项接口数据源（表单设计器/渲染器共用）
 *
 * 字段 schema：
 *   optionsSource: 'api'
 *   api: {
 *     method: 'GET' | 'POST',
 *     url: '/api/v1/customers/${formData.custId}',   // 支持路径参数 ${formData.字段} / ${字段}
 *     params: [{ name, value, in: 'query' | 'body' | 'header' }],  // value 支持模板引用
 *     dataPath: 'data',        // 响应体中数据数组的点路径
 *     valueField: 'id',        // 值字段
 *     labelField: 'custName'   // 标签字段
 *   }
 */

/** 模板替换：${formData.xxx} / ${xxx} → 表单字段值 */
export function substituteTemplate(str, formValues) {
  if (str === undefined || str === null) return str
  return String(str).replace(/\$\{formData\.([^}]+)\}|\$\{([^}]+)\}/g, (_, fdKey, plainKey) => {
    const key = fdKey || plainKey
    const val = formValues ? formValues[key] : undefined
    return val === undefined || val === null ? '' : String(val)
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

/** 按配置拉取接口选项，转换为 [{ value, label }] 字典结构 */
export async function fetchApiOptions(api, formValues = {}) {
  if (!api || !api.url) return []
  const url = substituteTemplate(api.url, formValues)
  const method = (api.method || 'GET').toUpperCase()
  const query = {}
  const body = {}
  const headers = {}
  for (const p of (api.params || [])) {
    if (!p || !p.name) continue
    const val = substituteTemplate(p.value, formValues)
    const pos = p.in || 'query'
    if (pos === 'header') headers[p.name] = val
    else if (pos === 'body') body[p.name] = val
    else query[p.name] = val
  }
  const res = await request({
    url,
    method,
    params: method === 'GET' ? query : undefined,
    data: method === 'POST' ? body : undefined,
    headers: Object.keys(headers).length ? headers : undefined
  })
  let data = resolvePath(res, api.dataPath || 'data')
  if (data === undefined) data = resolvePath(res, 'data')
  if (data === undefined) data = res
  const valueField = api.valueField || 'value'
  const labelField = api.labelField || 'label'
  return (Array.isArray(data) ? data : []).map(it => ({
    value: it[valueField],
    label: it[labelField] !== undefined && it[labelField] !== null ? String(it[labelField]) : String(it[valueField])
  }))
}

/** 收集 sections 中接口选项来源的 select 字段 */
export function collectApiFields(sections) {
  const arr = []
  for (const sec of (sections || [])) {
    for (const row of (sec.children || [])) {
      for (const cell of (row.cells || [])) {
        for (const f of (cell.fields || [])) {
          if (f && f.type === 'select' && f.optionsSource === 'api' && f.api && f.api.url) arr.push(f)
        }
      }
    }
  }
  return arr
}

/**
 * 接口选项 Hook：监听接口选项字段与表单值，模板参数变化自动重拉
 * @param apiFieldsRef 扁平字段数组 ref（由 collectApiFields 收集）
 * @param formValuesRef 表单值 ref
 * 返回 { apiOptions: {fieldKey:[{value,label}]}, apiLoading: {fieldKey:boolean} }
 */
export function useApiOptions(apiFieldsRef, formValuesRef) {
  const apiOptions = reactive({})
  const apiLoading = reactive({})
  const sigCache = {}

  function fieldSig(api, formValues) {
    const vals = {}
    JSON.stringify(api || {}).replace(/\$\{[^}]+\}/g, (m) => {
      const key = m.replace(/^\$\{(formData\.)?/, '').replace(/}$/, '')
      vals[key] = formValues ? (formValues[key] ?? '') : ''
      return m
    })
    return JSON.stringify(api) + '|' + JSON.stringify(vals)
  }

  async function loadOne(f, formValues) {
    const key = f.field || f.key || f.id
    apiLoading[key] = true
    try {
      apiOptions[key] = await fetchApiOptions(f.api, formValues)
    } catch {
      apiOptions[key] = []
    } finally {
      apiLoading[key] = false
    }
  }

  function refresh() {
    const fv = formValuesRef ? (formValuesRef.value || {}) : {}
    for (const f of (apiFieldsRef.value || [])) {
      const key = f.field || f.key || f.id
      const sig = fieldSig(f.api, fv)
      if (sigCache[key] === sig) continue
      sigCache[key] = sig
      loadOne(f, fv)
    }
  }

  watch([() => apiFieldsRef.value, () => (formValuesRef ? formValuesRef.value : null)], refresh, { deep: true, immediate: true })

  return { apiOptions, apiLoading, refreshApiOptions: refresh }
}
