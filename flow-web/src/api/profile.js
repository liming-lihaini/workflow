import request from './request'

// 个人信息
export function getProfile() {
  return request.get('/profile')
}

export function updateProfile(data) {
  return request.put('/profile', data)
}

export function changePassword(data) {
  return request.put('/profile/password', data)
}

// 个人API Token管理
export function listApiTokens() {
  return request.get('/profile/tokens')
}

export function createApiToken(data) {
  return request.post('/profile/tokens', data)
}

export function deleteApiToken(id) {
  return request.delete(`/profile/tokens/${id}`)
}
