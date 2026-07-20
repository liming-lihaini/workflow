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
export function delegateTask(id, data) {
  return request.post(`/tasks/${id}/delegate`, data)
}
export function counterSignVote(id, data) {
  return request.post(`/tasks/${id}/counter-sign/vote`, data)
}
export function addSign(id, data) {
  return request.post(`/tasks/${id}/add-sign`, data)
}
