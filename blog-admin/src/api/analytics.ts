import request from '@/utils/request'

export interface AnalyticsOverview {
  todayPv: number
  todayUv: number
  totalPv: number
  articleViewCount: number
}

export interface AnalyticsTrend {
  date: string
  pv: number
  uv: number
}

export interface HotArticle {
  articleId: number
  title: string
  viewCount: number
}

export const getAnalyticsOverview = () =>
  request.get('/admin/analytics/overview')

export const getAnalyticsTrend = (params?: { days?: number }) =>
  request.get('/admin/analytics/trend', { params })

export const getHotArticles = (params?: { days?: number; limit?: number }) =>
  request.get('/admin/analytics/hot-articles', { params })
