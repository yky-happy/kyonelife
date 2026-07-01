import request from '@/utils/request'

export interface AdminItem {
  id: number
  username: string
  nickname: string
  status: number
  lastLoginTime: string
  createTime: string
  roleIds: number[]
  roleNames: string[]
}

export interface AdminSaveDTO {
  username: string
  nickname: string
  password: string
  roleIds: number[]
}

export interface AdminUpdateDTO {
  nickname: string
  password?: string
  roleIds: number[]
}

export const getAdminPage = (params: { page: number; size: number; keyword?: string }) =>
  request.get('/admin/admin/page', { params })

export const saveAdmin = (data: AdminSaveDTO) => request.post('/admin/admin', data)

export const updateAdmin = (id: number, data: AdminUpdateDTO) => request.put(`/admin/admin/${id}`, data)

export const updateAdminStatus = (id: number, status: number) =>
  request.patch(`/admin/admin/${id}/status`, null, { params: { status } })

export const deleteAdmin = (id: number) => request.delete(`/admin/admin/${id}`)
