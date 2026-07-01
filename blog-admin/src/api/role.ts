import request from '@/utils/request'

export interface Role {
  id: number
  code: string
  name: string
  remarks: string
  createTime: string
}

export interface RoleSaveDTO {
  code: string
  name: string
  remarks?: string
}

export const getRolePage = (params: { page: number; size: number; keyword?: string }) =>
  request.get('/admin/role/page', { params })

export const getRoleList = () => request.get('/admin/role/list')

export const saveRole = (data: RoleSaveDTO) => request.post('/admin/role', data)

export const updateRole = (id: number, data: RoleSaveDTO) => request.put(`/admin/role/${id}`, data)

export const deleteRole = (id: number) => request.delete(`/admin/role/${id}`)

export const getRoleMenus = (id: number) => request.get(`/admin/role/${id}/menus`)

export const assignRoleMenus = (id: number, menuIds: number[]) =>
  request.put(`/admin/role/${id}/menus`, { menuIds })
