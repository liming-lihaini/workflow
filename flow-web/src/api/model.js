import request from './request'

// 数据模型
export function getDataModelList(params) {
  return request.get('/data-models', { params })
}

export function getDataModel(modelKey) {
  return request.get(`/data-models/${modelKey}`)
}

export function createDataModel(data) {
  return request.post('/data-models', data)
}

export function updateDataModel(modelKey, data) {
  return request.put(`/data-models/${modelKey}`, data)
}

export function deleteDataModel(modelKey) {
  return request.delete(`/data-models/${modelKey}`)
}

export function publishDataModel(modelKey) {
  return request.post(`/data-models/${modelKey}/publish`)
}

export function getDataModelFormFields(modelKey) {
  return request.get(`/data-models/${modelKey}/form-fields`)
}
