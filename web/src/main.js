import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

// 样式：Element Plus / Vant 整包 CSS 统一在此引入（组件仍按需打包）。
// 这样避免"按组件引样式"漏样式的问题；若后续首屏压力大，可改为按需引样式。
import 'element-plus/dist/index.css'
import 'vant/lib/index.css'
// 本项目全局样式与主题变量（必须在两个 UI 库之后，才能覆盖变量）
import './styles/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
