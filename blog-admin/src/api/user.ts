import request from '@/utils/request'

export interface ReaderItem {
  id: number
  account: string
  email: string
  nickname: string
  status: number
  ipLocation: string
  lastLoginTime: string
  createTime: string
}

export const getUserPage = (params: {
  page: number
  size: number
  keyword?: string
  status?: number | null
}) => request.get('/admin/user/page', { params })

export const updateUserStatus = (id: number, status: number) =>
  request.patch(`/admin/user/${id}/status`, null, { params: { status } })

export const deleteUser = (id: number) => request.delete(`/admin/user/${id}`)
