import request from '@/utils/request'

export interface Banner {
  id: number
  imageUrl: string
  linkUrl: string
  title: string
  sort: number
  status: number
  createTime: string
}

export interface BannerSaveDTO {
  imageUrl: string
  linkUrl?: string
  title?: string
  sort?: number
  status?: number
}

export const getBannerPage = (params: { page: number; size: number }) =>
  request.get('/admin/banner/page', { params })

export const saveBanner = (data: BannerSaveDTO) => request.post('/admin/banner', data)

export const updateBanner = (id: number, data: BannerSaveDTO) => request.put(`/admin/banner/${id}`, data)

export const deleteBanner = (id: number) => request.delete(`/admin/banner/${id}`)
