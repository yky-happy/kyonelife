import request from '@/utils/request'

export interface Collection {
  id: number
  name: string
  cover: string
  description: string
  sort: number
  createTime: string
  articleCount: number
}

export interface CollectionSaveDTO {
  name: string
  cover?: string
  description?: string
  sort?: number
}

export const getCollectionPage = (params: { page: number; size: number; keyword?: string }) =>
  request.get('/admin/collection/page', { params })

export const saveCollection = (data: CollectionSaveDTO) =>
  request.post('/admin/collection', data)

export const updateCollection = (id: number, data: CollectionSaveDTO) =>
  request.put(`/admin/collection/${id}`, data)

export const deleteCollection = (id: number) =>
  request.delete(`/admin/collection/${id}`)
