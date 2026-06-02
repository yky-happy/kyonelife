<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getArticleArchive } from '../../api/article'
import type { ArchiveMonth } from '../../api/types'
import { formatDate } from '../../utils/format'

const archives = ref<ArchiveMonth[]>([])
const loading = ref(false)
const error = ref('')

async function loadArchive() {
  loading.value = true
  error.value = ''
  try {
    archives.value = await getArticleArchive()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '归档加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadArchive)
</script>

<template>
  <section class="plain-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">archives</p>
        <h1>旅行归档</h1>
      </div>
    </div>

    <div v-if="loading" class="state-card">正在整理年份和星尘...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <div v-else class="archive-list">
      <section v-for="group in archives" :key="group.month" class="archive-month">
        <h2>{{ group.month }}</h2>
        <RouterLink v-for="article in group.articles" :key="article.id" :to="`/article/${article.id}`">
          <span>{{ formatDate(article.createTime) }}</span>
          <strong>{{ article.title }}</strong>
        </RouterLink>
      </section>
      <div v-if="!archives.length" class="state-card">还没有可归档的文章。</div>
    </div>
  </section>
</template>
