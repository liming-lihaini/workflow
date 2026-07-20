import request from './request'

// 管理员类型
export function getAdminTypes() {
  return request.get('/system/admin/types')
}

// 三员用户列表
export function getTripleAdminUsers(params) {
  return request.get('/system/admin/users', { params })
}

// 三员审计日志
export function getTripleAdminAuditLogs(params) {
  return request.get('/system/admin/audit-logs', { params })
}
