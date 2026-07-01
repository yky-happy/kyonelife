import { flushEvents, type EventReport, type EventType } from '../api/event'
import { getVisitorId } from './visitor'

let lastPageUrl = ''

// 客户端攒批：事件先入缓冲，按数量/时间/页面隐藏批量上报，减少请求数
const buffer: EventReport[] = []
const MAX_BUFFER = 10
const FLUSH_INTERVAL = 5000
let timer: ReturnType<typeof setTimeout> | null = null

/** 立即冲刷缓冲区，批量发送累积的事件。 */
export function flush(): void {
  if (timer !== null) {
    clearTimeout(timer)
    timer = null
  }
  if (buffer.length === 0) {
    return
  }
  const events = buffer.splice(0, buffer.length)
  flushEvents(events)
}

function enqueue(event: EventReport): void {
  buffer.push(event)
  // 终态/重要事件（阅读时长）立即发送，避免页面随即关闭而丢失；缓冲满也立即发
  if (event.eventType === 'read_duration' || buffer.length >= MAX_BUFFER) {
    flush()
    return
  }
  if (timer === null) {
    timer = setTimeout(flush, FLUSH_INTERVAL)
  }
}

export function trackEvent(eventType: EventType, payload: Partial<EventReport> = {}) {
  const pageUrl = payload.pageUrl || window.location.pathname + window.location.search
  const referrer = payload.referrer ?? lastPageUrl ?? document.referrer
  enqueue({
    eventType,
    visitorId: getVisitorId(),
    pageUrl,
    referrer,
    ...payload,
  })
}

export function trackPageView(pageUrl: string) {
  trackEvent('page_view', {
    pageUrl,
    referrer: lastPageUrl || document.referrer,
  })
  lastPageUrl = pageUrl
}

// 页面隐藏/卸载时冲刷缓冲，配合 sendBeacon 保证 read_duration 等事件不漏报
if (typeof window !== 'undefined') {
  window.addEventListener('pagehide', flush)
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      flush()
    }
  })
}
