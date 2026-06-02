<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getArticlePage } from '../../api/article'
import { getTagList } from '../../api/tag'
import type { ArticleCard, TagItem } from '../../api/types'
import { formatDate } from '../../utils/format'

const route = useRoute()
const tagId = computed(() => Number(route.params.id))
const articles = ref<ArticleCard[]>([])
const tags = ref<TagItem[]>([])
const loading = ref(false)
const error = ref('')
const currentTag = computed(() => tags.value.find((tag) => tag.id === tagId.value))

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [tagList, articlePage] = await Promise.all([
      getTagList(),
      getArticlePage({ page: 1, size: 20, tagId: tagId.value }),
    ])
    tags.value = tagList
    articles.value = articlePage.records
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
  <section class="plain-page">
    <RouterLink to="/tags" class="text-link">返回标签</RouterLink>
    <div class="section-heading">
      <div>
        <p class="eyebrow">tag articles</p>
        <h1>{{ currentTag?.name || '标签文章' }}</h1>
      </div>
    </div>

    <div v-if="loading" class="state-card">正在寻找文章...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <div v-else class="list-stack">
      <RouterLink v-for="article in articles" :key="article.id" :to="`/article/${article.id}`" class="list-card">
        <span>{{ formatDate(article.createTime) }}</span>
        <strong>{{ article.title }}</strong>
        <p>{{ article.summary }}</p>
      </RouterLink>
      <div v-if="!articles.length" class="state-card">这个标签下还没有已发布文章。</div>
    </div>
  </section>
</template>
