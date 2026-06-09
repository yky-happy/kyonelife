const VISITOR_ID_KEY = 'kyonelife_visitor_id'

export function getVisitorId() {
  const cached = localStorage.getItem(VISITOR_ID_KEY)
  if (cached) {
    return cached
  }

  const randomPart = crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(16).slice(2)}`
  const visitorId = `v_${randomPart}`
  localStorage.setItem(VISITOR_ID_KEY, visitorId)
  return visitorId
}
