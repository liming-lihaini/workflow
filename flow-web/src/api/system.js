import request from './request'

// 部门管理
export function getDepts(params) {
  return request.get('/system/depts', { params })
}
export function getDeptsPage(params) {
  return request.get('/system/depts/page', { params })
}
export function getDeptTree() {
  return request.get('/system/depts/tree')
}
export function getDept(id) {
  return request.get(`/system/depts/${id}`)
}
export function createDept(data) {
  return request.post('/system/depts', data)
}
export function updateDept(id, data) {
  return request.put(`/system/depts/${id}`, data)
}
export function deleteDept(id) {
  return request.delete(`/system/depts/${id}`)
}
export function setDeptLeader(id, data) {
  return request.put(`/system/depts/${id}/leader`, data)
}

// 用户管理
export function getUsers(params) {
  return request.get('/system/users', { params })
}
export function getUsersPage(params) {
  return request.get('/system/users/page', { params })
}
export function getUser(id) {
  return request.get(`/system/users/${id}`)
}
export function createUser(data) {
  return request.post('/system/users', data)
}
export function updateUser(id, data) {
  return request.put(`/system/users/${id}`, data)
}
export function deleteUser(id) {
  return request.delete(`/system/users/${id}`)
}
export function resetPassword(id, data) {
  return request.post(`/system/users/${id}/reset-pwd`, data)
}
export function getUserPosts(id) {
  return request.get(`/system/users/${id}/posts`)
}
export function addUserPost(id, data) {
  return request.post(`/system/users/${id}/posts`, data)
}
export function deleteUserPost(id, postId) {
  return request.delete(`/system/users/${id}/posts/${postId}`)
}

// 角色管理
export function getRoles(params) {
  return request.get('/system/roles', { params })
}
export function getRole(id) {
  return request.get(`/system/roles/${id}`)
}
export function createRole(data) {
  return request.post('/system/roles', data)
}
export function updateRole(id, data) {
  return request.put(`/system/roles/${id}`, data)
}
export function deleteRole(id) {
  return request.delete(`/system/roles/${id}`)
}
export function getRoleUsers(id) {
  return request.get(`/system/roles/${id}/users`)
}
export function assignRoleUsers(id, data) {
  return request.post(`/system/roles/${id}/users`, data)
}

// 权限管理
export function getPermissions(params) {
  return request.get('/system/permissions', { params })
}
export function createPermission(data) {
  return request.post('/system/permissions', data)
}
export function updatePermission(id, data) {
  return request.put(`/system/permissions/${id}`, data)
}
export function deletePermission(id) {
  return request.delete(`/system/permissions/${id}`)
}
export function getRolePermissions(id) {
  return request.get(`/system/roles/${id}/permissions`)
}
export function assignRolePermissions(id, data) {
  return request.put(`/system/roles/${id}/permissions`, data)
}
export function setRoleDataScope(id, data) {
  return request.put(`/system/roles/${id}/data-scope`, data)
}
