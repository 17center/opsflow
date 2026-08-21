import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'
export type SidebarStyle = 'dark' | 'light' | 'gradient'

export interface BrandPreset {
  key: string
  name: string
  color: string
}

/** 可选的品牌强调色 */
export const BRAND_PRESETS: BrandPreset[] = [
  { key: 'ocean', name: '深海蓝', color: '#409eff' },
  { key: 'teal', name: '青碧', color: '#00b8a9' },
  { key: 'violet', name: '紫罗兰', color: '#7c5cff' },
  { key: 'orange', name: '暖橙', color: '#ff7a45' },
  { key: 'rose', name: '玫红', color: '#f04a6a' },
  { key: 'emerald', name: '翡翠', color: '#2fbf71' }
]

const STORAGE_KEY = 'opsflow_theme'

interface ThemeState {
  mode: ThemeMode
  brandKey: string
  sidebar: SidebarStyle
}

function hexToRgb(hex: string) {
  const h = hex.replace('#', '')
  const full = h.length === 3 ? h.split('').map((c) => c + c).join('') : h
  const n = parseInt(full, 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

/** 向指定目标色(白=255/黑=0)按权重混合 */
function mix(hex: string, target: number, weight: number) {
  const { r, g, b } = hexToRgb(hex)
  const blend = (a: number) => Math.round(a + (target - a) * weight)
  return `#${[blend(r), blend(g), blend(b)]
    .map((v) => v.toString(16).padStart(2, '0'))
    .join('')}`
}
const lighten = (hex: string, w: number) => mix(hex, 255, w)
const darken = (hex: string, w: number) => mix(hex, 0, w)

function loadState(): ThemeState {
  const fallback: ThemeState = { mode: 'dark', brandKey: 'ocean', sidebar: 'gradient' }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return fallback
    return { ...fallback, ...JSON.parse(raw) }
  } catch {
    return fallback
  }
}

/** 将品牌色写入 CSS 变量，并同步到 Element Plus 主题 */
function applyBrand(brandKey: string) {
  const preset = BRAND_PRESETS.find((p) => p.key === brandKey) ?? BRAND_PRESETS[0]
  const root = document.documentElement
  const shades = {
    '--brand': preset.color,
    '--brand-light-3': lighten(preset.color, 0.3),
    '--brand-light-5': lighten(preset.color, 0.5),
    '--brand-light-7': lighten(preset.color, 0.7),
    '--brand-light-8': lighten(preset.color, 0.8),
    '--brand-light-9': lighten(preset.color, 0.9),
    '--brand-dark-2': darken(preset.color, 0.2),
    '--el-color-primary': preset.color,
    '--el-color-primary-light-3': lighten(preset.color, 0.3),
    '--el-color-primary-light-5': lighten(preset.color, 0.5),
    '--el-color-primary-light-7': lighten(preset.color, 0.7),
    '--el-color-primary-light-8': lighten(preset.color, 0.8),
    '--el-color-primary-light-9': lighten(preset.color, 0.9),
    '--el-color-primary-dark-2': darken(preset.color, 0.2)
  }
  Object.entries(shades).forEach(([k, v]) => root.style.setProperty(k, v))
}

/** 应用明暗模式：设置 data-theme 属性 + 切换 Element Plus 的 dark class */
function applyMode(mode: ThemeMode) {
  const root = document.documentElement
  root.setAttribute('data-theme', mode)
  root.classList.toggle('dark', mode === 'dark')
}

function applySidebar(sidebar: SidebarStyle) {
  document.documentElement.setAttribute('data-sidebar', sidebar)
}

export const useThemeStore = defineStore('theme', {
  state: (): ThemeState => loadState(),

  actions: {
    /** 应用全部主题设置到 DOM 并持久化 */
    apply() {
      applyMode(this.mode)
      applyBrand(this.brandKey)
      applySidebar(this.sidebar)
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ mode: this.mode, brandKey: this.brandKey, sidebar: this.sidebar })
      )
    },
    setMode(mode: ThemeMode) {
      this.mode = mode
      this.apply()
    },
    setBrand(key: string) {
      this.brandKey = key
      this.apply()
    },
    setSidebar(s: SidebarStyle) {
      this.sidebar = s
      this.apply()
    },
    /** 初始化：在应用挂载前调用，避免首屏闪烁 */
    init() {
      this.apply()
    }
  }
})