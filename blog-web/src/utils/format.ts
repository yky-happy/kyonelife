export function formatDate(value?: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

export function readingLabel(count?: number) {
  const value = count ?? 0
  return value > 999 ? `${(value / 1000).toFixed(1)}k` : String(value)
}
