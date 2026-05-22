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
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data.code === 200) return data
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(data)
  },
  (err) => {
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
