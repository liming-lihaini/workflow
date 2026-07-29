<template>
  <div class="data-ref-renderer">
    <!-- 加载中 -->
    <template v-if="loading">
      <a-skeleton :active="true" :paragraph="{ rows: display.mode === 'table' ? 3 : 1 }" />
    </template>
    <!-- 错误状态 -->
    <template v-else-if="error">
      <div class="dataref-error">
        <span class="error-text">{{ error }}</span>
        <a-button type="link" size="small" @click="fetchData">重试</a-button>
      </div>
    </template>
    <!-- 空数据 -->
    <template v-else-if="isEmpty">
      <div class="dataref-empty">暂无数据</div>
    </template>
    <!-- text 模式：单文本 -->
    <template v-else-if="display.mode === 'text'">
      <span class="dataref-text">{{ escapeHtml(textValue) || '-' }}</span>
    </template>
    <!-- texts 模式：多文本平铺 -->
    <template v-else-if="display.mode === 'texts'">
      <div class="dataref-texts">
        <div v-for="(f, idx) in display.fields" :key="idx" class="dataref-text-item">
          <span class="text-label">{{ f.label }}:</span>
          <span class="text-value">{{ escapeHtml(resolvePath(fetchedData, f.path)) }}</span>
        </div>
      </div>
    </template>
    <!-- table 模式：表格 -->
    <template v-else-if="display.mode === 'table'">
      <a-table
        :columns="tableColumns"
        :data-source="tableData"
        :pagination="false"
        size="small"
        :scroll="{ y: 200 }"
        row-key="__dr_idx"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import request from '../api/request'

