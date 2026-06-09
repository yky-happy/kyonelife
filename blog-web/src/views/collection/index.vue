<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getCollectionList } from '../../api/collection'
import type { CollectionItem } from '../../api/types'
import { trackEvent } from '../../utils/tracker'

const collections = ref<CollectionItem[]>([])
const loading = ref(false)
const error = ref('')

async function loadCollections() {
  loading.value = true
  error.value = ''
  try {
    collections.value = await getCollectionList()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '合集加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadCollections)
</script>

<template>
  <section class="plain-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">collections</p>
        <h1>星球合集</h1>
      </div>
    </div>

    <div v-if="loading" class="state-card">正在靠近星球...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <div v-else class="collection-grid">
      <RouterLink
        v-for="item in collections"
        :key="item.id"
        :to="`/collection/${item.id}`"
        class="collection-card"
        @click="trackEvent('collection_click', { collectionId: item.id, pageUrl: `/collection/${item.id}` })"
      >
        <div v-if="item.cover" class="collection-cover" :style="{ backgroundImage: `url(${item.cover})` }"></div>
        <strong>{{ item.name }}</strong>
        <p>{{ item.description || '一个仍在生长的主题星球。' }}</p>
        <span>{{ item.articleCount }} 篇文章</span>
      </RouterLink>
    </div>
  </section>
</template>
