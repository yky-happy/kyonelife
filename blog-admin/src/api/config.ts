import request from '@/utils/request'

export interface WebConfig {
  id?: number
  siteName: string
  logo: string
  summary: string
  author: string
  authorAvatar: string
  signature: string
  github: string
  email: string
  aboutMe: string
  icpNumber: string
  bulletin: string
}

export const getConfig = () => request.get('/admin/config')

export const updateConfig = (data: Partial<WebConfig>) => request.put('/admin/config', data)
