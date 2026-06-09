import request from '@/utils/request'

export interface RuntimeLogLine {
  time: string
  level: string
  thread: string
  logger: string
  message: string
  raw: string
  method: string
  path: string
  status: number
  costTime: number
  ip: string
}

export interface RuntimeLogSummary {
  todayRequestCount: number
  warnCount: number
  errorCount: number
  slowRequestCount: number
  lastStartTime: string | null
  lastShutdownTime: string | null
  logFilePath: string
}

export interface RuntimeLogSlowRequest {
  time: string
  method: string
  path: string
  status: number
  costTime: number
  ip: string
}

export interface RuntimeLogApiMetric {
  method: string
  path: string
  requestCount: number
  averageCostTime: number
  maxCostTime: number
}

export const getRuntimeLogRecent = (params: { lines?: number; level?: string }) =>
  request.get('/admin/log/recent', { params })

export const searchRuntimeLog = (params: { keyword?: string; level?: string; lines?: number }) =>
  request.get('/admin/log/search', { params })

export const getRuntimeLogSummary = (params?: { slowThreshold?: number }) =>
  request.get('/admin/log/summary', { params })

export const getRuntimeLogSlowRequests = (params?: { threshold?: number; limit?: number }) =>
  request.get('/admin/log/slow-requests', { params })

export const getRuntimeLogTopApis = (params?: { limit?: number }) =>
  request.get('/admin/log/top-apis', { params })
