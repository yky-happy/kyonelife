<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { pageHeader, HOME_BANNER } from '../composables/pageHeader'
import { trackEvent } from '../utils/tracker'

const router = useRouter()

function onCategoryClick() {
  const id = pageHeader.postCategoryId
  if (!id) return
  trackEvent('collection_click', {
    collectionId: id,
    pageUrl: `/collection/${id}`,
  })
  router.push(`/collection/${id}`)
}

/* ============ 轮播图 ============ */
// home 用 bgList 多图轮播；其余类型只有一张（bg）
const slides = computed(() => {
  if (pageHeader.type === 'home' && pageHeader.bgList.length) {
    return pageHeader.bgList
  }
  return [pageHeader.bg]
})
const isCarousel = computed(() => pageHeader.type === 'home' && slides.value.length > 1)
const current = ref(0)
let slideTimer: number | undefined

function clearSlideTimer() {
  if (slideTimer) {
    window.clearInterval(slideTimer)
    slideTimer = undefined
  }
}

function go(index: number) {
  current.value = (index + slides.value.length) % slides.value.length
}

watch(
  slides,
  () => {
    current.value = 0
    clearSlideTimer()
    if (isCarousel.value) {
      slideTimer = window.setInterval(() => go(current.value + 1), 5000)
    }
  },
  { immediate: true },
)

function onSlideError(e: Event) {
  const img = e.target as HTMLImageElement
  if (!img.src.endsWith(HOME_BANNER)) {
    img.src = HOME_BANNER
  }
}

/* ============ 打字机副标题 ============ */
const typed = ref('')
let typeTimer: number | undefined
let phraseIdx = 0
let charIdx = 0
let deleting = false

function clearTypeTimer() {
  if (typeTimer) {
    window.clearTimeout(typeTimer)
    typeTimer = undefined
  }
}

function tick() {
  const phrases = pageHeader.typewriter
  if (!phrases.length) return
  const full = phrases[phraseIdx % phrases.length]
  if (deleting) {
    charIdx -= 1
    typed.value = full.slice(0, charIdx)
    if (charIdx <= 0) {
      deleting = false
      phraseIdx += 1
      typeTimer = window.setTimeout(tick, 400)
      return
    }
    typeTimer = window.setTimeout(tick, 45)
  } else {
    charIdx += 1
    typed.value = full.slice(0, charIdx)
    if (charIdx >= full.length) {
      deleting = true
      typeTimer = window.setTimeout(tick, 1800)
      return
    }
    typeTimer = window.setTimeout(tick, 110)
  }
}

watch(
  () => pageHeader.typewriter,
  (phrases) => {
    clearTypeTimer()
    typed.value = ''
    phraseIdx = 0
    charIdx = 0
    deleting = false
    if (phrases.length) tick()
  },
  { immediate: true },
)

function scrollDown() {
  window.scrollTo({ top: window.innerHeight - 60, behavior: 'smooth' })
}

onUnmounted(() => {
  clearSlideTimer()
  clearTypeTimer()
})
</script>

<template>
  <section class="ky-banner" :class="`type-${pageHeader.type}`">
    <!-- 背景 / 轮播图层 -->
    <div class="banner-slides">
      <img
        v-for="(img, i) in slides"
        :key="`${img}-${i}`"
        class="banner-slide"
        :class="{ active: i === current }"
        :src="img"
        alt=""
        @error="onSlideError"
      />
    </div>

    <div class="banner-inner">
      <h1 class="banner-title" :class="{ 'song-font': pageHeader.songTitle }">{{ pageHeader.title }}</h1>

      <p v-if="pageHeader.type === 'home'" class="banner-sub">
        <span>{{ typed }}</span><span class="typed-cursor">&nbsp;</span>
      </p>

      <p v-else-if="pageHeader.type === 'page' && pageHeader.subtitle" class="banner-sub">
        {{ pageHeader.subtitle }}
      </p>

      <div v-if="pageHeader.type === 'post'" class="banner-meta">
        <div class="meta-line">
          <span><i class="fa-regular fa-calendar"></i>发表于 {{ pageHeader.postDate }}</span>
          <span class="sep">|</span>
          <span><i class="fa-solid fa-clock-rotate-left"></i>更新于 {{ pageHeader.postUpdate || pageHeader.postDate }}</span>
          <template v-if="pageHeader.postCategory">
            <span class="sep">|</span>
            <span
              v-if="pageHeader.postCategoryId"
              class="banner-link"
              role="link"
              tabindex="0"
              @click="onCategoryClick"
              @keyup.enter="onCategoryClick"
            >
              <i class="fa-solid fa-inbox"></i>{{ pageHeader.postCategory }}
            </span>
            <span v-else>
              <i class="fa-solid fa-inbox"></i>{{ pageHeader.postCategory }}
            </span>
          </template>
        </div>
        <div class="meta-line">
          <span><i class="fa-regular fa-eye"></i>浏览量: {{ pageHeader.postViews }}</span>
        </div>
      </div>
    </div>

    <!-- 轮播圆点 -->
    <div v-if="isCarousel" class="banner-dots">
      <button
        v-for="(_, i) in slides"
        :key="i"
        :class="{ active: i === current }"
        :aria-label="`第 ${i + 1} 张`"
        @click="go(i)"
      ></button>
    </div>

    <button v-if="pageHeader.type === 'home'" class="scroll-down" title="向下滚动" @click="scrollDown">
      <i class="fa-solid fa-angle-down"></i>
    </button>
  </section>
</template>
