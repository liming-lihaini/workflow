import request from './request'

// 流程定义
export function getProcessDefinitions(params) {
  return request.get('/process/definitions', { params })
}
export function getProcessDefinition(id) {
  return request.get(`/process/definitions/${id}`)
}
export function getProcessDefinitionByKey(processKey) {
  return request.get(`/process/definitions/key/${processKey}`)
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

// 流程实例
export function getProcessInstances(params) {
  return request.get('/process/instances', { params })
}
export function getProcessInstance(id) {
  return request.get(`/process/instances/${id}`)
}
export function startProcessInstance(data) {
  return request.post('/process/instances', data)
}
export function suspendProcessInstance(id) {
  return request.post(`/process/instances/${id}/suspend`)
}
export function resumeProcessInstance(id) {
  return request.post(`/process/instances/${id}/resume`)
}
export function terminateProcessInstance(id) {
  return request.post(`/process/instances/${id}/terminate`)
}
export function getProcessVariables(id) {
  return request.get(`/process/instances/${id}/variables`)
}
export function updateProcessVariables(id, data) {
  return request.put(`/process/instances/${id}/variables`, data)
}
