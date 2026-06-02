<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getArticleDetail } from '../../api/article'
import type { ArticleDetail } from '../../api/types'
import { formatDate, readingLabel } from '../../utils/format'
import { renderMarkdown } from '../../utils/markdown'

const route = useRoute()
const article = ref<ArticleDetail | null>(null)
const loading = ref(false)
const error = ref('')
const articleId = computed(() => Number(route.params.id))
const contentHtml = computed(() => renderMarkdown(article.value?.contentMd || article.value?.content || ''))

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    article.value = await getArticleDetail(articleId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <article class="detail-page">
    <div v-if="loading" class="state-card">正在穿过星群...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <template v-else-if="article">
      <div class="article-paper" :class="{ 'without-cover': !article.cover }">
        <div v-if="article.cover" class="detail-cover" :style="{ backgroundImage: `url(${article.cover})` }"></div>

        <header class="article-hero">
          <RouterLink to="/" class="text-link">返回首页</RouterLink>
          <h1>{{ article.title }}</h1>
          <p v-if="article.summary" class="detail-summary">{{ article.summary }}</p>

          <div class="detail-meta">
            <span v-if="article.collectionName" class="meta-group">
              合集：
              <RouterLink
                v-if="article.collectionId"
                :to="`/collection/${article.collectionId}`"
                class="meta-link"
              >
                {{ article.collectionName }}
              </RouterLink>
              <span v-else>{{ article.collectionName }}</span>
            </span>
            <span v-if="article.tags?.length" class="meta-divider">|</span>
            <RouterLink
              v-for="tag in article.tags"
              :key="tag.id"
              :to="`/tag/${tag.id}`"
              class="tag-chip"
              :style="{ '--tag-color': tag.color || '#6bbf8a' }"
            >
              {{ tag.name }}
            </RouterLink>
            <span class="meta-divider">|</span>
            <span>{{ formatDate(article.createTime) }}</span>
            <span>{{ readingLabel(article.viewCount) }} 阅读</span>
          </div>
        </header>

        <section class="markdown-body" v-html="contentHtml"></section>

        <section v-if="article.isOriginal === 0 && article.originalUrl" class="source-card">
          <span>转载来源</span>
          <a :href="article.originalUrl" target="_blank" rel="noreferrer">{{ article.originalUrl }}</a>
        </section>
      </div>
    </template>
  </article>
</template>
