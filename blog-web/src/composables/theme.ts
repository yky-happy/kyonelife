import { ref } from 'vue'

const THEME_KEY = 'kyonelife_theme'
export type ThemeMode = 'light' | 'dark'

function readTheme(): ThemeMode {
  const saved = localStorage.getItem(THEME_KEY)
  return saved === 'dark' ? 'dark' : 'light'
}

export const theme = ref<ThemeMode>(readTheme())

function apply(mode: ThemeMode) {
  document.documentElement.setAttribute('data-theme', mode)
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', mode === 'dark' ? '#0d0d0d' : '#ffffff')
}

export function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem(THEME_KEY, theme.value)
  apply(theme.value)
}

// 启动时同步一次（index.html 已先行设置，这里保证 Vue 状态一致）
apply(theme.value)
