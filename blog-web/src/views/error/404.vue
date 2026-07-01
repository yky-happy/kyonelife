<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { setPageHeader } from '../../composables/pageHeader'
import { setMeta } from '../../utils/seo'

onMounted(() => {
  setPageHeader({ type: 'page', title: '404', subtitle: '这里好像什么都没有…' })
  setMeta({ title: '页面不存在' })
})
</script>

<template>
  <div class="ky-layout">
    <main class="ky-main">
      <div class="ky-card not-found">
        <div class="nf-code">404</div>
        <p class="nf-text">页面走丢了，要不回首页看看？</p>
        <RouterLink to="/" class="ky-btn"><i class="fa-solid fa-house"></i>返回首页</RouterLink>
      </div>
    </main>
  </div>
</template>
