import request from '../utils/request'
import type { ArticleCard, CollectionItem, PageResult } from './types'

export const getCollectionList = () =>
  request.get<unknown, CollectionItem[]>('/api/collection/list')

export const getCollectionArticles = (id: number, params: { page?: number; size?: number }) =>
  request.get<unknown, PageResult<ArticleCard>>(`/api/collection/${id}/articles`, { params })
