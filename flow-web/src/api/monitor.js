import request from './request'

// 执行轨迹
export function getExecutionHistory(id) {
  return request.get(`/monitor/instances/${id}/history`)
}

// 变量历史
export function getVariableHistory(id) {
  return request.get(`/monitor/instances/${id}/variables`)
}

// 耗时统计
export function getMonitorStatistics(id) {
  return request.get(`/monitor/instances/${id}/statistics`)
}

// 运行中的流程
export function getRunningProcesses(params) {
  return request.get('/monitor/running', { params })
}

// 导出数据
export function exportMonitorData(id) {
  return request.get(`/monitor/instances/${id}/export`)
}

// 管理员干预
export function interveneProcess(id, data) {
  return request.post(`/monitor/instances/${id}/intervene`, null, {
    params: { targetNodeId: data.targetNodeId, operatorId: 1, reason: data.reason }
  })
}
