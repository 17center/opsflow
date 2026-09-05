<template>
  <div class="terminal-page">
    <!-- 工具栏 -->
    <div class="terminal-toolbar">
      <div class="toolbar-left">
        <el-select
          v-model="selectedHostId"
          placeholder="选择主机"
          style="width: 240px"
          :disabled="connected"
          @focus="loadHosts"
        >
          <el-option
            v-for="h in hosts"
            :key="h.id"
            :label="`${h.hostname} (${h.ipAddress})`"
            :value="h.id"
          />
        </el-select>
        <el-button
          type="primary"
          :disabled="!selectedHostId || connected"
          :loading="connecting"
          @click="connect"
        >
          <el-icon><Connection /></el-icon>
          连接
        </el-button>
        <el-button
          type="danger"
          :disabled="!connected"
          @click="disconnect"
        >
          <el-icon><SwitchButton /></el-icon>
          断开
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-tag v-if="connected" type="success" effect="dark" size="small">
          已连接: {{ currentHostName }}
        </el-tag>
        <el-tag v-else-if="connecting" type="warning" effect="dark" size="small">
          连接中...
        </el-tag>
        <el-tag v-else type="info" size="small">未连接</el-tag>
      </div>
    </div>

    <!-- 终端区域 -->
    <div class="terminal-container" ref="terminalContainer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import '@xterm/xterm/css/xterm.css'
import { ElMessage } from 'element-plus'
import { get } from '@/utils/request'

interface HostOption {
  id: number
  hostname: string
  ipAddress: string
}

const selectedHostId = ref<number | null>(null)
const hosts = ref<HostOption[]>([])
const connected = ref(false)
const connecting = ref(false)
const currentHostName = ref('')
const terminalContainer = ref<HTMLElement>()

let term: Terminal | null = null
let fitAddon: FitAddon | null = null
let ws: WebSocket | null = null
let resizeObserver: ResizeObserver | null = null

async function loadHosts() {
  if (hosts.value.length > 0) return
  try {
    const res = await get<{ records: HostOption[] }>('/automation/hosts', {
      params: { current: 1, size: 100 }
    })
    hosts.value = res.records || []
  } catch {
    ElMessage.error('主机列表加载失败')
  }
}

function initTerminal() {
  if (term) return
  term = new Terminal({
    fontSize: 14,
    fontFamily: "'Cascadia Code', 'Fira Code', 'Consolas', monospace",
    theme: {
      background: '#1e1e2e',
      foreground: '#cdd6f4',
      cursor: '#f5e0dc',
      selectionBackground: '#585b70'
    },
    cursorBlink: true,
    allowProposedApi: true
  })
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(terminalContainer.value!)
  fitAddon.fit()

  // 窗口大小变化时重新适配
  resizeObserver = new ResizeObserver(() => {
    if (fitAddon && term && connected.value) {
      fitAddon.fit()
    }
  })
  resizeObserver.observe(terminalContainer.value!)
}

function connect() {
  if (!selectedHostId.value || !term) return
  connecting.value = true

  const token = localStorage.getItem('opsflow_token')
  if (!token) {
    ElMessage.error('未登录')
    return
  }

  const host = hosts.value.find(h => h.id === selectedHostId.value)
  currentHostName.value = host ? `${host.hostname} (${host.ipAddress})` : ''

  // 构造 WebSocket URL
  const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${proto}//${location.host}/ws/terminal?hostId=${selectedHostId.value}&token=${token}`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    connecting.value = false
    connected.value = true
    term!.clear()
    term!.writeln(`\x1b[32m*** 连接成功: ${currentHostName.value} ***\x1b[0m\r\n`)
    // 发送初始尺寸
    sendResize()
  }

  ws.onmessage = (event) => {
    term!.write(event.data)
  }

  ws.onerror = () => {
    connecting.value = false
    connected.value = false
    ElMessage.error('WebSocket 连接失败')
  }

  ws.onclose = () => {
    connecting.value = false
    connected.value = false
    if (term) {
      term.writeln('\r\n\x1b[31m*** 连接已断开 ***\x1b[0m')
    }
  }

  // 用户输入 → WebSocket
  term.onData((data) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(data)
    }
  })

  // 终端尺寸变化 → 发送 resize
  term.onResize(({ cols, rows }) => {
    sendResize(cols, rows)
  })
}

function sendResize(cols?: number, rows?: number) {
  if (!ws || ws.readyState !== WebSocket.OPEN || !term) return
  const c = cols ?? term.cols
  const r = rows ?? term.rows
  ws.send(JSON.stringify({ type: 'resize', cols: c, rows: r }))
}

function disconnect() {
  if (ws) {
    ws.close()
    ws = null
  }
  connected.value = false
}

onMounted(async () => {
  await nextTick()
  initTerminal()
})

onBeforeUnmount(() => {
  disconnect()
  resizeObserver?.disconnect()
  term?.dispose()
  term = null
})
</script>

<style scoped>
.terminal-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
}

.terminal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.terminal-container {
  flex: 1;
  padding: 8px;
  background: #1e1e2e;
  overflow: hidden;
}

.terminal-container :deep(.xterm) {
  padding: 8px;
}
</style>
