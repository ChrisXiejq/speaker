import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './style.css'
import App from './App.vue'
import router from './router'

const isLocalDevelopment = ['localhost', '127.0.0.1'].includes(window.location.hostname)

if (window.location.protocol === 'http:' && !isLocalDevelopment) {
  const secureUrl = new URL(window.location.href)
  secureUrl.protocol = 'https:'
  secureUrl.port = ''
  window.location.replace(secureUrl.toString())
} else {
  const app = createApp(App)
  app.use(router)
  app.use(ElementPlus)
  app.mount('#app')
}
