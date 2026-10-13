import { createApp } from 'vue'

import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import './assets/styles/design-tokens.css'

const app = createApp(App)

app.use(pinia)
app.use(router)

app.mount('#app')
