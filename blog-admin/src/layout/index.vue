<template>
  <div class="layout">
    <Sidebar :mobile-open="mobileOpen" @close="mobileOpen = false" />
    <div class="sidebar-backdrop" :class="{ show: mobileOpen }" @click="mobileOpen = false"></div>
    <div class="layout-main">
      <Header @toggle-sidebar="mobileOpen = !mobileOpen" />
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'

const mobileOpen = ref(false)
const route = useRoute()
// 切换路由时自动收起移动端抽屉
watch(() => route.path, () => { mobileOpen.value = false })
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 14% 2%, rgba(216, 208, 196, .22), transparent 32%),
    radial-gradient(circle at 92% 4%, rgba(79, 141, 131, .07), transparent 28%),
    linear-gradient(180deg, #f5f6f8 0%, #eceef2 100%);
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout-content {
  flex: 1;
  overflow-y: auto;
  padding: 22px 24px 28px;
}

/* 移动端抽屉遮罩：默认隐藏，窄屏开启抽屉时显示 */
.sidebar-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(28, 37, 48, .38);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  opacity: 0;
  visibility: hidden;
  transition: opacity .28s ease, visibility .28s ease;
}

.sidebar-backdrop.show {
  opacity: 1;
  visibility: visible;
}

@media (min-width: 1025px) {
  .sidebar-backdrop { display: none; }
}
</style>
