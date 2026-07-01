import request from '../utils/request'
import type { ArchiveMonth, ArticleCard, ArticleDetail, PageResult } from './types'

export interface ArticlePageParams {
  page?: number
  size?: number
  keyword?: string
  tagId?: number
  collectionId?: number
}

export const getArticlePage = (params: ArticlePageParams) =>
  request.get<unknown, PageResult<ArticleCard>>('/api/article/page', { params })

export const getArticleDetail = (id: number) =>
  request.get<unknown, ArticleDetail>(`/api/article/${id}`)

export const getArticleArchive = () =>
  request.get<unknown, ArchiveMonth[]>('/api/article/archive')

export const getHotArticles = (limit = 5) =>
  request.get<unknown, ArticleCard[]>('/api/article/hot', { params: { limit } })

export const getRelatedArticles = (id: number, limit = 6) =>
  request.get<unknown, ArticleCard[]>(`/api/article/${id}/related`, { params: { limit } })

export interface LikeStatus {
  liked: boolean
  likeCount: number
}

export const getLikeStatus = (id: number, visitorId: string) =>
  request.get<unknown, LikeStatus>(`/api/article/${id}/like-status`, { params: { visitorId } })

export const toggleLike = (id: number, visitorId: string) =>
  request.post<unknown, LikeStatus>(`/api/article/${id}/like`, { visitorId })
