import { createApp } from 'vue'
import VueSonner from 'vue-sonner'
import 'vue-sonner/style.css'
import App from './App.vue'
import router from './router'
import legacyUi from './plugins/legacy-ui'
import './assets/tailwind.css'
import './assets/mcm-theme.css'

const app = createApp(App)

app.use(router)
app.use(VueSonner)
app.use(legacyUi)

app.mount('#app')
