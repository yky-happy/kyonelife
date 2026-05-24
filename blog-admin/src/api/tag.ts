import request from '@/utils/request'

export interface Tag {
  id: number
  name: string
  color: string
  createTime: string
  articleCount: number
}

export interface TagSaveDTO {
  name: string
  color?: string
}

export const getTagPage = (params: { page: number; size: number; keyword?: string }) =>
  request.get('/admin/tag/page', { params })

export const saveTag = (data: TagSaveDTO) =>
  request.post('/admin/tag', data)

export const updateTag = (id: number, data: TagSaveDTO) =>
  request.put(`/admin/tag/${id}`, data)

export const deleteTag = (id: number) =>
  request.delete(`/admin/tag/${id}`)
