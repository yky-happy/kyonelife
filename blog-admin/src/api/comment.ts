import request from '@/utils/request'

export interface AdminCommentItem {
  id: number
  articleId: number
  articleTitle: string
  parentId: number | null
  userId: number
  nickname: string
  content: string
  status: number
  createTime: string
}

export const getCommentPage = (params: {
  page: number
  size: number
  articleId?: number
  keyword?: string
}) => request.get('/admin/comment/page', { params })

export const deleteComment = (id: number) => request.delete(`/admin/comment/${id}`)

export const updateCommentStatus = (id: number, status: number) =>
  request.patch(`/admin/comment/${id}/status`, null, { params: { status } })
