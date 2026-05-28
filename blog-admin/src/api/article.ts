import request from '@/utils/request'
import type { Tag } from '@/api/tag'

export interface Article {
  id: number
  title: string
  cover: string
  summary: string
  collectionId: number | null
  collectionName: string
  tags: Tag[]
  status: number
  isStick: number
  isCarousel: number
  viewCount: number
  createTime: string
  updateTime: string
}

export interface ArticleDetail extends Article {
  content: string
  contentMd: string
  keywords: string
  aiDescribe: string
  originalUrl: string
  carouselSort: number
  isOriginal: number
  tagIds: number[]
}

export interface ArticleSaveDTO {
  title: string
  cover?: string
  summary?: string
  content?: string
  contentMd: string
  keywords?: string
  aiDescribe?: string
  collectionId?: number | null
  tagIds?: number[]
  status: number
  isStick?: number
  isCarousel?: number
  carouselSort?: number
  isOriginal: number
  originalUrl?: string
}

export interface ArticlePageParams {
  page: number
  size: number
  keyword?: string
  status?: number
}

export const getArticlePage = (params: ArticlePageParams) =>
  request.get('/admin/article/page', { params })

export const getArticleDetail = (id: number) =>
  request.get(`/admin/article/${id}`)

export const saveArticle = (data: ArticleSaveDTO) =>
  request.post('/admin/article', data)

export const updateArticle = (id: number, data: ArticleSaveDTO) =>
  request.put(`/admin/article/${id}`, data)

export const updateArticleStatus = (id: number, status: number) =>
  request.patch(`/admin/article/${id}/status`, { status })

export const deleteArticle = (id: number) =>
  request.delete(`/admin/article/${id}`)
