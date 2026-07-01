<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getTagList } from '../../api/tag'
import type { TagItem } from '../../api/types'
import { trackEvent } from '../../utils/tracker'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import SideBar from '../../components/SideBar.vue'

const tags = ref<TagItem[]>([])
const loading = ref(false)
const error = ref('')

async function loadTags() {
  loading.value = true
  error.value = ''
  try {
    tags.value = await getTagList()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '标签加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setPageHeader({ type: 'page', title: '标签', subtitle: '用关键词找到同类的文章', songTitle: true })
  setMeta({ title: '标签' })
  loadTags()
})
</script>

<template>
  <div class="ky-layout no-aside-mobile">
    <main class="ky-main">
      <div class="ky-card ky-page">
        <div class="ky-page-head">
          <p class="eyebrow">tags</p>
        </div>

        <div v-if="loading" class="ky-state">正在整理标签…</div>
        <div v-else-if="error" class="ky-state">{{ error }}</div>
        <div v-else-if="!tags.length" class="ky-state">还没有标签。</div>
        <div v-else class="tag-cloud">
          <RouterLink
            v-for="tag in tags"
            :key="tag.id"
            :to="`/tag/${tag.id}`"
            :style="{ color: tag.color || 'var(--ky-theme)' }"
            @click="trackEvent('tag_click', { tagId: tag.id, pageUrl: `/tag/${tag.id}` })"
          >
            {{ tag.name }}
          </RouterLink>
        </div>
      </div>
    </main>

    <SideBar />
  </div>
</template>
