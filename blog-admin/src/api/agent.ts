import request from '@/utils/request'

// ===== 选题助手 =====

export interface AgentTopicsDTO {
  days?: number
  count?: number
  direction?: string
  sessionId?: string
}

export interface TopicSuggestion {
  title: string
  reason: string
  refKeywords: string[]
  refArticles: string[]
}

export interface AgentTopicsVO {
  topics: TopicSuggestion[]
  rounds: number
  capped: boolean
  degraded: boolean
}

// ===== 创作助手（生成草稿）=====

export interface AgentDraftDTO {
  topic: string
  points?: string
  style?: string
  sessionId?: string
}

export interface AgentDraftVO {
  draftId: number | null
  title: string
  summary: string | null
  tags: string[]
  rounds: number
  capped: boolean
  degraded: boolean
}

/**
 * Agent 一次会多轮调用模型，比第一层慢得多（创作可能 30s+），
 * 这里单独放宽超时到 200s，避免被全局 10s 超时掐断。
 */
const AGENT_TIMEOUT = 200_000

/** 选题助手：基于埋点数据生成选题列表 */
export const agentSuggestTopics = (data: AgentTopicsDTO) =>
  request.post('/admin/ai/agent/topics', data, { timeout: AGENT_TIMEOUT })

/** 创作助手：按选题/要点生成文章草稿并入库（status=0） */
export const agentGenerateDraft = (data: AgentDraftDTO) =>
  request.post('/admin/ai/agent/draft', data, { timeout: AGENT_TIMEOUT })
