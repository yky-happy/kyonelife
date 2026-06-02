<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getArticlePage } from '../../api/article'
import type { ArticleCard } from '../../api/types'
import { formatDate, readingLabel } from '../../utils/format'
import heroArt from '../../assets/prince-planet-hero.jpg'

const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 8
const total = ref(0)
const articles = ref<ArticleCard[]>([])
const currentSlide = ref(0)
let carouselTimer: number | undefined

const slides = [
  {
    eyebrow: 'planet journal',
    title: '把日常里的微光，整理成可以再次出发的文章。',
    text: '这里记录技术、生活和思考。愿每篇文章都像一次星际停靠，带着好奇，也带着一点柔软。',
  },
  {
    eyebrow: 'rose keeper',
    title: '认真照看一朵玫瑰，也认真照看自己的问题。',
    text: '把复杂的技术拆开，把细小的感受留下，让答案慢慢长出清晰的形状。',
  },
  {
    eyebrow: 'fox road',
    title: '和世界建立连接，再把连接写成路径。',
    text: '读书、写代码、观察生活，所有绕远路的时刻，都可能成为下一篇文章的开头。',
  },
]

const featuredArticles = computed(() => articles.value.slice(0, 3))
const hasMore = computed(() => page.value * pageSize < total.value)

async function loadArticles(reset = false) {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  loading.value = true
  error.value = ''
  try {
    const result = await getArticlePage({ page: page.value, size: pageSize })
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

onMounted(() => loadArticles(true))
onMounted(() => {
  carouselTimer = window.setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % slides.length
  }, 5200)
})
onUnmounted(() => {
  if (carouselTimer) window.clearInterval(carouselTimer)
})
</script>

<template>
  <section class="hero-carousel">
    <div class="hero-copy">
      <p class="eyebrow">{{ slides[currentSlide].eyebrow }}</p>
      <h1>{{ slides[currentSlide].title }}</h1>
      <p class="hero-text">
        {{ slides[currentSlide].text }}
      </p>
      <div class="hero-actions">
        <a href="#articles" class="primary-link">阅读文章</a>
        <RouterLink to="/archives" class="secondary-link">查看归档</RouterLink>
      </div>
      <div class="carousel-dots" aria-label="轮播切换">
        <button
          v-for="(_, index) in slides"
          :key="index"
          :class="{ active: index === currentSlide }"
          :aria-label="`切换到第 ${index + 1} 张`"
          @click="currentSlide = index"
        ></button>
      </div>
    </div>

    <div class="hero-visual" aria-hidden="true">
      <img :src="heroArt" alt="" />
      <div class="hero-note">
        <span>today's orbit</span>
        <strong>写一点清醒，也写一点温柔。</strong>
      </div>
    </div>
  </section>

  <section class="featured-strip" v-if="featuredArticles.length">
    <RouterLink
      v-for="article in featuredArticles"
      :key="article.id"
      :to="`/article/${article.id}`"
      class="featured-card"
    >
      <span>{{ article.collectionName || '星球札记' }}</span>
      <strong>{{ article.title }}</strong>
    </RouterLink>
  </section>

  <section id="articles" class="content-section">
    <div class="section-heading">
      <div>
        <p class="eyebrow">latest notes</p>
        <h2>文章列表</h2>
      </div>
      <RouterLink to="/tags" class="text-link">按标签浏览</RouterLink>
    </div>

    <div v-if="error" class="state-card">{{ error }}</div>
    <div v-else-if="!loading && !articles.length" class="state-card">还没有已发布文章。</div>

    <div class="article-grid">
      <RouterLink
        v-for="article in articles"
        :key="article.id"
        :to="`/article/${article.id}`"
        class="article-card"
      >
        <div v-if="article.cover" class="cover" :style="{ backgroundImage: `url(${article.cover})` }">
          <span v-if="article.isStick" class="pin">置顶</span>
        </div>
        <div class="article-body">
          <div class="meta-row">
            <span>{{ formatDate(article.createTime) }}</span>
            <span>{{ readingLabel(article.viewCount) }} 阅读</span>
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ article.summary || '这篇文章还没有摘要，先去看看正文。' }}</p>
          <div class="tag-row">
            <span v-if="article.collectionName" class="collection-chip">{{ article.collectionName }}</span>
            <span
              v-for="tag in article.tags"
              :key="tag.id"
              class="tag-chip"
              :style="{ '--tag-color': tag.color || '#6bbf8a' }"
            >
              {{ tag.name }}
            </span>
          </div>
        </div>
      </RouterLink>
    </div>

    <div class="load-row" v-if="hasMore || loading">
      <button class="soft-button" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '继续探索' }}
      </button>
    </div>
  </section>
</template>
