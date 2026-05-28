import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: 'http://localhost:8080',
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
