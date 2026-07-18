import 'frappe-ui/style.css'
import './styles/index.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { FrappeUI } from 'frappe-ui'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 无 Frappe 后端：关闭 socketio，否则组件库默认去连 :9000/socket.io 并无限重试。
app.use(FrappeUI, { socketio: false })
app.use(createPinia())
app.use(router)

app.mount('#app')