const props = defineProps({
  /** data-ref 字段配置 */
  field: { type: Object, required: true },
  /** 当前表单值 */
  formValues: { type: Object, default: () => ({}) },
  /** 只读模式 */
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update-value'])

const loading = ref(false)
const error = ref('')
const fetchedData = ref(null)
const isEmpty = ref(false)

// Shorthand
const dataSource = computed(() => props.field.dataSource || {})
const display = computed(() => props.field.display || { mode: 'text' })

// --- Path resolution utility ---
function resolvePath(obj, path) {
  if (!obj || !path) return undefined
  const parts = path.split('.')
  let cur = obj
  for (const p of parts) {
    if (cur === null || cur === undefined) return undefined
    cur = cur[p]
  }
  return cur
}

// --- Template parameter substitution ---
function resolveTemplate(str) {
  if (typeof str !== 'string') return str
  const fv = props.formValues || {}
  const user = getUserInfo()
  return str.replace(/\$\{(\w+)\}/g, (_, name) => {
    // Fixed user variables
    if (name === 'userId') return user.userId || ''
    if (name === 'userAccount') return user.userAccount || ''
    if (name === 'userName') return user.userName || ''
    // Form field values
    return fv[name] !== undefined ? String(fv[name]) : ''
  })
}

function resolveParams(params) {
  if (!params || typeof params !== 'object') return {}
  const result = {}
  for (const [key, rawVal] of Object.entries(params)) {
    result[key] = resolveTemplate(rawVal)
  }
  return result
}

/**
 * 当前用户信息取值：登录后 /auth/info 返回的 userId(用户ID)、username(登录账号)、
 * realName(用户姓名) 由 user store 写入 localStorage，此处直接读取
 */
function getUserInfo() {
  try {
    return {
      userId: localStorage.getItem('userId') || '',
      userAccount: localStorage.getItem('username') || '',
      userName: localStorage.getItem('realName') || ''
    }
  } catch { return { userId: '', userAccount: '', userName: '' } }
}

// --- Data fetching ---
let abortController = null

function processData(res) {
  // Resolve data by dataPath
  const dataPath = dataSource.value.dataPath || 'data'
  let data = resolvePath(res, dataPath)
  if (data === undefined) data = resolvePath(res, 'data')
  if (data === undefined) data = res

  fetchedData.value = data
  isEmpty.value = (data === null || data === undefined || (Array.isArray(data) && data.length === 0))

  // Write back to form
  writeBackToForm(data)
}

async function fetchData() {
  const rawUrl = dataSource.value.url
  if (!rawUrl) { isEmpty.value = true; return }
  // 接口路径参数替换：支持 ${字段标识} 及 ${userId}/${userAccount}/${userName}
  const url = resolveTemplate(rawUrl)

  loading.value = true
  error.value = ''
  isEmpty.value = false

  try {
    const method = (dataSource.value.method || 'GET').toUpperCase()
    const params = resolveParams(dataSource.value.params)
    const headers = dataSource.value.headers || {}

    // Handle URL: avoid double baseURL prefix when URL already starts with /api/
    if (url.startsWith('/api/') || url.startsWith('http://') || url.startsWith('https://')) {
      const fetchOpts = { method: method, headers: { ...headers } }
      const token = localStorage.getItem('token')
      if (token) fetchOpts.headers['Authorization'] = `Bearer ${token}`

      if (method === 'GET') {
        const qs = new URLSearchParams(params).toString()
        const fullUrl = qs ? `${url}?${qs}` : url
        const resp = await fetch(fullUrl, fetchOpts)
        if (!resp.ok) throw new Error(`请求失败 (${resp.status})`)
        processData(await resp.json())
      } else {
        fetchOpts.headers['Content-Type'] = 'application/json'
        fetchOpts.body = JSON.stringify(params)
        const resp = await fetch(url, fetchOpts)
        if (!resp.ok) throw new Error(`请求失败 (${resp.status})`)
        processData(await resp.json())
      }
    } else {
      // Normal relative URL: use axios request instance
      const config = { url, method, headers }
      if (method === 'GET') {
        config.params = params
      } else {
        config.data = params
      }
      const res = await request(config)
      processData(res)
    }
  } catch (e) {
    error.value = e.message || '请求失败'
    fetchedData.value = null
  } finally {
    loading.value = false
  }
}

// --- Write back ---
function writeBackToForm(data) {
  if (!props.field.bindToForm) return
  const mode = display.value.mode
  const fieldKey = props.field.field

  if (mode === 'text') {
    const val = resolvePath(data, display.value.valuePath)
    emit('update-value', val !== undefined ? val : '')
  } else if (mode === 'texts') {
    const obj = {}
    for (const f of (display.value.fields || [])) {
      obj[f.path] = resolvePath(data, f.path)
    }
    emit('update-value', obj)
  } else if (mode === 'table') {
    const arr = Array.isArray(data) ? data : []
    emit('update-value', arr)
  }
}

// --- Computed values for rendering ---
const textValue = computed(() => {
  if (!fetchedData.value) return ''
  return resolvePath(fetchedData.value, display.value.valuePath) ?? ''
})

const tableColumns = computed(() => {
  return (display.value.columns || []).map(col => ({
    title: col.label,
    dataIndex: col.path,
    key: col.path,
    ellipsis: true,
    customRender: col.type === 'tag' ? ({ text }) => renderTag(text, col.optionsJson) : undefined
  }))
})

const tableData = computed(() => {
  if (!Array.isArray(fetchedData.value)) return []
  return fetchedData.value.map((row, idx) => {
    const mapped = { __dr_idx: idx }
    for (const col of (display.value.columns || [])) {
      mapped[col.path] = resolvePath(row, col.path)
    }
    return mapped
  })
})

function renderTag(value, optionsJson) {
  let map = {}
  try { map = JSON.parse(optionsJson || '{}') } catch { /* ignore */ }
  const label = map[String(value)] !== undefined ? map[String(value)] : value
  return String(label)
}

// --- HTML escape ---
function escapeHtml(str) {
  if (str === null || str === undefined) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// --- Watch: watch fields trigger re-fetch with debounce ---
let debounceTimer = null

watch(
  () => {
    // Build a reactive key from watch fields + resolved url (path params react to form changes)
    const watchKeys = dataSource.value.watch || []
    const fv = props.formValues || {}
    return watchKeys.map(k => fv[k]).join('|') + '||' + resolveTemplate(dataSource.value.url || '')
  },
  () => {
    if (!dataSource.value.url) return
    clearTimeout(debounceTimer)
    debounceTimer = setTimeout(() => { fetchData() }, 300)
  },
  { immediate: false }
)

// Initial fetch
onMounted(() => {
  if (dataSource.value.url) {
    fetchData()
  } else {
    isEmpty.value = true
  }
})

onUnmounted(() => {
  clearTimeout(debounceTimer)
})
</script>

<style scoped>
.data-ref-renderer { min-height: 24px; }
.dataref-text { font-size: 14px; color: #333; padding: 4px 0; display: inline-block; }
.dataref-texts { display: flex; flex-direction: column; gap: 4px; }
.dataref-text-item { display: flex; gap: 6px; font-size: 13px; line-height: 1.6; }
.text-label { color: #888; white-space: nowrap; }
.text-value { color: #333; }
.dataref-empty { color: #999; font-size: 13px; padding: 8px 0; text-align: center; }
.dataref-error { display: flex; align-items: center; gap: 4px; padding: 4px 0; }
.error-text { color: #ff4d4f; font-size: 13px; }
</style>
