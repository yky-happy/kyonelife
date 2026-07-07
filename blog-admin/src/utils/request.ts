import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// 生产不设置 VITE_API_BASE_URL，走同源相对路径（由 nginx 反代 /api、/admin 到后端）；
// 本地开发用 .env.development 里的 http://localhost:8080 直连后端。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers['Authorization'] = auth.token
  }
  if (import.meta.env.DEV) {
    console.debug('[API Request]', {
      method: config.method?.toUpperCase(),
      url: config.url,
      params: config.params,
      data: config.data,
    })
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code === 200) {
      if (import.meta.env.DEV) {
        console.debug('[API Response]', {
          method: res.config.method?.toUpperCase(),
          url: res.config.url,
          data,
        })
      }
      return data
    }
    if (import.meta.env.DEV) {
      console.error('[API Business Error]', {
        method: res.config.method?.toUpperCase(),
        url: res.config.url,
        params: res.config.params,
        requestData: res.config.data,
        response: data,
      })
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (err) => {
    if (import.meta.env.DEV) {
      console.error('[API Network Error]', {
        method: err.config?.method?.toUpperCase(),
        url: err.config?.url,
        params: err.config?.params,
        requestData: err.config?.data,
        status: err.response?.status,
        response: err.response?.data,
        message: err.message,
      })
    }
    if (err.response?.status === 401) {
      useAuthStore().clearAuth()
      router.push('/login')
    } else {
      ElMessage.error('网络异常，请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default request
