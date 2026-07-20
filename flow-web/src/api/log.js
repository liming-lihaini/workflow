import request from './request'

// 访问日志
export function getAccessLogs(params) {
  return request.get('/system/logs/access', { params })
}
export function exportAccessLogs(params) {
  return request.get('/system/logs/access/export', { params })
}

// 操作日志
export function getOperationLogs(params) {
  return request.get('/system/logs/operation', { params })
}
export function exportOperationLogs(params) {
  return request.get('/system/logs/operation/export', { params })
}

// 清理日志
export function cleanLogs(data) {
  return request.post('/system/logs/clean', data)
}
