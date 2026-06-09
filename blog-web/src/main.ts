import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { trackPageView } from './utils/tracker'

router.afterEach((to) => {
  trackPageView(to.fullPath)
})

createApp(App).use(router).mount('#app')
