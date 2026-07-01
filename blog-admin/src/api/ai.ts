import request from '@/utils/request'

export interface AiAssistDTO {
  title?: string
  contentMd: string
}

export interface AiSummaryVO {
  summary: string
  degraded: boolean
  fromCache: boolean
}

export interface AiTagsVO {
  tags: string[]
  degraded: boolean
  fromCache: boolean
}

/** AI 生成文章摘要 */
export const aiGenerateSummary = (data: AiAssistDTO) =>
  request.post('/admin/ai/summary', data)

/** AI 推荐标签 */
export const aiRecommendTags = (data: AiAssistDTO) =>
  request.post('/admin/ai/tags', data)
