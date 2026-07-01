<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import hljs from 'highlight.js/lib/common'
import 'highlight.js/styles/atom-one-dark.css'
import { getArticleDetail, getLikeStatus, getRelatedArticles, toggleLike } from '../../api/article'
import type { ArticleCard, ArticleDetail } from '../../api/types'
import { formatDate, readingLabel } from '../../utils/format'
import { renderMarkdown } from '../../utils/markdown'
import { trackEvent } from '../../utils/tracker'
import { getVisitorId } from '../../utils/visitor'
import { setPageHeader, PAGE_BANNER } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import CommentSection from '../../components/CommentSection.vue'

interface TocItem {
  id: string
  text: string
  level: number
}

const route = useRoute()
const article = ref<ArticleDetail | null>(null)
const loading = ref(false)
const error = ref('')
const enterTime = ref(Date.now())
const durationReported = ref(false)
const articleId = computed(() => Number(route.params.id))
const contentHtml = computed(() => renderMarkdown(article.value?.contentMd || article.value?.content || ''))

const liked = ref(false)
const likeCount = ref(0)
const likePending = ref(false)

const related = ref<ArticleCard[]>([])
const toc = ref<TocItem[]>([])
const tocOpen = ref(true)
const activeHeading = ref('')
const showFloatingToc = ref(false)
const lightboxSrc = ref('')
const contentRef = ref<HTMLElement | null>(null)

function syncBanner() {
  const a = article.value
  if (!a) return
  setPageHeader({
    type: 'post',
    title: a.title,
    bg: a.cover || PAGE_BANNER,
    postDate: formatDate(a.createTime),
    postUpdate: formatDate(a.createTime),
    postCategory: a.collectionName || '',
    postCategoryId: a.collectionId,
    postViews: a.viewCount ?? 0,
  })
  // SEO：标题 / 摘要 / 封面 / 关键词 / 发布时间
  setMeta({
    title: a.title,
    description: a.summary || a.title,
    image: a.cover || undefined,
    type: 'article',
    keywords: a.keywords || undefined,
    publishedTime: a.createTime,
    modifiedTime: a.createTime,
  })
}

// 内容渲染后：代码高亮 + 生成目录
async function enhanceContent() {
  await nextTick()
  const root = contentRef.value
  if (!root) return
  root.querySelectorAll<HTMLElement>('pre code').forEach((el) => {
    el.removeAttribute('data-highlighted')
    // 无效/未注册的语言（如占位 ```language）会让 highlightElement 抛错并中断整页高亮，
    // 这里检测后去掉无效的 language-* 类，改走自动检测
    const langClass = Array.from(el.classList).find((c) => c.startsWith('language-'))
    const lang = langClass ? langClass.slice('language-'.length) : ''
    if (lang && !hljs.getLanguage(lang)) {
      el.classList.remove(langClass!)
    }
    try {
      hljs.highlightElement(el)
    } catch {
      // 单个代码块高亮失败不影响其他
    }
  })
  const heads = Array.from(root.querySelectorAll<HTMLElement>('h1, h2, h3'))
  toc.value = heads.map((h, i) => {
    if (!h.id) h.id = `heading-${i}`
    return { id: h.id, text: h.textContent || '', level: Number(h.tagName[1]) }
  })
  activeHeading.value = toc.value[0]?.id || ''
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    article.value = await getArticleDetail(articleId.value)
    syncBanner()
    trackEvent('article_view', { articleId: articleId.value })
    loadLikeStatus()
    loadRelated()
    enhanceContent()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  } finally {
    loading.value = false
  }
}

async function loadRelated() {
  try {
    related.value = await getRelatedArticles(articleId.value, 6)
  } catch {
    related.value = []
  }
}

async function loadLikeStatus() {
  try {
    const status = await getLikeStatus(articleId.value, getVisitorId())
    liked.value = status.liked
    likeCount.value = status.likeCount
  } catch {
    // 点赞状态加载失败不影响阅读
  }
}

async function onToggleLike() {
  if (likePending.value) return
  likePending.value = true
  try {
    const status = await toggleLike(articleId.value, getVisitorId())
    liked.value = status.liked
    likeCount.value = status.likeCount
  } catch {
    // 忽略点赞失败
  } finally {
    likePending.value = false
  }
}

function reportReadDuration() {
  if (!article.value || durationReported.value) return
  const duration = Date.now() - enterTime.value
  if (duration < 1000) return
  durationReported.value = true
  trackEvent('read_duration', {
    articleId: articleId.value,
    duration,
    pageUrl: `/article/${articleId.value}`,
  })
}

function handleVisibilityChange() {
  if (document.visibilityState === 'hidden') reportReadDuration()
}

// 目录点击：滚动到对应标题（标题已设 scroll-margin-top 避开固定导航）
function gotoHeading(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  activeHeading.value = id
}

// 目录高亮：找到当前视口顶部之上的最后一个标题
function onScrollSpy() {
  // 正文滚动到顶部导航附近时才显示浮动目录，避免叠在大图 banner 上
  const c = contentRef.value
  showFloatingToc.value = !!c && c.getBoundingClientRect().top < 120
  if (!toc.value.length) return
  let current = toc.value[0].id
  for (const item of toc.value) {
    const el = document.getElementById(item.id)
    if (el && el.getBoundingClientRect().top <= 90) current = item.id
    else break
  }
  activeHeading.value = current
}

// 正文图片点击放大
function onContentClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.tagName === 'IMG') {
    lightboxSrc.value = (target as HTMLImageElement).src
  }
}

