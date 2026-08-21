import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'

// ============ 知识标签 ============

/** 知识标签 */
export interface KbTag {
  id: number
  name: string
  createTime?: string
}

export function listKbTags() {
  return get<KbTag[]>('/kb/tags')
}

export function createKbTag(data: { name: string }) {
  return post<null>('/kb/tags', data)
}

export function deleteKbTag(id: number) {
  return del<null>(`/kb/tags/${id}`)
}

// ============ 知识文章 ============

/** 知识文章 */
export interface KbArticle {
  id: number
  title: string
  content: string
  category: number
  categoryName?: string
  status: number
  statusName?: string
  viewCount: number
  relatedTicketId?: number
  authorId?: number
  authorName?: string
  tagIds: number[]
  tagNames: string[]
  createTime?: string
  remark?: string
}

/** 知识文章请求 */
export interface KbArticleForm {
  title: string
  content: string
  category: number
  tagIds: number[]
  status?: number
  remark?: string
}

/** 分类文本 */
export const CATEGORY_TEXT: Record<number, string> = {
  1: '故障排查',
  2: '操作手册',
  3: '最佳实践',
  4: 'FAQ'
}

/** 状态文本 */
export const ARTICLE_STATUS_TEXT: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '审核中'
}

/** 状态 → 标签 */
export const ARTICLE_STATUS_TAG: Record<number, 'info' | 'success' | 'warning'> = {
  0: 'info',
  1: 'success',
  2: 'warning'
}

export function pageKbArticles(params: {
  current?: number
  size?: number
  category?: number
  status?: number
  keyword?: string
  tagId?: number
} = {}) {
  return get<PageResult<KbArticle>>('/kb/articles', params)
}

export function getKbArticle(id: number) {
  return get<KbArticle>(`/kb/articles/${id}`)
}

export function createKbArticle(data: KbArticleForm) {
  return post<null>('/kb/articles', data)
}

export function updateKbArticle(id: number, data: KbArticleForm) {
  return put<null>(`/kb/articles/${id}`, data)
}

export function deleteKbArticle(id: number) {
  return del<null>(`/kb/articles/${id}`)
}

export function changeKbArticleStatus(id: number, status: number) {
  return post<null>(`/kb/articles/${id}/status`, { status })
}

export function kbArticleFromTicket(ticketId: number) {
  return post<KbArticle>(`/kb/articles/from-ticket/${ticketId}`)
}

// ============ 智能问答 ============

/** 智能问答 */
export interface KbQa {
  answer: string
  sources: { articleId: number; title: string; relevance: number }[]
  conversationId: string
}

export function askKbQuestion(question: string, conversationId?: string) {
  return post<KbQa>('/kb/qa', { question, conversationId })
}