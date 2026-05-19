import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import '@/styles/variables.scss'
import '@/styles/mcm-decorations.scss'
import '@/styles/global.scss'

createApp(App)
  .use(pinia)
  .use(router)
  .use(ElementPlus)
  .mount('#app')
