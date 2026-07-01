<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getArticleArchive } from '../../api/article'
import type { ArchiveMonth } from '../../api/types'
import { formatDate } from '../../utils/format'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import SideBar from '../../components/SideBar.vue'

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

onMounted(() => {
  setPageHeader({ type: 'page', title: '归档', subtitle: '时间长河里的每一次记录', songTitle: true })
  setMeta({ title: '归档' })
  loadArchive()
})
</script>

<template>
  <div class="ky-layout">
    <main class="ky-main">
      <div class="ky-card ky-page">
        <div class="ky-page-head">
          <p class="eyebrow">archives</p>
        </div>

        <div v-if="loading" class="ky-state">正在整理年份…</div>
        <div v-else-if="error" class="ky-state">{{ error }}</div>
        <div v-else-if="!archives.length" class="ky-state">还没有可归档的文章。</div>
        <div v-else class="archive-list">
          <section v-for="group in archives" :key="group.month" class="archive-month">
            <h2><i class="fa-regular fa-calendar"></i>{{ group.month }}</h2>
            <RouterLink
              v-for="article in group.articles"
              :key="article.id"
              :to="`/article/${article.id}`"
              class="a-item"
            >
              <span class="a-date">{{ formatDate(article.createTime) }}</span>
              <span class="a-title">{{ article.title }}</span>
            </RouterLink>
          </section>
        </div>
      </div>
    </main>

    <SideBar />
  </div>
</template>
