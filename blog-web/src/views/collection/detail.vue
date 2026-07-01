<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getCollectionArticles, getCollectionList } from '../../api/collection'
import type { ArticleCard, CollectionItem } from '../../api/types'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import SideBar from '../../components/SideBar.vue'

const route = useRoute()
const collectionId = computed(() => Number(route.params.id))
const collections = ref<CollectionItem[]>([])
const articles = ref<ArticleCard[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const currentCollection = computed(() => collections.value.find((item) => item.id === collectionId.value))
const hasMore = computed(() => page.value * pageSize < total.value)

async function loadArticles(reset = false) {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  const res = await getCollectionArticles(collectionId.value, { page: page.value, size: pageSize })
  total.value = res.total
  articles.value = reset ? res.records : [...articles.value, ...res.records]
}

async function loadMore() {
  page.value += 1
  loading.value = true
  try {
    await loadArticles()
  } catch (err) {
    page.value -= 1
    error.value = err instanceof Error ? err.message : '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    collections.value = await getCollectionList()
    await loadArticles(true)
    const name = currentCollection.value?.name || '合集'
    setPageHeader({
      type: 'page',
      title: name,
      subtitle: currentCollection.value?.description || `共 ${total.value} 篇文章`,
    })
    setMeta({ title: `合集：${name}`, description: currentCollection.value?.description || '' })
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
  <div class="ky-layout no-aside-mobile">
    <main class="ky-main">
      <div class="ky-card ky-page">
        <RouterLink to="/collections" class="back-link"><i class="fa-solid fa-angle-left"></i>返回合集</RouterLink>
        <div class="collection-head">
          <div class="mini-planet" aria-hidden="true"></div>
          <div class="ky-page-head" style="margin-bottom: 0">
            <p class="eyebrow">collection</p>
            <h1>{{ currentCollection?.name || '合集' }}</h1>
            <p style="color: var(--ky-text-muted); margin-top: 6px">
              {{ currentCollection?.description || '这个合集仍在整理中。' }}
            </p>
          </div>
        </div>

        <div v-if="loading && !articles.length" class="ky-state">正在翻阅合集…</div>
        <div v-else-if="error" class="ky-state">{{ error }}</div>
        <div v-else-if="!articles.length" class="ky-state">这个合集下还没有已发布文章。</div>
        <div v-else class="title-list">
          <RouterLink v-for="article in articles" :key="article.id" :to="`/article/${article.id}`">
            {{ article.title }}
          </RouterLink>
        </div>

        <div class="load-more" v-if="hasMore">
          <button class="ky-btn" :disabled="loading" @click="loadMore">
            {{ loading ? '加载中…' : '加载更多' }}
          </button>
        </div>
      </div>
    </main>

    <SideBar />
  </div>
</template>
