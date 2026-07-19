// 统一时间显示：24 小时制、零填充、精确到分钟，避免各页各自 toLocaleString 产生
// 「2026/7/15 上午1:40:00」这类冗长且不一致的时间串。
const DATE_TIME_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', DATE_TIME_OPTIONS)
}
