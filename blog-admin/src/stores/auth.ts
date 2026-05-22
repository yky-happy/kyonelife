import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') ?? '')
  const nickname = ref(localStorage.getItem('nickname') ?? '')
  const adminId = ref(localStorage.getItem('adminId') ?? '')

  function setAuth(data: { token: string; nickname: string; id: number }) {
    token.value = data.token
    nickname.value = data.nickname
    adminId.value = String(data.id)
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('adminId', String(data.id))
  }

  function clearAuth() {
    token.value = ''
    nickname.value = ''
    adminId.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('adminId')
  }

  return { token, nickname, adminId, setAuth, clearAuth }
})
