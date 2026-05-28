<template>
  <div class="layout">
    <Sidebar />
    <div class="layout-main">
      <Header />
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Header from './components/Header.vue'
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255,255,255,.72), rgba(243,247,252,.58)),
    var(--main-bg);
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
</style>
