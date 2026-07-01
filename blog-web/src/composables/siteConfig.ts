import { computed, reactive } from 'vue'
import { getSiteConfig, type WebConfig } from '../api/config'

// 站点配置：进程内只拉一次，跨组件复用；未配置的字段用兜底默认值
const state = reactive<Partial<WebConfig>>({})

let loaded = false
let inflight: Promise<void> | null = null

export function ensureSiteConfig(): Promise<void> {
  if (loaded) return Promise.resolve()
  if (inflight) return inflight
  inflight = (async () => {
    try {
      const cfg = await getSiteConfig()
      Object.assign(state, cfg)
      loaded = true
    } catch {
      // 配置加载失败时使用兜底默认值
    } finally {
      inflight = null
    }
  })()
  return inflight
}

const DEFAULT_ABOUT =
  '这里是一个关于技术、生活和长期思考的个人空间。把复杂的技术拆开，把细小的感受留下，愿每一篇文章都像一次轻盈的远行。'

// 带兜底的只读访问器（在 <script setup> 中直接用，模板会自动解包）
export const siteName = computed(() => state.siteName || 'Kyonelife')
export const logo = computed(() => state.logo || '')
export const summary = computed(() => state.summary || '记录技术、生活与思考')
export const author = computed(() => state.author || 'Kyonelife')
export const authorAvatar = computed(() => state.authorAvatar || '')
export const signature = computed(() => state.signature || '守护一朵玫瑰，也记录每一次远行')
export const github = computed(() => state.github || '')
export const email = computed(() => state.email || '')
export const aboutMe = computed(() => state.aboutMe || DEFAULT_ABOUT)
export const icpNumber = computed(() => state.icpNumber || '')
export const bulletin = computed(() => state.bulletin || '')
