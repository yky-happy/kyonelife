<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import TheBanner from '../components/TheBanner.vue'
import LoginModal from '../components/LoginModal.vue'
import { pageHeader } from '../composables/pageHeader'
import { ensureSiteConfig, siteName, icpNumber } from '../composables/siteConfig'
import { loginModalOpen } from '../composables/userAuth'

const showTop = ref(false)

function onScroll() {
  showTop.value = window.scrollY > 400
}

function backToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  ensureSiteConfig()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <NavBar />
  <TheBanner v-if="!pageHeader.hidden" />

  <RouterView v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </RouterView>

  <footer class="ky-footer">
    <div class="row">{{ siteName }}</div>
    <div class="row" v-if="icpNumber">
      <a href="https://beian.miit.gov.cn" target="_blank" rel="noopener">{{ icpNumber }}</a>
    </div>
  </footer>

  <button class="back-to-top" :class="{ show: showTop }" title="回到顶部" @click="backToTop">
    <i class="fa-solid fa-arrow-up"></i>
  </button>

  <LoginModal v-if="loginModalOpen" />
</template>
