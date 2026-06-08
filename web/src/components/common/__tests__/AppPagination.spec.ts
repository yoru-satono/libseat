import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AppPagination from '@/components/common/AppPagination.vue'

function factory(page: number, totalPages: number, total = 100, pageSize = 10) {
  return mount(AppPagination, { props: { page, totalPages, total, pageSize } })
}

describe('AppPagination', () => {
  describe('箭头按钮禁用状态', () => {
    it('首页时上一页按钮禁用', () => {
      const wrapper = factory(1, 5)
      const buttons = wrapper.findAll('button')
      expect(buttons[0].attributes('disabled')).toBeDefined()
    })

    it('末页时下一页按钮禁用', () => {
      const wrapper = factory(5, 5)
      const buttons = wrapper.findAll('button')
      expect(buttons[buttons.length - 1].attributes('disabled')).toBeDefined()
    })

    it('中间页时两侧箭头均可点击', () => {
      const wrapper = factory(3, 5)
      const buttons = wrapper.findAll('button')
      expect(buttons[0].attributes('disabled')).toBeUndefined()
      expect(buttons[buttons.length - 1].attributes('disabled')).toBeUndefined()
    })
  })

  describe('省略号显示', () => {
    it('当前页居中且总页数 ≤ 5 时所有页都在 delta 范围内，不显示省略号', () => {
      // page=3, totalPages=5: delta=2 → pages=[1,2,3,4,5]，首尾均在范围内
      const wrapper = factory(3, 5)
      expect(wrapper.text()).not.toContain('…')
    })

    it('当前页靠前且总页数较多时，右侧显示省略号', () => {
      const wrapper = factory(1, 10)
      expect(wrapper.text()).toContain('…')
    })

    it('当前页靠后且总页数较多时，左侧显示省略号', () => {
      const wrapper = factory(10, 10)
      expect(wrapper.text()).toContain('…')
    })

    it('当前页在中间时，两侧显示省略号', () => {
      const wrapper = factory(5, 10)
      const text = wrapper.text()
      // 至少包含一个省略号
      expect(text).toContain('…')
    })
  })

  describe('页码渲染', () => {
    it('始终显示第 1 页和最后一页的按钮（当页数较多时）', () => {
      const wrapper = factory(5, 10)
      const text = wrapper.text()
      expect(text).toContain('1')
      expect(text).toContain('10')
    })

    it('当前页的按钮带高亮样式', () => {
      const wrapper = factory(3, 5)
      const buttons = wrapper.findAll('button')
      // 找到显示当前页码 "3" 的按钮
      const activeBtn = buttons.find(b => b.text() === '3')
      expect(activeBtn?.classes()).toContain('bg-primary-600')
    })

    it('总数显示正确', () => {
      const wrapper = factory(1, 5, 42, 10)
      expect(wrapper.text()).toContain('42')
    })
  })

  describe('点击交互', () => {
    it('点击页码按钮触发 update:page emit，携带正确页码', async () => {
      const wrapper = factory(1, 5)
      const buttons = wrapper.findAll('button')
      const pageBtn = buttons.find(b => b.text() === '2')
      await pageBtn?.trigger('click')
      expect(wrapper.emitted('update:page')).toBeTruthy()
      expect(wrapper.emitted('update:page')?.[0]).toEqual([2])
    })

    it('点击下一页触发 update:page，页码 +1', async () => {
      const wrapper = factory(2, 5)
      const buttons = wrapper.findAll('button')
      const nextBtn = buttons[buttons.length - 1]
      await nextBtn.trigger('click')
      expect(wrapper.emitted('update:page')?.[0]).toEqual([3])
    })

    it('点击上一页触发 update:page，页码 -1', async () => {
      const wrapper = factory(3, 5)
      const buttons = wrapper.findAll('button')
      const prevBtn = buttons[0]
      await prevBtn.trigger('click')
      expect(wrapper.emitted('update:page')?.[0]).toEqual([2])
    })
  })
})
