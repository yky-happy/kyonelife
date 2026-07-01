import request from '@/utils/request'

export interface MenuNode {
  id: number
  parentId: number
  title: string
  type: string
  path?: string
  component?: string
  perm?: string
  icon?: string
  sort: number
  hidden: number
  children?: MenuNode[]
}

export interface MenuSaveDTO {
  parentId?: number
  title: string
  type: string
  path?: string
  component?: string
  perm?: string
  icon?: string
  sort?: number
  hidden?: number
}

export const getMenuTree = () => request.get('/admin/menu/tree')

export const saveMenu = (data: MenuSaveDTO) => request.post('/admin/menu', data)

export const updateMenu = (id: number, data: MenuSaveDTO) => request.put(`/admin/menu/${id}`, data)

export const deleteMenu = (id: number) => request.delete(`/admin/menu/${id}`)
