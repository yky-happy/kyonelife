import request from '@/utils/request'

export interface OperationLog {
  id: number
  adminId: number | null
  adminName: string
  module: string
  operation: string
  requestMethod: string
  requestPath: string
  requestParams: string
  responseCode: number
  responseMessage: string
  ip: string
  userAgent: string
  costTime: number
  success: number
  errorMessage: string
  createTime: string
}

export interface OperationLogPageParams {
  page: number
  size: number
  module?: string
  operation?: string
  success?: number
  startTime?: string
  endTime?: string
}

export const getOperationLogPage = (params: OperationLogPageParams) =>
  request.get('/admin/operation-log/page', { params })
