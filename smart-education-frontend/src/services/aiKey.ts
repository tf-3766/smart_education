// 前端自带（BYO）大模型密钥与模型：仅存于当前浏览器 localStorage，随 AI 请求头发送，不上传保存。
const AI_KEY_STORAGE_KEY = 'smart-education-ai-key'
const AI_MODEL_STORAGE_KEY = 'smart-education-ai-model'
export const AI_KEY_HEADER = 'X-AI-Api-Key'
export const AI_MODEL_HEADER = 'X-AI-Model'

export function getAiKey(): string {
  return localStorage.getItem(AI_KEY_STORAGE_KEY)?.trim() ?? ''
}

export function setAiKey(key: string): void {
  const trimmed = key.trim()
  if (trimmed) localStorage.setItem(AI_KEY_STORAGE_KEY, trimmed)
  else localStorage.removeItem(AI_KEY_STORAGE_KEY)
}

export function clearAiKey(): void {
  localStorage.removeItem(AI_KEY_STORAGE_KEY)
}

export function getAiModel(): string {
  return localStorage.getItem(AI_MODEL_STORAGE_KEY)?.trim() ?? ''
}

export function setAiModel(model: string): void {
  const trimmed = model.trim()
  if (trimmed) localStorage.setItem(AI_MODEL_STORAGE_KEY, trimmed)
  else localStorage.removeItem(AI_MODEL_STORAGE_KEY)
}

/** 仅对 AI 接口路径附加密钥/模型请求头；其它请求不受影响。 */
export function aiKeyHeader(path: string): Record<string, string> {
  if (!path.startsWith('/api/v1/ai/')) return {}
  const headers: Record<string, string> = {}
  const key = getAiKey()
  const model = getAiModel()
  if (key) headers[AI_KEY_HEADER] = key
  if (model) headers[AI_MODEL_HEADER] = model
  return headers
}
