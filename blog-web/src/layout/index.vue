<template>
  <div class="site-shell">
    <header class="site-header">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">K</span>
        <span>
          <strong>Kyonelife</strong>
          <small>planet journal</small>
        </span>
      </RouterLink>

      <nav class="site-nav" aria-label="主导航">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/tags">标签</RouterLink>
        <RouterLink to="/collections">合集</RouterLink>
        <RouterLink to="/archives">归档</RouterLink>
        <RouterLink to="/about">关于</RouterLink>
      </nav>
    </header>

    <main>
      <RouterView />
    </main>

    <footer class="site-footer">
      <span>守护一朵玫瑰，也记录每一次远行。</span>
      <span>Kyonelife Blog</span>
    </footer>
  </div>
</template>
