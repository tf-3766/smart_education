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

/** 把 API 的绝对时间转换为当前浏览器时区的 datetime-local 值，避免直接 slice 导致 8 小时时差。 */
export function toDateTimeLocalValue(value?: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 16)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
