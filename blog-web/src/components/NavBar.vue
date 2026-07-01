<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { theme, toggleTheme } from '../composables/theme'
import { pageHeader } from '../composables/pageHeader'
import { ensureSiteConfig, siteName, logo } from '../composables/siteConfig'
import { isLoggedIn, currentUser, openLogin, doLogout, ensureUser } from '../composables/userAuth'

const NAV = [
  { to: '/', label: '首页', icon: 'fa-solid fa-house' },
  { to: '/archives', label: '归档', icon: 'fa-solid fa-box-archive' },
  { to: '/tags', label: '标签', icon: 'fa-solid fa-tags' },
  { to: '/collections', label: '合集', icon: 'fa-solid fa-folder-open' },
  { to: '/about', label: '关于', icon: 'fa-solid fa-heart' },
]

const router = useRouter()
const scrolled = ref(false)
// 滚动后 或 无 Banner（搜索时）都用实色导航
const solid = computed(() => scrolled.value || pageHeader.hidden)
const drawerOpen = ref(false)
const keyword = ref('')
const userMenuOpen = ref(false)

async function onLogout() {
  await doLogout()
  userMenuOpen.value = false
}

function onSearch() {
  const q = keyword.value.trim()
  router.push(q ? { path: '/', query: { keyword: q } } : { path: '/' })
  drawerOpen.value = false
}

function onScroll() {
  scrolled.value = window.scrollY > 80
}

onMounted(() => {
  ensureSiteConfig()
  ensureUser()
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <header class="ky-nav" :class="{ 'is-solid': solid }">
    <RouterLink class="nav-brand" to="/">
      <img v-if="logo" :src="logo" class="nav-logo-img" alt="" />
      <span v-else class="nav-logo">{{ siteName.slice(0, 1).toUpperCase() }}</span>
      <span>{{ siteName }}</span>
    </RouterLink>

    <nav class="nav-menu">
      <RouterLink v-for="item in NAV" :key="item.to" :to="item.to">
        <i :class="item.icon"></i><span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="nav-right">
      <form class="nav-search" @submit.prevent="onSearch">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input v-model="keyword" type="search" placeholder="搜索文章…" aria-label="搜索文章" />
      </form>
      <!-- 读者登录态 -->
      <div class="nav-user" v-if="isLoggedIn">
        <button class="nav-user-btn" title="账号" @click="userMenuOpen = !userMenuOpen">
          <img v-if="currentUser?.avatar" :src="currentUser.avatar" class="nav-user-avatar" alt="" />
          <span v-else class="nav-user-avatar">{{ (currentUser?.nickname || 'U').slice(0, 1).toUpperCase() }}</span>
        </button>
        <div class="nav-user-menu" v-if="userMenuOpen">
          <div class="nav-user-name">{{ currentUser?.nickname }}</div>
          <button @click="onLogout"><i class="fa-solid fa-right-from-bracket"></i> 退出登录</button>
        </div>
      </div>
      <button v-else class="nav-login-btn" @click="openLogin">
        <i class="fa-solid fa-right-to-bracket"></i><span> 登录</span>
      </button>

      <button
        class="nav-icon-btn"
        :title="theme === 'dark' ? '切换到浅色' : '切换到深色'"
        @click="toggleTheme"
      >
        <i :class="theme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon'"></i>
      </button>
      <button class="nav-icon-btn nav-burger" title="菜单" @click="drawerOpen = true">
        <i class="fa-solid fa-bars"></i>
      </button>
    </div>
  </header>

  <!-- 移动端抽屉 -->
  <div class="nav-drawer" :class="{ open: drawerOpen }">
    <div class="mask" @click="drawerOpen = false"></div>
    <div class="panel">
      <form class="drawer-search" @submit.prevent="onSearch">
        <i class="fa-solid fa-magnifying-glass"></i>
        <input v-model="keyword" type="search" placeholder="搜索文章…" aria-label="搜索文章" />
      </form>
      <RouterLink
        v-for="item in NAV"
        :key="item.to"
        :to="item.to"
        @click="drawerOpen = false"
      >
        <i :class="item.icon"></i><span>{{ item.label }}</span>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }
  .nav-search {
    display: none;
  }
  .nav-burger {
    display: grid;
  }
}
</style>
