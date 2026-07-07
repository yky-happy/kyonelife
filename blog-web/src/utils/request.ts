import axios from 'axios'

// 生产不设置 VITE_API_BASE_URL，走同源相对路径（由 nginx 反代 /api 到后端）；
// 本地开发用 .env.development 里的 http://localhost:8080 直连后端。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

// 读者登录态：附带 user-token（与后台 satoken 隔离）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('kyonelife_user_token')
  if (token) {
    config.headers.set('user-token', token)
  }
  return config
})

request.interceptors.response.use((response) => {
  const result = response.data
  if (result && typeof result === 'object' && 'code' in result) {
    if (result.code === 200) {
      return result.data
    }
    return Promise.reject(new Error(result.message || '请求失败'))
  }
  return result
})

export default request
