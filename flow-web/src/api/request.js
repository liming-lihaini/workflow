import axios from 'axios'
import { h } from 'vue'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  // 数组参数序列化为 key=v1&key=v2（不带方括号），以匹配后端 @RequestParam List<Long>
  paramsSerializer: {
    serialize: (params) => {
      const searchParams = new URLSearchParams()
      Object.keys(params || {}).forEach((key) => {
        const value = params[key]
        if (value === null || value === undefined) return
        if (Array.isArray(value)) {
          value.forEach((item) => {
            if (item !== null && item !== undefined && item !== '') {
              searchParams.append(key, String(item))
            }
          })
        } else {
          searchParams.append(key, String(value))
        }
      })
      return searchParams.toString()
    }
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一响应格式: { code, data, message }
    if (res.code !== undefined) {
      if (res.code === 200 || res.code === 0) {
        return res
      }
      // 1037 Token无效 / 1038 Token过期：会话已失效，清登录态并跳转登录页
      if (res.code === 1037 || res.code === 1038) {
        message.error(res.message || '登录已过期，请重新登录')
        localStorage.removeItem('token')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(new Error(res.message || '登录已过期'))
      }
      const err = new Error(res.message || '请求失败')
      err.res = res
      err.code = res.code
      message.error(res.message || '请求失败')
      return Promise.reject(err)
    }
    // 非标准格式直接返回
    return res
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      const msg = data?.message
      if (status === 401) {
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        window.location.href = '/login'
      } else if (status === 403) {
        message.error('权限不足')
      } else if (msg && msg.includes('资源冲突')) {
        // 资源冲突提示含换行符，按信息自动换行展示
        message.error({ content: h('div', { style: 'white-space: pre-line; text-align: left; line-height: 1.6;' }, msg), duration: 6 })
      } else {
        message.error(msg || `请求失败 (${status})`)
      }
    } else {
      message.error('网络异常，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

/**
 * 以 blob 方式下载文件并触发浏览器保存。
 * @param {Promise} requestPromise 返回 blob 的 axios 请求（需配置 responseType:'blob'）
 * @param {string} filename 默认下载文件名
 */
export function downloadBlob(requestPromise, filename) {
  return requestPromise.then((res) => {
    const blob = res instanceof Blob ? res : res.data
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  })
}

export default request
