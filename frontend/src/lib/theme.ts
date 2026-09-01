import { ref } from 'vue'

export type Theme = 'dark' | 'light'

const STORAGE_KEY = 'relay.theme'

function resolveInitial(): Theme {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'dark' || saved === 'light') {
    return saved
  }
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

const theme = ref<Theme>(resolveInitial())

function apply(value: Theme) {
  document.documentElement.setAttribute('data-theme', value)
}

export function initTheme() {
  apply(theme.value)
}

export function useTheme() {
  function toggle() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem(STORAGE_KEY, theme.value)
    apply(theme.value)
  }

  return { theme, toggle }
}
