import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const isAdmin = ref(localStorage.getItem('isAdmin') === 'true')
  const roles = ref(JSON.parse(localStorage.getItem('roles') || '[]'))

  /** 判断是否拥有指定权限 */
  function hasPermission(key) {
    if (isAdmin.value) return true
    return permissions.value.includes(key)
  }

  /** 登录后拉取用户信息 */
  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      const data = res.data || res
      permissions.value = data.permissions || []
      isAdmin.value = !!data.isAdmin
      roles.value = data.roles || []
      localStorage.setItem('permissions', JSON.stringify(permissions.value))
      localStorage.setItem('isAdmin', String(isAdmin.value))
      localStorage.setItem('roles', JSON.stringify(roles.value))
    } catch (e) {
      console.warn('获取用户信息失败', e)
    }
  }

  async function login(loginData) {
    const res = await loginApi(loginData)
    token.value = res.data?.token || res.token || 'mock-token'
    username.value = loginData.username
    localStorage.setItem('token', token.value)
    localStorage.setItem('username', username.value)
    await fetchUserInfo()
    return res
  }

  function logout() {
    token.value = ''
    username.value = ''
    permissions.value = []
    isAdmin.value = false
    roles.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('permissions')
    localStorage.removeItem('isAdmin')
    localStorage.removeItem('roles')
  }

  return { token, username, permissions, isAdmin, roles, hasPermission, login, logout, fetchUserInfo }
})
