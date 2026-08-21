<template>
  <div>
    <!-- 拓扑图 -->
    <el-card shadow="never" class="topo-card">
      <template #header>
        <div class="topo-header">
          <span>资产拓扑图</span>
          <el-button size="small" :icon="Refresh" @click="loadTopology">刷新</el-button>
        </div>
      </template>
      <div ref="topoRef" class="topo-canvas" v-loading="topoLoading"></div>
    </el-card>

    <!-- 关联管理 -->
    <el-card shadow="never" class="relation-card">
      <template #header>
        <div class="topo-header">
          <span>关联关系管理</span>
          <el-button type="primary" size="small" :icon="Plus" @click="openCreate">建立关联</el-button>
        </div>
      </template>

      <el-table :data="relations" v-loading="relLoading" border stripe size="small">
        <el-table-column label="源资产" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'HOST' ? '' : 'success'">{{ row.sourceTypeName }}</el-tag>
            <span style="margin-left: 6px">{{ row.sourceName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关系" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="warning">{{ row.relationTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标资产" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.targetType === 'HOST' ? '' : 'success'">{{ row.targetTypeName }}</el-tag>
            <span style="margin-left: 6px">{{ row.targetName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="165" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 建立关联弹窗 -->
    <el-dialog v-model="createVisible" title="建立关联" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="源资产" prop="source">
          <el-select v-model="form.source" filterable placeholder="选择源资产" style="width: 100%">
            <el-option-group label="主机">
              <el-option v-for="h in hosts" :key="`H-${h.id}`" :label="h.hostname" :value="`HOST:${h.id}`" />
            </el-option-group>
            <el-option-group label="服务">
              <el-option v-for="s in services" :key="`S-${s.id}`" :label="s.name" :value="`SERVICE:${s.id}`" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select v-model="form.relationType" style="width: 100%">
            <el-option v-for="(v, k) in RELATION_TYPE_TEXT" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标资产" prop="target">
          <el-select v-model="form.target" filterable placeholder="选择目标资产" style="width: 100%">
            <el-option-group label="主机">
              <el-option v-for="h in hosts" :key="`H-${h.id}`" :label="h.hostname" :value="`HOST:${h.id}`" />
            </el-option-group>
            <el-option-group label="服务">
              <el-option v-for="s in services" :key="`S-${s.id}`" :label="s.name" :value="`SERVICE:${s.id}`" />
            </el-option-group>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  listRelations,
  createRelation,
  deleteRelation,
  getTopology,
  RELATION_TYPE_TEXT,
  type CmdbRelation,
  type CmdbRelationForm,
  type CmdbTopology,
  type TopologyNode,
  type TopologyEdge
} from '@/api/cmdb'
import { pageHosts, type CmdbHost } from '@/api/automation'
import { pageServices, type CmdbService } from '@/api/cmdb'

const topoRef = ref<HTMLElement>()
let topoChart: echarts.ECharts | null = null
const topoLoading = ref(false)

const relations = ref<CmdbRelation[]>([])
const relLoading = ref(false)
const hosts = ref<CmdbHost[]>([])
const services = ref<CmdbService[]>([])

const createVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<{ source?: string; relationType: string; target?: string }>({
  source: undefined,
  relationType: 'DEPENDS_ON',
  target: undefined
})

const rules: FormRules = {
  source: [{ required: true, message: '请选择源资产', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
  target: [{ required: true, message: '请选择目标资产', trigger: 'change' }]
}

const statusColor: Record<number, string> = {
  0: '#f56c6c',
  1: '#67c23a',
  2: '#e6a23c'
}

async function loadAssets() {
  const [h, s] = await Promise.all([
    pageHosts({ current: 1, size: 100 }),
    pageServices({ current: 1, size: 100 })
  ])
  hosts.value = h.records
  services.value = s.records
}

async function loadRelations() {
  relLoading.value = true
  try {
    relations.value = await listRelations()
  } finally {
    relLoading.value = false
  }
}

async function loadTopology() {
  topoLoading.value = true
  try {
    const data: CmdbTopology = await getTopology()
    renderTopology(data)
  } finally {
    topoLoading.value = false
  }
}

function renderTopology(data: CmdbTopology) {
  awaitNextTick()
  topoChart = topoChart || echarts.init(topoRef.value!)
  const nodes = data.nodes.map((n: TopologyNode) => ({
    id: n.id,
    name: n.label || n.name,
    symbolSize: n.type === 'HOST' ? 46 : 34,
    itemStyle: {
      color: statusColor[n.status] || '#909399',
      borderColor: n.type === 'HOST' ? '#409eff' : '#67c23a',
      borderWidth: 2
    },
    label: { show: true, position: n.type === 'HOST' ? 'bottom' : 'top', fontSize: 11 },
    category: n.type
  }))
  const edges = data.edges.map((e: TopologyEdge) => ({
    source: e.source,
    target: e.target,
    label: { show: true, formatter: RELATION_TYPE_TEXT[e.relationType] || e.relationType, fontSize: 10 }
  }))
  topoChart.setOption({
    tooltip: {
      formatter: (p: any) => {
        if (p.dataType === 'edge') return `${p.data.label?.formatter || ''}`
        return p.data.name
      }
    },
    legend: {
      data: [
        { name: 'HOST', itemStyle: { color: '#409eff' } },
        { name: 'SERVICE', itemStyle: { color: '#67c23a' } }
      ],
      bottom: 0
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: nodes,
        links: edges,
        categories: [
          { name: 'HOST', itemStyle: { color: '#409eff' } },
          { name: 'SERVICE', itemStyle: { color: '#67c23a' } }
        ],
        roam: true,
        draggable: true,
        force: { repulsion: 200, edgeLength: 120 },
        label: { show: true },
        emphasis: { focus: 'adjacency' }
      }
    ]
  })
}

function awaitNextTick() {
  return new Promise<void>((resolve) => nextTick(resolve))
}

async function openCreate() {
  await loadAssets()
  form.source = undefined
  form.relationType = 'DEPENDS_ON'
  form.target = undefined
  createVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  const [srcType, srcId] = (form.source as string).split(':')
  const [tgtType, tgtId] = (form.target as string).split(':')
  const data: CmdbRelationForm = {
    sourceType: srcType,
    sourceId: Number(srcId),
    targetType: tgtType,
    targetId: Number(tgtId),
    relationType: form.relationType
  }
  saving.value = true
  try {
    await createRelation(data)
    ElMessage.success('关联成功')
    createVisible.value = false
    loadRelations()
    loadTopology()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: CmdbRelation) {
  await ElMessageBox.confirm(`确定删除关联「${row.sourceName} ${row.relationTypeName} ${row.targetName}」吗？`, '提示', { type: 'warning' })
  await deleteRelation(row.id)
  ElMessage.success('删除成功')
  loadRelations()
  loadTopology()
}

function handleResize() {
  topoChart?.resize()
}

onMounted(() => {
  loadRelations()
  loadTopology()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  topoChart?.dispose()
})
</script>

<style scoped>
.topo-card {
  margin-bottom: 16px;
}

.topo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topo-canvas {
  height: 480px;
}

.relation-card {
  margin-bottom: 16px;
}
</style>