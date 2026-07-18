import { describe, expect, it } from 'vitest'
import { formatDateTime } from '@/utils/datetime'

describe('formatDateTime', () => {
  it('输出统一为 YYYY/MM/DD HH:mm（24 小时、零填充、无秒）', () => {
    expect(formatDateTime('2026-07-15T09:05:30Z')).toMatch(/^\d{4}\/\d{2}\/\d{2} \d{2}:\d{2}$/)
  })

  it('空值显示破折号', () => {
    expect(formatDateTime(null)).toBe('—')
    expect(formatDateTime(undefined)).toBe('—')
    expect(formatDateTime('')).toBe('—')
  })

  it('非法日期原样返回，不抛错', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date')
  })
})
