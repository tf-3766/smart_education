/** 将模型返回的轻量 Markdown 清理为适合业务界面直接阅读的纯文本。 */
export function cleanAiText(value?: string | null): string {
  if (!value) return ''
  return value
    .replace(/\*\*|__/g, '')
    .replace(/`{1,3}/g, '')
    .replace(/^\s*#{1,6}\s+/gm, '')
    .replace(/^\s*[-*_]{3,}\s*$/gm, '')
    .replace(/^\s*[-*]\s+/gm, '• ')
    .replace(/[✅📚📎📌🎯🔍💡]/gu, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}