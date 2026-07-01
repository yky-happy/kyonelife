import request from '../utils/request'

export type EventType = 'page_view' | 'article_view' | 'tag_click' | 'collection_click' | 'search' | 'read_duration'

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

const BATCH_URL = `${(request.defaults.baseURL || '').replace(/\/$/, '')}/api/event/report/batch`

/**
 * 批量上报埋点事件。
 * 优先用 navigator.sendBeacon——它在页面跳转/关闭时仍能可靠送达，且不阻塞导航
 * （这正是普通 fetch 在卸载阶段会被浏览器掐断、导致 read_duration 丢失的问题）。
 * sendBeacon 不可用时回退到 fetch keepalive。
 */
export function flushEvents(events: EventReport[]): void {
  if (!events.length) {
    return
  }
  const body = JSON.stringify(events)

  if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
    try {
      const blob = new Blob([body], { type: 'application/json' })
      if (navigator.sendBeacon(BATCH_URL, blob)) {
        return
      }
    } catch {
      // 落到 fetch 兜底
    }
  }

  fetch(BATCH_URL, {
    method: 'POST',
    body,
    headers: { 'Content-Type': 'application/json' },
    keepalive: true,
  }).catch(() => {
    // 埋点失败不影响用户浏览
  })
}
