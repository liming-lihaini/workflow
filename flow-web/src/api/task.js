import request from './request'

export function getTodoTasks(params) {
  return request.get('/tasks/todo', { params })
}
export function getDoneTasks(params) {
  return request.get('/tasks/done', { params })
}
export function getTaskDetail(id) {
  return request.get(`/tasks/${id}`)
}
export function getTasksByInstance(instanceId) {
  return request.get(`/tasks/instance/${instanceId}`)
}
export function claimTask(id, data) {
  return request.post(`/tasks/${id}/claim`, data)
}
export function unclaimTask(id) {
  return request.post(`/tasks/${id}/unclaim`)
}
export function completeTask(id, data) {
  return request.post(`/tasks/${id}/complete`, data)
}
export function rejectTask(id, data) {
  return request.post(`/tasks/${id}/reject`, data)
}
export function transferTask(id, data) {
  return request.post(`/tasks/${id}/transfer`, data)
}
export function counterSignVote(id, data) {
  return request.post(`/tasks/${id}/counter-sign/vote`, data)
}
export function addSign(id, data) {
  return request.post(`/tasks/${id}/add-sign`, data)
}
export function getFormPermissions(taskId) {
  return request.get(`/tasks/${taskId}/form-permissions`)
}

// 全局委托相关
export function createDelegation(data) {
  return request.post('/delegations', data)
}
export function getMyDelegations(params) {
  return request.get('/delegations/my', { params })
}
export function getProxyDelegations(params) {
  return request.get('/delegations/proxy', { params })
}
export function cancelDelegation(id, data) {
  return request.post(`/delegations/${id}/cancel`, data)
}

// 用户搜索
export function searchUsers(params) {
  return request.get('/system/users/page', { params })
}
