import request from './request'

// 表单定义
export function getFormList(params) {
  return request.get('/forms', { params })
}

export function getFormAll() {
  return request.get('/forms/all')
}

export function getForm(formKey) {
  return request.get(`/forms/${formKey}`)
}

export function createForm(data) {
  return request.post('/forms', data)
}

export function updateForm(formKey, data) {
  return request.put(`/forms/${formKey}`, data)
}

export function deleteForm(formKey) {
  return request.delete(`/forms/${formKey}`)
}
