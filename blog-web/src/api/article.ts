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
