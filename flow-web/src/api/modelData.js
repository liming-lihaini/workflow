import request from './request'

// 模型业务数据（数据模型生成表后的通用 CRUD）

// 模型数据动态菜单
export function getModelMenus() {
  return request.get('/model-data/menus')
}

// 分页查询（keyword/page/size）
export function getModelDataPage(modelKey, params) {
  return request.get(`/model-data/${modelKey}`, { params })
}

// 详情（含子表数据）
export function getModelDataDetail(modelKey, id) {
  return request.get(`/model-data/${modelKey}/${id}`)
}

export function createModelData(modelKey, data) {
  return request.post(`/model-data/${modelKey}`, data)
}

export function updateModelData(modelKey, id, data) {
  return request.put(`/model-data/${modelKey}/${id}`, data)
}

export function deleteModelData(modelKey, id) {
  return request.delete(`/model-data/${modelKey}/${id}`)
}
