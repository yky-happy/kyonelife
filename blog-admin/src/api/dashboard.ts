import request from '@/utils/request'

export interface DashboardStats {
  articleCount: number
  publishedCount: number
  draftCount: number
  tagCount: number
  collectionCount: number
  userCount: number
  totalViews: number
}

export const getDashboardStats = () => request.get('/admin/dashboard/stats')
