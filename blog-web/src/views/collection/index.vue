<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getCollectionList } from '../../api/collection'
import type { CollectionItem } from '../../api/types'
import { trackEvent } from '../../utils/tracker'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import SideBar from '../../components/SideBar.vue'

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

onMounted(() => {
  setPageHeader({ type: 'page', title: '合集', subtitle: '把同一主题的文章收进一颗星球', songTitle: true })
  setMeta({ title: '合集' })
  loadCollections()
})
</script>

<template>
  <div class="ky-layout no-aside-mobile">
    <main class="ky-main">
      <div class="ky-card ky-page">
        <div class="ky-page-head">
          <p class="eyebrow">collections</p>
        </div>

        <div v-if="loading" class="ky-state">正在靠近星球…</div>
        <div v-else-if="error" class="ky-state">{{ error }}</div>
        <div v-else-if="!collections.length" class="ky-state">还没有合集。</div>
        <div v-else class="collection-grid">
          <RouterLink
            v-for="item in collections"
            :key="item.id"
            :to="`/collection/${item.id}`"
            class="collection-card"
            @click="trackEvent('collection_click', { collectionId: item.id, pageUrl: `/collection/${item.id}` })"
          >
            <div class="cc-cover" :style="item.cover ? { backgroundImage: `url(${item.cover})` } : {}"></div>
            <div class="cc-body">
              <div class="cc-name">{{ item.name }}</div>
              <p class="cc-desc">{{ item.description || '一个仍在生长的主题星球。' }}</p>
              <div class="cc-count"><i class="fa-regular fa-file-lines"></i> {{ item.articleCount }} 篇文章</div>
            </div>
          </RouterLink>
        </div>
      </div>
    </main>

    <SideBar />
  </div>
</template>
