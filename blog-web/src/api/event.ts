import request from '../utils/request'

export type EventType = 'page_view' | 'article_view' | 'tag_click' | 'collection_click' | 'search'

export interface EventReport {
  eventType: EventType
  visitorId: string
  articleId?: number
  tagId?: number
  collectionId?: number
  keyword?: string
  pageUrl?: string
  referrer?: string
  duration?: number
}

export const reportEvent = (data: EventReport) =>
  request.post('/api/event/report', data)
