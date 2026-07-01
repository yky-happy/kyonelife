<script setup lang="ts">
import { onMounted } from 'vue'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'
import { ensureSiteConfig, author, authorAvatar, aboutMe, github, email } from '../../composables/siteConfig'

onMounted(() => {
  setPageHeader({ type: 'page', title: '关于', subtitle: '关于这个博客，以及写它的人' })
  setMeta({ title: '关于' })
  ensureSiteConfig()
})
</script>

<template>
  <div class="ky-layout">
    <main class="ky-main">
      <div class="ky-card about-card">
        <img v-if="authorAvatar" class="about-avatar-img" :src="authorAvatar" :alt="author" />
        <div v-else class="about-avatar">{{ author.slice(0, 1).toUpperCase() }}</div>
        <h1>{{ author }}</h1>
        <p class="about-text">{{ aboutMe }}</p>
        <div class="about-social">
          <a v-if="github" :href="github" target="_blank" rel="noopener" title="GitHub"><i class="fa-brands fa-github"></i></a>
          <a v-if="email" :href="`mailto:${email}`" title="邮箱"><i class="fa-solid fa-envelope"></i></a>
        </div>
      </div>
    </main>
  </div>
</template>
