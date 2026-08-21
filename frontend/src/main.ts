import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/styles/theme.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from '@/stores/theme'

const app = createApp(App)

// 全局注册 Element Plus 图标，供模板中 <el-icon><IconName /></el-icon> 使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 先安装 Pinia 再应用主题设置，避免首屏闪烁
useThemeStore().init()

app.mount('#app')