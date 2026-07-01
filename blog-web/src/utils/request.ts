import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',
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
