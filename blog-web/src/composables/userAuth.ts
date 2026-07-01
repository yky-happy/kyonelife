import { computed, ref } from 'vue'
import { login as apiLogin, register as apiRegister, getMe, logout as apiLogout, type UserInfo } from '../api/auth'

const TOKEN_KEY = 'kyonelife_user_token'

export const userToken = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
export const currentUser = ref<UserInfo | null>(null)
export const isLoggedIn = computed(() => !!currentUser.value)

// 全局登录弹窗开关
export const loginModalOpen = ref(false)
export function openLogin() {
  loginModalOpen.value = true
}
export function closeLogin() {
  loginModalOpen.value = false
}

function setToken(token: string) {
  userToken.value = token
  localStorage.setItem(TOKEN_KEY, token)
}
function clearToken() {
  userToken.value = ''
  currentUser.value = null
  localStorage.removeItem(TOKEN_KEY)
}

let loaded = false
/** 进程内启动时拉一次当前用户（带 token 才拉） */
export async function ensureUser() {
  if (loaded || !userToken.value) return
  loaded = true
  try {
    currentUser.value = await getMe()
  } catch {
    clearToken()
  }
}

export async function doLogin(identifier: string, password: string) {
  const res = await apiLogin(identifier, password)
  setToken(res.token)
  currentUser.value = res.user
  loaded = true
}

export async function doRegister(data: {
  email: string
  code: string
  password: string
  nickname?: string
}) {
  const res = await apiRegister(data)
  setToken(res.token)
  currentUser.value = res.user
  loaded = true
}

export async function doLogout() {
  try {
    await apiLogout()
  } catch {
    // 忽略
  }
  clearToken()
}
