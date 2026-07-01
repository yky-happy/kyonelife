import { siteName } from '../composables/siteConfig'

function ensureMeta(key: string, attr: 'name' | 'property') {
  const selector = `meta[${attr}="${key}"]`
  let el = document.head.querySelector<HTMLMetaElement>(selector)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  return el
}

export interface MetaOptions {
  title?: string
  description?: string
  image?: string
  url?: string
  type?: string
  keywords?: string
  publishedTime?: string
  modifiedTime?: string
}

function ensureLink(rel: string) {
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`)
  if (!el) {
    el = document.createElement('link')
    el.setAttribute('rel', rel)
    document.head.appendChild(el)
  }
  return el
}

/** 动态设置 document.title 与 description / Open Graph 元标签（客户端 SEO） */
export function setMeta(opts: MetaOptions = {}) {
  const site = siteName.value
  const fullTitle = opts.title ? `${opts.title} · ${site}` : site
  document.title = fullTitle

  const desc = opts.description || ''
  ensureMeta('description', 'name').setAttribute('content', desc)
  ensureMeta('og:site_name', 'property').setAttribute('content', site)
  ensureMeta('og:title', 'property').setAttribute('content', fullTitle)
  ensureMeta('og:description', 'property').setAttribute('content', desc)
  ensureMeta('og:type', 'property').setAttribute('content', opts.type || 'website')
  ensureMeta('og:url', 'property').setAttribute('content', opts.url || window.location.href)
  if (opts.image) {
    ensureMeta('og:image', 'property').setAttribute('content', opts.image)
  }
  ensureMeta('twitter:card', 'name').setAttribute('content', 'summary_large_image')
  ensureMeta('twitter:title', 'name').setAttribute('content', fullTitle)
  ensureMeta('twitter:description', 'name').setAttribute('content', desc)

  if (opts.keywords) {
    ensureMeta('keywords', 'name').setAttribute('content', opts.keywords)
  }
  // canonical 规范链接
  ensureLink('canonical').setAttribute('href', opts.url || window.location.href)
  // 文章发布/更新时间
  if ((opts.type || 'website') === 'article') {
    if (opts.publishedTime) {
      ensureMeta('article:published_time', 'property').setAttribute('content', opts.publishedTime)
    }
    if (opts.modifiedTime) {
      ensureMeta('article:modified_time', 'property').setAttribute('content', opts.modifiedTime)
    }
  }
}
