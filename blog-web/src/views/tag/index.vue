<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getTagList } from '../../api/tag'
import type { TagItem } from '../../api/types'
import { trackEvent } from '../../utils/tracker'

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

onMounted(loadTags)
</script>

<template>
  <section class="plain-page">
    <div class="section-heading">
      <div>
        <p class="eyebrow">tags</p>
        <h1>标签星图</h1>
      </div>
    </div>

    <div v-if="loading" class="state-card">正在整理星图...</div>
    <div v-else-if="error" class="state-card">{{ error }}</div>
    <div v-else class="tag-cloud">
      <RouterLink
        v-for="tag in tags"
        :key="tag.id"
        :to="`/tag/${tag.id}`"
        class="tag-bubble"
        :style="{ '--tag-color': tag.color || '#6bbf8a' }"
        @click="trackEvent('tag_click', { tagId: tag.id, pageUrl: `/tag/${tag.id}` })"
      >
        <strong>{{ tag.name }}</strong>
        <span>{{ tag.articleCount }} 篇文章</span>
      </RouterLink>
    </div>
  </section>
</template>
