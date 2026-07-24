export interface PaperScoreSuggestion {
  questionId: string
  score: number
}

/** 解析后端组卷建议中的结构化 questionId/建议分值，ID 始终按字符串处理以保留雪花 ID 精度。 */
export function parsePaperScoreSuggestions(content: string): PaperScoreSuggestion[] {
  const suggestions = new Map<string, number>()
  const pattern = /questionId\s*[=：:]\s*([0-9]+)[^\r\n]{0,240}?建议分值\s*[=：:]\s*([0-9]+(?:\.[0-9]+)?)/giu
  for (const match of content.matchAll(pattern)) {
    const score = Number(match[2])
    if (Number.isFinite(score) && score > 0) suggestions.set(match[1], score)
  }
  return [...suggestions].map(([questionId, score]) => ({ questionId, score }))
}
