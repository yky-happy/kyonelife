<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getArticlePage } from '../../api/article'
import { getBannerList } from '../../api/banner'
import type { ArticleCard } from '../../api/types'
import { formatDate, readingLabel } from '../../utils/format'
import { trackEvent } from '../../utils/tracker'
import { setPageHeader, HOME_BANNER } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import { ensureSiteConfig, bulletin, summary } from '../../composables/siteConfig'
import SideBar from '../../components/SideBar.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const articles = ref<ArticleCard[]>([])
const activeKeyword = ref('')

const HOME_TYPED = [
  '把日常里的微光，整理成可以再次出发的文章。',
  '守护一朵玫瑰，也记录每一次远行。',
  '读书、写代码、观察生活。',
]

const hasMore = computed(() => page.value * pageSize < total.value)

function routeKeyword(): string {
  const k = route.query.keyword
  return (typeof k === 'string' ? k : '').trim()
}

const bannerImages = ref<string[]>([HOME_BANNER])

// 搜索时隐藏首页轮播、直接展示结果；否则展示轮播（文字不变，传同一 HOME_TYPED 引用避免重启打字机）
function refreshHeader() {
  if (activeKeyword.value) {
    setPageHeader({ type: 'home', hidden: true })
  } else {
    setPageHeader({ type: 'home', title: 'Kyonelife', typewriter: HOME_TYPED, bgList: bannerImages.value })
  }
}

async function loadBanners() {
  try {
    const list = await getBannerList()
    const images = list.map((b) => b.imageUrl).filter(Boolean)
    bannerImages.value = images.length ? images : [HOME_BANNER]
  } catch {
    bannerImages.value = [HOME_BANNER]
  }
  refreshHeader()
}

async function loadArticles(reset = false) {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  loading.value = true
  error.value = ''
  try {
    const result = await getArticlePage({ page: page.value, size: pageSize, keyword: activeKeyword.value || undefined })
    total.value = result.total
    articles.value = reset ? result.records : [...articles.value, ...result.records]
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value += 1
  await loadArticles()
}

// 顶部导航的搜索框通过 URL query 驱动；任意页面搜索都会跳到首页并带上 keyword
function applySearchFromRoute() {
  const kw = routeKeyword()
  activeKeyword.value = kw
  if (kw) {
    trackEvent('search', { keyword: kw, pageUrl: '/' })
  }
  refreshHeader()
  loadArticles(true)
}

function clearSearch() {
  router.push({ path: '/' })
}

watch(() => route.query.keyword, applySearchFromRoute)

onMounted(() => {
  ensureSiteConfig().then(() => setMeta({ description: summary.value }))
  loadBanners()
  applySearchFromRoute()
})
</script>

<template>
  <div class="ky-layout" :class="{ 'no-banner': activeKeyword }">
    <main class="ky-main">
      <div v-if="bulletin && !activeKeyword" class="bulletin-bar">
        <i class="fa-solid fa-bullhorn"></i><span>{{ bulletin }}</span>
      </div>

      <div v-if="activeKeyword" class="search-status">
        <span>搜索「{{ activeKeyword }}」的结果（{{ total }} 篇）</span>
        <button type="button" @click="clearSearch"><i class="fa-solid fa-xmark"></i>清除</button>
      </div>

      <div v-if="error" class="ky-state">{{ error }}</div>
      <div v-else-if="!loading && !articles.length" class="ky-state">
        {{ activeKeyword ? '没有找到匹配的文章。' : '还没有已发布文章。' }}
      </div>

      <div class="recent-posts">
        <RouterLink
          v-for="(article, idx) in articles"
          :key="article.id"
          :to="`/article/${article.id}`"
          class="post-item"
          :class="{ right: idx % 2 === 1 }"
        >
          <div class="post-cover">
            <img v-if="article.cover" :src="article.cover" :alt="article.title" />
          </div>
          <div class="post-info">
            <span class="post-title">
              <span v-if="article.isStick" class="pin" title="置顶">📌</span>
              {{ article.title }}
            </span>
            <div class="post-meta">
              <span><i class="fa-regular fa-calendar"></i>{{ formatDate(article.createTime) }}</span>
              <span v-if="article.collectionName"><i class="fa-solid fa-inbox"></i>{{ article.collectionName }}</span>
              <span><i class="fa-regular fa-eye"></i>{{ readingLabel(article.viewCount) }} 阅读</span>
            </div>
            <p class="post-excerpt">{{ article.summary || '这篇文章还没有摘要，先去看看正文。' }}</p>
            <div class="post-tags" v-if="article.tags?.length">
              <span
                v-for="tag in article.tags"
                :key="tag.id"
                class="chip"
                :style="{ '--chip-color': tag.color || 'var(--ky-theme)' }"
              >
                {{ tag.name }}
              </span>
            </div>
          </div>
        </RouterLink>
      </div>

      <div class="load-more" v-if="hasMore || loading">
        <button class="ky-btn" :disabled="loading" @click="loadMore">
          {{ loading ? '加载中…' : '继续阅读' }}
        </button>
      </div>
    </main>

    <SideBar />
  </div>
</template>
