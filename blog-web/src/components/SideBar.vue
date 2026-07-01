<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { readingLabel } from '../utils/format'
import {
  allCollections,
  allTags,
  ensureSiteData,
  hotArticles,
  totalArticles,
} from '../composables/siteData'
import {
  ensureSiteConfig,
  author,
  authorAvatar,
  signature,
  github,
  email,
} from '../composables/siteConfig'

const tagCount = computed(() => allTags.value.length)
const collectionCount = computed(() => allCollections.value.length)

onMounted(() => {
  ensureSiteData()
  ensureSiteConfig()
})
</script>

<template>
  <aside class="ky-aside">
    <div class="aside-sticky">
      <!-- 作者卡 -->
      <div class="aside-card aside-author">
        <img v-if="authorAvatar" class="aside-avatar-img" :src="authorAvatar" :alt="author" />
        <div v-else class="aside-avatar">{{ author.slice(0, 1).toUpperCase() }}</div>
        <div class="author-name">{{ author }}</div>
        <div class="author-desc">{{ signature }}</div>
        <div class="author-stats">
          <RouterLink to="/archives">
            <div class="num">{{ totalArticles }}</div>
            <div class="label">文章</div>
          </RouterLink>
          <RouterLink to="/tags">
            <div class="num">{{ tagCount }}</div>
            <div class="label">标签</div>
          </RouterLink>
          <RouterLink to="/collections">
            <div class="num">{{ collectionCount }}</div>
            <div class="label">合集</div>
          </RouterLink>
        </div>
        <div class="author-social">
          <RouterLink to="/about" title="关于我"><i class="fa-solid fa-circle-info"></i></RouterLink>
          <a v-if="github" :href="github" target="_blank" rel="noopener" title="GitHub"><i class="fa-brands fa-github"></i></a>
          <a v-if="email" :href="`mailto:${email}`" title="邮箱"><i class="fa-solid fa-envelope"></i></a>
        </div>
      </div>

      <!-- 热门文章 -->
      <div class="aside-card aside-recent" v-if="hotArticles.length">
        <div class="aside-title"><i class="fa-solid fa-fire"></i>热门文章</div>
        <RouterLink v-for="a in hotArticles" :key="a.id" :to="`/article/${a.id}`">
          <img v-if="a.cover" class="thumb" :src="a.cover" :alt="a.title" loading="lazy" />
          <span v-else class="thumb"></span>
          <span>
            <span class="r-title">{{ a.title }}</span>
            <span class="r-date"><i class="fa-regular fa-eye"></i> {{ readingLabel(a.viewCount) }} 阅读</span>
          </span>
        </RouterLink>
      </div>

      <!-- 合集（分类） -->
      <div class="aside-card aside-cat" v-if="allCollections.length">
        <div class="aside-title"><i class="fa-solid fa-folder-open"></i>合集</div>
        <RouterLink v-for="c in allCollections" :key="c.id" :to="`/collection/${c.id}`">
          <span>{{ c.name }}</span>
          <span class="count">{{ c.articleCount }}</span>
        </RouterLink>
      </div>

      <!-- 标签云 -->
      <div class="aside-card" v-if="allTags.length">
        <div class="aside-title"><i class="fa-solid fa-tags"></i>标签云</div>
        <div class="aside-tags">
          <RouterLink
            v-for="t in allTags"
            :key="t.id"
            :to="`/tag/${t.id}`"
            class="chip"
            :style="{ '--chip-color': t.color || 'var(--ky-theme)' }"
          >
            {{ t.name }}
          </RouterLink>
        </div>
      </div>

      <!-- 网站信息 -->
      <div class="aside-card aside-webinfo">
        <div class="aside-title"><i class="fa-solid fa-chart-line"></i>网站信息</div>
        <div class="row"><span>文章数目</span><span>{{ totalArticles }}</span></div>
        <div class="row"><span>标签数目</span><span>{{ tagCount }}</span></div>
        <div class="row"><span>合集数目</span><span>{{ collectionCount }}</span></div>
      </div>
    </div>
  </aside>
</template>
