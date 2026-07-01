import request from '../utils/request'
import { getVisitorId } from '../utils/visitor'

export interface CommentItem {
  id: number
  parentId: number | null
  content: string
  userId: number
  nickname: string
  avatar: string | null
  createTime: string
  likeCount: number
  liked: boolean
  replies?: CommentItem[]
}

export interface CommentLikeResult {
  liked: boolean
  likeCount: number
}

export interface CommentPage {
  records: CommentItem[]
  total: number
  size: number
  current: number
  pages: number
}

export const getComments = (articleId: number, page = 1, size = 10) =>
  request.get<unknown, CommentPage>('/api/comment/list', {
    params: { articleId, page, size, visitorId: getVisitorId() },
  })

export const addComment = (articleId: number, content: string, parentId?: number | null) =>
  request.post<unknown, CommentItem>('/api/comment', { articleId, content, parentId: parentId ?? null })

export const likeComment = (commentId: number) =>
  request.post<unknown, CommentLikeResult>(`/api/comment/${commentId}/like`, {
    visitorId: getVisitorId(),
  })
