import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const userInfo = ref(null)

  async function login(loginData) {
    const res = await loginApi(loginData)
    token.value = res.data?.token || res.token || 'mock-token'
    username.value = loginData.username
    localStorage.setItem('token', token.value)
    localStorage.setItem('username', username.value)
    return res
  }

  function logout() {
    token.value = ''
    username.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
  }

  return { token, username, userInfo, login, logout }
})
