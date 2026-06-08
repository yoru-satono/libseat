import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppBadge from '@/components/common/AppBadge.vue'

describe('AppBadge', () => {
  it('ACTIVE → 「待签到」，带 primary 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'ACTIVE' } })
    expect(wrapper.text()).toBe('待签到')
    expect(wrapper.find('span').classes()).toContain('bg-primary-50')
    expect(wrapper.find('span').classes()).toContain('text-primary-700')
  })

  it('CHECKED_IN → 「已签到」，带 success 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'CHECKED_IN' } })
    expect(wrapper.text()).toBe('已签到')
    expect(wrapper.find('span').classes()).toContain('bg-success-50')
    expect(wrapper.find('span').classes()).toContain('text-success-700')
  })

  it('IN_USE → 「使用中」，带 accent 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'IN_USE' } })
    expect(wrapper.text()).toBe('使用中')
    expect(wrapper.find('span').classes()).toContain('bg-accent-50')
    expect(wrapper.find('span').classes()).toContain('text-accent-700')
  })

  it('COMPLETED → 「已完成」，带 surface 灰色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'COMPLETED' } })
    expect(wrapper.text()).toBe('已完成')
    expect(wrapper.find('span').classes()).toContain('bg-surface-100')
    expect(wrapper.find('span').classes()).toContain('text-surface-600')
  })

  it('CANCELLED → 「已取消」，带 danger 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'CANCELLED' } })
    expect(wrapper.text()).toBe('已取消')
    expect(wrapper.find('span').classes()).toContain('bg-danger-50')
    expect(wrapper.find('span').classes()).toContain('text-danger-600')
  })

  it('NO_SHOW → 「爽约」，带 warning 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'NO_SHOW' } })
    expect(wrapper.text()).toBe('爽约')
    expect(wrapper.find('span').classes()).toContain('bg-warning-50')
    expect(wrapper.find('span').classes()).toContain('text-warning-700')
  })

  it('AVAILABLE → 「可预约」，带 success 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'AVAILABLE' } })
    expect(wrapper.text()).toBe('可预约')
    expect(wrapper.find('span').classes()).toContain('bg-success-50')
    expect(wrapper.find('span').classes()).toContain('text-success-700')
  })

  it('UNAVAILABLE → 「不可用」', () => {
    const wrapper = mount(AppBadge, { props: { status: 'UNAVAILABLE' } })
    expect(wrapper.text()).toBe('不可用')
    expect(wrapper.find('span').classes()).toContain('bg-surface-100')
    expect(wrapper.find('span').classes()).toContain('text-surface-500')
  })

  it('WAITING → 「等待中」，带 warning 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'WAITING' } })
    expect(wrapper.text()).toBe('等待中')
    expect(wrapper.find('span').classes()).toContain('bg-warning-50')
    expect(wrapper.find('span').classes()).toContain('text-warning-700')
  })

  it('PENDING → 「待审核」', () => {
    const wrapper = mount(AppBadge, { props: { status: 'PENDING' } })
    expect(wrapper.text()).toBe('待审核')
    expect(wrapper.find('span').classes()).toContain('bg-warning-50')
    expect(wrapper.find('span').classes()).toContain('text-warning-700')
  })

  it('APPROVED → 「已批准」，带 success 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'APPROVED' } })
    expect(wrapper.text()).toBe('已批准')
    expect(wrapper.find('span').classes()).toContain('bg-success-50')
    expect(wrapper.find('span').classes()).toContain('text-success-700')
  })

  it('REJECTED → 「已拒绝」', () => {
    const wrapper = mount(AppBadge, { props: { status: 'REJECTED' } })
    expect(wrapper.text()).toBe('已拒绝')
    expect(wrapper.find('span').classes()).toContain('bg-danger-50')
    expect(wrapper.find('span').classes()).toContain('text-danger-600')
  })

  it('LOCKED → 「已锁定」，带 danger 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'LOCKED' } })
    expect(wrapper.text()).toBe('已锁定')
    expect(wrapper.find('span').classes()).toContain('bg-danger-50')
    expect(wrapper.find('span').classes()).toContain('text-danger-600')
  })

  it('SUSPENDED → 「已暂停」，带 warning 色样式', () => {
    const wrapper = mount(AppBadge, { props: { status: 'SUSPENDED' } })
    expect(wrapper.text()).toBe('已暂停')
    expect(wrapper.find('span').classes()).toContain('bg-warning-50')
    expect(wrapper.find('span').classes()).toContain('text-warning-700')
  })

  it('INACTIVE → 「未激活」', () => {
    const wrapper = mount(AppBadge, { props: { status: 'INACTIVE' } })
    expect(wrapper.text()).toBe('未激活')
    expect(wrapper.find('span').classes()).toContain('bg-surface-100')
    expect(wrapper.find('span').classes()).toContain('text-surface-500')
  })

  it('未知 status → 原样显示，不抛错', () => {
    const wrapper = mount(AppBadge, { props: { status: 'UNKNOWN_STATUS' as any } })
    expect(wrapper.text()).toBe('UNKNOWN_STATUS')
    expect(wrapper.find('span').classes()).toContain('bg-surface-100')
    expect(wrapper.find('span').classes()).toContain('text-surface-600')
  })
})
