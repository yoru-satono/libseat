import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.locale('zh-cn')
dayjs.extend(relativeTime)

export function formatDate(date: string): string {
  return dayjs(date).format('YYYY-MM-DD')
}

export function formatDateTime(dt: string): string {
  return dayjs(dt).format('YYYY-MM-DD HH:mm')
}

export function fromNow(dt: string): string {
  return dayjs(dt).fromNow()
}

export function today(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function tomorrow(): string {
  return dayjs().add(1, 'day').format('YYYY-MM-DD')
}

export function isFuture(dateStr: string): boolean {
  return dayjs(dateStr).isAfter(dayjs(), 'day')
}
