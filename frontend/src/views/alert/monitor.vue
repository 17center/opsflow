<template>
  <div class="monitor-page">
    <div class="monitor-toolbar">
      <span class="page-title">
        <el-icon><DataLine /></el-icon>
        实时监控大屏
      </span>
      <div class="toolbar-right">
        <span class="src-hint">数据源：Prometheus · {{ serverHost }}</span>
        <el-button size="small" :icon="Refresh" @click="reload">刷新</el-button>
        <el-button size="small" text :icon="FullScreen" @click="fullscreen">全屏</el-button>
      </div>
    </div>

    <div class="monitor-frame" ref="frameWrap">
      <iframe
        :key="frameKey"
        class="grafana-frame"
        :src="grafanaUrl"
        frameborder="0"
        allowfullscreen
      ></iframe>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Refresh, FullScreen, DataLine } from '@element-plus/icons-vue'

const serverHost = window.location.host

function buildGrafanaUrl() {
  return '/grafana/d/opsflow-node-overview?orgId=1&kiosk=tv&refresh=15s&from=now-6h&to=now&_t=' + Date.now()
}

const grafanaUrl = ref(buildGrafanaUrl())
const frameKey = ref(0)
const frameWrap = ref<HTMLElement>()

function reload() {
  grafanaUrl.value = buildGrafanaUrl()
  frameKey.value++ // 递增 key 强制 iframe 重载
}

function fullscreen() {
  const el = frameWrap.value
  if (!el) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    el.requestFullscreen?.()
  }
}
</script>

<style scoped>
.monitor-page {
  height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.monitor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-header);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  flex-shrink: 0;
}

.page-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.src-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin-right: 6px;
}

.monitor-frame {
  flex: 1;
  min-height: 0;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.grafana-frame {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>