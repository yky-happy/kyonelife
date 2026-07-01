import request from '../utils/request'

export interface UploadResult {
  url: string
  objectName: string
  originalFilename: string
  size: number
}

/** 读者上传图片（评论用，需登录） */
export const uploadImage = (file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post<unknown, UploadResult>('/api/upload/image', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
