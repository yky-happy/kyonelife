<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getArticlePage } from '../../api/article'
import { getTagList } from '../../api/tag'
import type { ArticleCard, TagItem } from '../../api/types'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import SideBar from '../../components/SideBar.vue'

const route = useRoute()
const tagId = computed(() => Number(route.params.id))
const articles = ref<ArticleCard[]>([])
const tags = ref<TagItem[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = 10
const total = ref(0)
const currentTag = computed(() => tags.value.find((tag) => tag.id === tagId.value))
const hasMore = computed(() => page.value * pageSize < total.value)

async function loadArticles(reset = false) {
  if (reset) {
    page.value = 1
    articles.value = []
  }
  const res = await getArticlePage({ page: page.value, size: pageSize, tagId: tagId.value })
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
    tags.value = await getTagList()
    await loadArticles(true)
    const name = currentTag.value?.name
    setPageHeader({
      type: 'page',
      title: name ? `# ${name}` : '标签文章',
      subtitle: `共 ${total.value} 篇相关文章`,
    })
    setMeta({ title: name ? `标签：${name}` : '标签文章' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文章加载失败'
  } finally {
    loading.value = false
  }
}

watch(tagId, loadData)
onMounted(loadData)
</script>

<template>
  <div class="ky-layout no-aside-mobile">
    <main class="ky-main">
      <div class="ky-card ky-page">
        <RouterLink to="/tags" class="back-link"><i class="fa-solid fa-angle-left"></i>返回标签</RouterLink>
        <div class="ky-page-head">
          <p class="eyebrow">tag</p>
          <h1>{{ currentTag?.name || '标签文章' }}</h1>
        </div>

        <div v-if="loading && !articles.length" class="ky-state">正在寻找文章…</div>
        <div v-else-if="error" class="ky-state">{{ error }}</div>
        <div v-else-if="!articles.length" class="ky-state">这个标签下还没有已发布文章。</div>
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
