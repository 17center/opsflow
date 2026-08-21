import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'

// ============ 服务资产 ============

/** 服务资产 */
export interface CmdbService {
  id: number
  name: string
  serviceType: string
  version?: string
  hostId?: number
  hostName?: string
  hostIp?: string
  port?: number
  status: number
  statusName?: string
  ownerId?: number
  ownerName?: string
  createTime?: string
  remark?: string
}

/** 服务资产请求 */
export interface CmdbServiceForm {
  name: string
  serviceType: string
  version?: string
  hostId?: number
  port?: number
  status?: number
  ownerId?: number
  remark?: string
}

/** 自动发现的服务 */
export interface DiscoveredService {
  name: string
  serviceType: string
  hostId: number
  port: number
}

/** 服务状态文本 */
export const SERVICE_STATUS_TEXT: Record<number, string> = {
  0: '不可用',
  1: '运行中',
  2: '维护中'
}

/** 服务状态 → 标签 */
export const SERVICE_STATUS_TAG: Record<number, 'info' | 'success' | 'warning' | 'danger'> = {
  0: 'danger',
  1: 'success',
  2: 'warning'
}

/** 常见服务类型 */
export const SERVICE_TYPES = ['MySQL', 'Redis', 'Nginx', 'Tomcat', 'PostgreSQL', 'MongoDB', 'Elasticsearch', 'Kafka', 'RabbitMQ']

export function pageServices(params: {
  current?: number
  size?: number
  keyword?: string
  serviceType?: string
  status?: number
} = {}) {
  return get<PageResult<CmdbService>>('/cmdb/services', params)
}

export function getService(id: number) {
  return get<CmdbService>(`/cmdb/services/${id}`)
}

export function createService(data: CmdbServiceForm) {
  return post<null>('/cmdb/services', data)
}

export function updateService(id: number, data: CmdbServiceForm) {
  return put<null>(`/cmdb/services/${id}`, data)
}

export function deleteService(id: number) {
  return del<null>(`/cmdb/services/${id}`)
}

export function changeServiceStatus(id: number, status: number) {
  return post<null>(`/cmdb/services/${id}/status`, { status })
}

export function discoverServices(hostId: number) {
  return post<DiscoveredService[]>(`/cmdb/services/discover/${hostId}`)
}

export function batchCreateServices(data: CmdbServiceForm[]) {
  return post<null>('/cmdb/services/batch', data)
}

// ============ 资产关联 ============

/** 资产关联 */
export interface CmdbRelation {
  id: number
  sourceType: string
  sourceTypeName?: string
  sourceId: number
  sourceName?: string
  targetType: string
  targetTypeName?: string
  targetId: number
  targetName?: string
  relationType: string
  relationTypeName?: string
  createTime?: string
}

/** 资产关联请求 */
export interface CmdbRelationForm {
  sourceType: string
  sourceId: number
  targetType: string
  targetId: number
  relationType: string
}

/** 拓扑数据 */
export interface TopologyNode {
  id: string
  type: string
  name: string
  label: string
  status: number
  hostId?: number
}

export interface TopologyEdge {
  source: string
  target: string
  relationType: string
}

export interface CmdbTopology {
  nodes: TopologyNode[]
  edges: TopologyEdge[]
}

/** 关系类型文本 */
export const RELATION_TYPE_TEXT: Record<string, string> = {
  DEPLOYED_ON: '部署于',
  DEPENDS_ON: '依赖',
  CONTAINS: '包含'
}

/** 资产类型文本 */
export const ASSET_TYPE_TEXT: Record<string, string> = {
  HOST: '主机',
  SERVICE: '服务'
}

export function listRelations() {
  return get<CmdbRelation[]>('/cmdb/relations')
}

export function createRelation(data: CmdbRelationForm) {
  return post<null>('/cmdb/relations', data)
}

export function deleteRelation(id: number) {
  return del<null>(`/cmdb/relations/${id}`)
}

export function getTopology() {
  return get<CmdbTopology>('/cmdb/relations/topology')
}