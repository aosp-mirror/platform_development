import { createApp } from 'vue'
import vuetify from './plugins/vuetify'
import App from './App.vue'
import router from './router'
import store from './store'

const app = createApp(App)
app.use(router)
app.use(store)
app.use(vuetify)

app.mount('#app')