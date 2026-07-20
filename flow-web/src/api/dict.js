import request from './request'

// 字典类型
export function getDictTypes(params) {
  return request.get('/system/dict/types', { params })
}
export function getDictType(id) {
  return request.get(`/system/dict/types/${id}`)
}
export function createDictType(data) {
  return request.post('/system/dict/types', data)
}
export function updateDictType(id, data) {
  return request.put(`/system/dict/types/${id}`, data)
}
export function deleteDictType(id) {
  return request.delete(`/system/dict/types/${id}`)
}

// 字典项
export function getDictItems(params) {
  return request.get('/system/dict/items', { params })
}
export function getDictItemsByTypeId(typeId) {
  return request.get(`/system/dict/items/type/${typeId}`)
}
export function getDictItemsByCode(dictCode) {
  return request.get(`/system/dict/items/code/${dictCode}`)
}
export function createDictItem(data) {
  return request.post('/system/dict/items', data)
}
export function updateDictItem(id, data) {
  return request.put(`/system/dict/items/${id}`, data)
}
export function deleteDictItem(id) {
  return request.delete(`/system/dict/items/${id}`)
}
