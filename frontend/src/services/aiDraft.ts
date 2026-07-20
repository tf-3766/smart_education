import type { AiDraftVO } from '@/services/api/types'
import type { AiResult } from '@/types/domain'

/** 把后端 AI 草稿 VO 映射为界面展示用的 AiResult（不含置信度，由后端语义决定是否有）。 */
export function aiDraftToResult(draft: AiDraftVO, type: AiResult['type'], title: string): AiResult {
  return {
    id: draft.requestId,
    type,
    title,
    content: draft.content,
    citations: draft.citations.map((c) => ({ source: c.resourceType, title: c.title })),
    confirmed: false,
  }
}
