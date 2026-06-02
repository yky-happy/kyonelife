<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getCollectionArticles, getCollectionList } from '../../api/collection'
import type { ArticleCard, CollectionItem } from '../../api/types'
import { formatDate } from '../../utils/format'

const route = useRoute()
const collectionId = computed(() => Number(route.params.id))
const collections = ref<CollectionItem[]>([])
const articles = ref<ArticleCard[]>([])
const loading = ref(false)
const error = ref('')
const currentCollection = computed(() => collections.value.find((item) => item.id === collectionId.value))

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [collectionList, articlePage] = await Promise.all([
      getCollectionList(),
      getCollectionArticles(collectionId.value, { page: 1, size: 30 }),
    ])
    collections.value = collectionList
    articles.value = articlePage.records
  } catch (err) {
    error.value = err instanceof Error ? err.message : '合集加载失败'
  } finally {
    loading.value = false
  }
}

watch(collectionId, loadData)
onMounted(loadData)
</script>

<template>
  <section class="plain-page">
    <RouterLink to="/collections" class="text-link">返回合集</RouterLink>
    <div class="collection-detail-head">
      <div>
        <p class="eyebrow">collection</p>
        <h1>{{ currentCollection?.name || '合集' }}</h1>
        <p>{{ currentCollection?.description || '这个合集仍在整理中。' }}</p>
      </div>
      <div class="mini-planet" aria-hidden="true"></div>
    </div>

    <div v-if="loading" class="state-card">正在翻阅合集...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <div v-else class="list-stack">
      <RouterLink v-for="article in articles" :key="article.id" :to="`/article/${article.id}`" class="list-card">
        <span>{{ formatDate(article.createTime) }}</span>
        <strong>{{ article.title }}</strong>
        <p>{{ article.summary }}</p>
      </RouterLink>
      <div v-if="!articles.length" class="state-card">这个合集下还没有已发布文章。</div>
    </div>
  </section>
</template>
