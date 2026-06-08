import { describe, it, expect, vi, afterEach } from 'vitest'
import { formatDate, formatDateTime, fromNow, today, tomorrow, isFuture } from '@/utils/date'

describe('date 工具函数', () => {
  describe('formatDate', () => {
    it('ISO 日期字符串格式化为 YYYY-MM-DD', () => {
      expect(formatDate('2026-05-11')).toBe('2026-05-11')
    })

    it('ISO 带时间的字符串只保留日期部分', () => {
      expect(formatDate('2026-05-11T14:30:00')).toBe('2026-05-11')
    })
  })

  describe('formatDateTime', () => {
    it('ISO 日期时间格式化为 YYYY-MM-DD HH:mm', () => {
      expect(formatDateTime('2026-05-11T14:30:00')).toBe('2026-05-11 14:30')
    })

    it('不同日期的格式化', () => {
      expect(formatDateTime('2025-01-01T09:05:00')).toBe('2025-01-01 09:05')
    })
  })

  describe('fromNow', () => {
    afterEach(() => {
      vi.useRealTimers()
    })

    it('1 分钟内的时间显示「几秒前」或「刚刚」', () => {
      const now = new Date('2026-05-11T12:00:00').getTime()
      vi.useFakeTimers()
      vi.setSystemTime(now)

      const thirtySecsAgo = new Date(now - 30 * 1000).toISOString()
      const result = fromNow(thirtySecsAgo)
      // dayjs zh-cn: 几秒前 / 刚刚
      expect(result).toMatch(/秒|刚刚/)
    })

    it('1 小时前的时间显示「1 小时前」', () => {
      const now = new Date('2026-05-11T12:00:00').getTime()
      vi.useFakeTimers()
      vi.setSystemTime(now)

      const oneHourAgo = new Date(now - 60 * 60 * 1000).toISOString()
      const result = fromNow(oneHourAgo)
      expect(result).toContain('小时')
    })

    it('昨天的时间显示「1 天前」', () => {
      const now = new Date('2026-05-11T12:00:00').getTime()
      vi.useFakeTimers()
      vi.setSystemTime(now)

      const yesterday = new Date(now - 24 * 60 * 60 * 1000).toISOString()
      const result = fromNow(yesterday)
      expect(result).toMatch(/天/)
    })
  })

  describe('today', () => {
    it('返回今天的 YYYY-MM-DD 字符串', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-11T10:00:00'))
      expect(today()).toBe('2026-05-11')
      vi.useRealTimers()
    })
  })

  describe('tomorrow', () => {
    it('返回明天的 YYYY-MM-DD 字符串', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-11T10:00:00'))
      expect(tomorrow()).toBe('2026-05-12')
      vi.useRealTimers()
    })
  })

  describe('isFuture', () => {
    it('未来日期返回 true', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-11T10:00:00'))
      expect(isFuture('2026-05-12')).toBe(true)
      vi.useRealTimers()
    })

    it('今天返回 false', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-11T10:00:00'))
      expect(isFuture('2026-05-11')).toBe(false)
      vi.useRealTimers()
    })

    it('过去日期返回 false', () => {
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2026-05-11T10:00:00'))
      expect(isFuture('2026-05-10')).toBe(false)
      vi.useRealTimers()
    })
  })
})
