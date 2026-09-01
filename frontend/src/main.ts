import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@fontsource-variable/inter'
import '@fontsource-variable/jetbrains-mono'
import App from './App.vue'
import router from './router'
import './styles/main.css'
import { initTheme } from './lib/theme'

initTheme()

createApp(App).use(createPinia()).use(router).mount('#app')
