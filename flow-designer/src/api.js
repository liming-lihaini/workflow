import axios from 'axios'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

// 当 flow-designer 被内嵌在 flow-web 的 iframe 中时，
// localStorage 不跨域共享，通过 postMessage 向父窗口请求 token
let parentToken = null
let tokenRequested = false

function requestParentToken() {
  if (tokenRequested) return
  tokenRequested = true
  if (window.parent && window.parent !== window) {
    window.parent.postMessage({ type: 'FLOW_DESIGNER_TOKEN_REQUEST' }, '*')
  }
}

// 监听父窗口返回的 token
window.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'FLOW_DESIGNER_TOKEN_RESPONSE' && event.data.token) {
    parentToken = event.data.token
    localStorage.setItem('token', event.data.token) // 缓存到本地
  }
})

// 请求拦截器
request.interceptors.request.use((config) => {
  // 优先用本地缓存，没有时触发父窗口请求
  let token = localStorage.getItem('token')
  if (!token && window.parent && window.parent !== window) {
    requestParentToken()
    token = parentToken
  } else if (parentToken) {
    token = parentToken
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined) {
      if (res.code === 200 || res.code === 0) return res
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      parentToken = null
      // 重新请求父窗口 token
      tokenRequested = false
      requestParentToken()
    }
    return Promise.reject(error)
  }
)

// 流程定义 API
export function getProcessDefinitions(params) {
  return request.get('/process/definitions', { params })
}

export function getProcessDefinition(id) {
  return request.get(`/process/definitions/${id}`)
}

export function createProcessDefinition(data) {
  return request.post('/process/definitions', data)
}

export function updateProcessDefinition(id, data) {
  return request.put(`/process/definitions/${id}`, data)
}

export function deleteProcessDefinition(id) {
  return request.delete(`/process/definitions/${id}`)
}

export function deployProcessDefinition(id) {
  return request.post(`/process/definitions/${id}/deploy`)
}

export function exportProcessDefinition(id) {
  return request.get(`/process/definitions/${id}/export`)
}

export function importProcessDefinition(data) {
  return request.post('/process/definitions/import', data)
}

// 表单 API
export function getFormAll() {
  return request.get('/forms/all')
}

export function getForm(formKey) {
  return request.get(`/forms/${formKey}`)
}

// 用户 API
export function getUsersPage(params) {
  return request.get('/system/users/page', { params })
}

// 角色 API
export function getRoles(params) {
  return request.get('/system/roles', { params })
}

// Webhook API
export function getWebhooks(params) {
  return request.get('/webhooks', { params })
}
export function createWebhook(data) {
  return request.post('/webhooks', data)
}
export function updateWebhook(webhookKey, data) {
  return request.put(`/webhooks/${webhookKey}`, data)
}
export function deleteWebhook(webhookKey) {
  return request.delete(`/webhooks/${webhookKey}`)
}
export function testWebhook(webhookKey, data) {
  return request.post(`/webhooks/${webhookKey}/test`, data || {})
}

export default request
