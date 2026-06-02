function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function escapeAttribute(value: string) {
  return value.replaceAll('"', '&quot;').replaceAll("'", '&#039;')
}

function isSafeUrl(value: string) {
  return /^(https?:\/\/|mailto:|\/|#)/i.test(value)
}

type InlineState = {
  strong: boolean
}

function renderStrongMarkers(value: string, state: InlineState) {
  return value.split('**').reduce((result, part, index) => {
    if (index === 0) return part
    state.strong = !state.strong
    return `${result}${state.strong ? '<strong>' : '</strong>'}${part}`
  }, '')
}

function renderInline(value: string, state: InlineState) {
  let html = escapeHtml(value)

  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  html = html.replace(
    /!\[([^\]]*)\]\(([^)\s]+)\)/g,
    (_, alt: string, url: string) =>
      isSafeUrl(url) ? `<img src="${escapeAttribute(url)}" alt="${alt}" />` : escapeHtml(_),
  )
  html = html.replace(
    /\[([^\]]+)\]\(([^)\s]+)\)/g,
    (_, text: string, url: string) =>
      isSafeUrl(url)
        ? `<a href="${escapeAttribute(url)}" target="_blank" rel="noreferrer">${text}</a>`
        : text,
  )
  html = html.replace(
    /(^|[\s(>])((https?:\/\/)[^\s<]+)/g,
    (_, prefix: string, url: string) =>
      `${prefix}<a href="${escapeAttribute(url)}" target="_blank" rel="noreferrer">${url}</a>`,
  )
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')
  html = renderStrongMarkers(html, state)
  html = html.replace(/__(.+?)__/g, '<strong>$1</strong>')
  html = html.replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
  html = html.replace(/(^|[^_])_([^_\n]+)_/g, '$1<em>$2</em>')
  html = html.replace(/&lt;(\/?u)&gt;/gi, '<$1>')
  html = html.replace(/&lt;br\s*\/?&gt;/gi, '<br>')

  return html
}

export function renderMarkdown(source?: string) {
  if (!source) return ''
  const lines = source.split('\n')
  const html: string[] = []
  let inCode = false
  let inList = false
  let inOrderedList = false
  let inBlockquote = false
  const inlineState: InlineState = { strong: false }

  const closeBlockquote = () => {
    if (inBlockquote) {
      html.push('</blockquote>')
      inBlockquote = false
    }
  }

  const closeList = () => {
    if (inList) {
      html.push('</ul>')
      inList = false
    }
    if (inOrderedList) {
      html.push('</ol>')
      inOrderedList = false
    }
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()
    if (line.startsWith('```')) {
      closeList()
      closeBlockquote()
      html.push(inCode ? '</code></pre>' : '<pre><code>')
      inCode = !inCode
      continue
    }
    if (inCode) {
      html.push(`${escapeHtml(line)}\n`)
      continue
    }
    if (!line.trim()) {
      closeList()
      closeBlockquote()
      continue
    }
    if (/^>\s?/.test(line)) {
      closeList()
      if (!inBlockquote) {
        html.push('<blockquote>')
        inBlockquote = true
      }
      html.push(`<p>${renderInline(line.replace(/^>\s?/, ''), inlineState)}</p>`)
      continue
    }
    if (line.startsWith('### ')) {
      closeList()
      closeBlockquote()
      html.push(`<h3>${renderInline(line.slice(4), inlineState)}</h3>`)
      continue
    }
    if (line.startsWith('## ')) {
      closeList()
      closeBlockquote()
      html.push(`<h2>${renderInline(line.slice(3), inlineState)}</h2>`)
      continue
    }
    if (line.startsWith('# ')) {
      closeList()
      closeBlockquote()
      html.push(`<h1>${renderInline(line.slice(2), inlineState)}</h1>`)
      continue
    }
    if (line.startsWith('- ')) {
      closeBlockquote()
      if (inOrderedList) {
        html.push('</ol>')
        inOrderedList = false
      }
      if (!inList) {
        html.push('<ul>')
        inList = true
      }
      html.push(`<li>${renderInline(line.slice(2), inlineState)}</li>`)
      continue
    }
    if (/^\d+\.\s+/.test(line)) {
      closeBlockquote()
      if (inList) {
        html.push('</ul>')
        inList = false
      }
      if (!inOrderedList) {
        html.push('<ol>')
        inOrderedList = true
      }
      html.push(`<li>${renderInline(line.replace(/^\d+\.\s+/, ''), inlineState)}</li>`)
      continue
    }
    closeList()
    closeBlockquote()
    html.push(`<p>${renderInline(line, inlineState)}</p>`)
  }

  closeList()
  closeBlockquote()
  if (inCode) html.push('</code></pre>')
  if (inlineState.strong) html.push('</strong>')
  return html.join('')
}