onMounted(() => {
  enterTime.value = Date.now()
  loadDetail()
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('scroll', onScrollSpy, { passive: true })
})
onBeforeUnmount(() => {
  reportReadDuration()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('scroll', onScrollSpy)
})

// 上一篇/下一篇是同一组件、仅路由参数变化，需手动重置并重新加载
watch(articleId, () => {
  reportReadDuration()
  enterTime.value = Date.now()
  durationReported.value = false
  liked.value = false
  likeCount.value = 0
  related.value = []
  toc.value = []
  window.scrollTo({ top: 0 })
  loadDetail()
})
</script>

<template>
  <div class="article-page-root">
  <div class="ky-layout">
    <main class="ky-main">
      <div v-if="loading" class="ky-state">正在加载文章…</div>
      <div v-else-if="error" class="ky-state">{{ error }}</div>

      <template v-else-if="article">
        <!-- 目录（窄屏：文章顶部折叠卡） -->
        <div v-if="toc.length" class="post-toc-card">
          <div class="toc-card-head" @click="tocOpen = !tocOpen">
            <span><i class="fa-solid fa-list-ul"></i> 目录</span>
            <i :class="tocOpen ? 'fa-solid fa-chevron-up' : 'fa-solid fa-chevron-down'"></i>
          </div>
          <ul v-show="tocOpen">
            <li
              v-for="item in toc"
              :key="item.id"
              :class="[`level-${item.level}`, { active: item.id === activeHeading }]"
              @click="gotoHeading(item.id)"
            >
              {{ item.text }}
            </li>
          </ul>
        </div>

        <article class="post-card">
          <section class="post-content" ref="contentRef" v-html="contentHtml" @click="onContentClick"></section>

          <section class="like-bar">
            <button
              type="button"
              class="like-btn"
              :class="{ liked }"
              :disabled="likePending"
              @click="onToggleLike"
            >
              <span class="like-icon">{{ liked ? '♥' : '♡' }}</span>
              <span>{{ liked ? '已赞' : '点赞' }}</span>
              <span class="like-count">{{ likeCount }}</span>
            </button>
          </section>

          <section v-if="article.isOriginal === 0 && article.originalUrl" class="source-box">
            <span class="label">转载来源</span>
            <a :href="article.originalUrl" target="_blank" rel="noreferrer">{{ article.originalUrl }}</a>
          </section>

          <div class="post-tags" v-if="article.tags?.length" style="margin-top: 22px">
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="`/tag/${tag.id}`"
              class="chip"
              :style="{ '--chip-color': tag.color || 'var(--ky-theme)' }"
              @click="trackEvent('tag_click', { tagId: tag.id, pageUrl: `/tag/${tag.id}` })"
            >
              <i class="fa-solid fa-tag"></i>{{ tag.name }}
            </RouterLink>
          </div>
        </article>

        <!-- 相关推荐 -->
        <section v-if="related.length" class="related-section">
          <h2 class="section-title"><i class="fa-solid fa-thumbs-up"></i>相关推荐</h2>
          <div class="sticky-grid">
            <RouterLink
              v-for="a in related"
              :key="a.id"
              :to="`/article/${a.id}`"
              class="sticky-card"
            >
              <div class="sc-cover" :style="a.cover ? { backgroundImage: `url(${a.cover})` } : {}"></div>
              <div class="sc-body">
                <div class="sc-title">{{ a.title }}</div>
                <div class="post-meta">
                  <span><i class="fa-regular fa-calendar"></i>{{ formatDate(a.createTime) }}</span>
                  <span><i class="fa-regular fa-eye"></i>{{ readingLabel(a.viewCount) }}</span>
                </div>
              </div>
            </RouterLink>
          </div>
        </section>

        <CommentSection :article-id="articleId" />

        <nav v-if="article.prevArticle || article.nextArticle" class="post-nav">
          <RouterLink
            v-if="article.prevArticle"
            :to="`/article/${article.prevArticle.id}`"
            class="prev"
          >
            <span class="nav-label"><i class="fa-solid fa-angle-left"></i> 上一篇</span>
            <span class="nav-title">{{ article.prevArticle.title }}</span>
          </RouterLink>
          <span v-else class="placeholder"></span>

          <RouterLink
            v-if="article.nextArticle"
            :to="`/article/${article.nextArticle.id}`"
            class="next"
          >
            <span class="nav-label">下一篇 <i class="fa-solid fa-angle-right"></i></span>
            <span class="nav-title">{{ article.nextArticle.title }}</span>
          </RouterLink>
          <span v-else class="placeholder"></span>
        </nav>
      </template>
    </main>
  </div>

  <!-- 文章目录（浮动，宽屏显示；滚动进正文后才出现，与正文平齐） -->
  <nav v-if="toc.length" class="post-toc" :class="{ visible: showFloatingToc }">
    <div class="toc-title"><i class="fa-solid fa-list-ul"></i>目录</div>
    <ul>
      <li
        v-for="item in toc"
        :key="item.id"
        :class="[`level-${item.level}`, { active: item.id === activeHeading }]"
        @click="gotoHeading(item.id)"
      >
        {{ item.text }}
      </li>
    </ul>
  </nav>

  <!-- 图片灯箱 -->
  <div v-if="lightboxSrc" class="lightbox" @click="lightboxSrc = ''">
    <img :src="lightboxSrc" alt="" />
  </div>
  </div>
</template>
