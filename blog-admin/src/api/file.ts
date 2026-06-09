import request from '@/utils/request'

export interface FileUploadVO {
  url: string
  objectName: string
  originalFilename: string
  size: number
}

export function uploadImage(file: File, dir = 'article') {
  return uploadFile('/admin/file/upload', file, dir)
}

export function uploadVideo(file: File, dir = 'video') {
  return uploadFile('/admin/file/upload/video', file, dir)
}

function uploadFile(url: string, file: File, dir: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('dir', dir)
  return request.post<unknown, { data: FileUploadVO }>(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}
