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

export interface ArticleTrend {
  date: string
  viewCount: number
  visitorCount: number
}

export interface AnalyticsRank {
  id: number | null
  name: string
  count: number
  visitorCount: number
}

export const getAnalyticsOverview = () =>
  request.get('/admin/analytics/overview')

export const getAnalyticsTrend = (params?: { days?: number }) =>
  request.get('/admin/analytics/trend', { params })

export const getHotArticles = (params?: { days?: number; limit?: number }) =>
  request.get('/admin/analytics/hot-articles', { params })

export const getArticleTrend = (params?: { days?: number }) =>
  request.get('/admin/analytics/article-trend', { params })

export const getHotTags = (params?: { days?: number; limit?: number }) =>
  request.get('/admin/analytics/hot-tags', { params })

export const getHotCollections = (params?: { days?: number; limit?: number }) =>
  request.get('/admin/analytics/hot-collections', { params })

export const getHotKeywords = (params?: { days?: number; limit?: number }) =>
  request.get('/admin/analytics/hot-keywords', { params })
