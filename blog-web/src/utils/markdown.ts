import { marked } from 'marked'
import DOMPurify from 'dompurify'

// GFM：支持表格、任务列表、嵌套列表、删除线等；breaks：单换行转 <br>
marked.setOptions({ gfm: true, breaks: true })

// 图片懒加载 + 外链安全
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  const el = node as Element
  if (el.tagName === 'IMG') {
    el.setAttribute('loading', 'lazy')
  }
  if (el.tagName === 'A') {
    el.setAttribute('target', '_blank')
    el.setAttribute('rel', 'noopener noreferrer')
  }
})

/** 渲染 Markdown 为安全 HTML（marked 解析 + DOMPurify 过滤，防 XSS） */
export function renderMarkdown(source?: string): string {
  if (!source) return ''
  const raw = marked.parse(source, { async: false }) as string
  return DOMPurify.sanitize(raw, { ADD_ATTR: ['target'] })
}
