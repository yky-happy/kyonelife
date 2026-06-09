import { reportEvent, type EventReport, type EventType } from '../api/event'
import { getVisitorId } from './visitor'

let lastPageUrl = ''

export function trackEvent(eventType: EventType, payload: Partial<EventReport> = {}) {
  const pageUrl = payload.pageUrl || window.location.pathname + window.location.search
  const referrer = payload.referrer ?? lastPageUrl ?? document.referrer
  reportEvent({
    eventType,
    visitorId: getVisitorId(),
    pageUrl,
    referrer,
    ...payload,
  }).catch(() => {
    // 埋点失败不影响用户浏览。
  })
}

export function trackPageView(pageUrl: string) {
  trackEvent('page_view', {
    pageUrl,
    referrer: lastPageUrl || document.referrer,
  })
  lastPageUrl = pageUrl
}
