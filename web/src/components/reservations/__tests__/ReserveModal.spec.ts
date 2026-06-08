import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ReserveModal from '@/components/reservations/ReserveModal.vue'
import type { Seat } from '@/types/api'
import { ApiError } from '@/api/http'

vi.mock('@/api/reservations', () => ({
  reservationsApi: {
    create: vi.fn(),
  },
}))

// Stub AppModal 以绕过 Headless UI Dialog 的 DOM 依赖
vi.mock('@/components/common/AppModal.vue', () => ({
  default: {
    name: 'AppModal',
    template: '<div v-if="open"><slot /><slot name="footer" /></div>',
    props: ['open', 'title'],
    emits: ['close'],
  },
}))

// Stub AppButton
vi.mock('@/components/common/AppButton.vue', () => ({
  default: {
    name: 'AppButton',
    template: '<button :disabled="disabled || loading" :data-loading="loading"><svg v-if="loading" data-testid="spinner" class="animate-spin" /><slot /></button>',
    props: ['variant', 'size', 'loading', 'disabled', 'fullWidth', 'type'],
  },
}))

// Stub useToast
const toastSuccess = vi.fn()
const toastError = vi.fn()
vi.mock('vue-toastification', () => ({
  useToast: () => ({ success: toastSuccess, error: toastError }),
}))

const { reservationsApi } = await import('@/api/reservations')

const mockSeat: Seat = {
  id: 'seat-1',
  libraryId: 'lib-1',
  libraryName: '中心图书馆',
  seatNo: '1F-A-001',
  floor: 1,
  area: 'QUIET',
  hasComputer: false,
  hasPower: true,
  hasWindow: false,
  status: 'AVAILABLE',
  posX: 0,
  posY: 0,
}

describe('ReserveModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('seat 为 null 时不渲染内容', () => {
    const wrapper = mount(ReserveModal, { props: { seat: null } })
    expect(wrapper.find('input[type="date"]').exists()).toBe(false)
  })

  it('seat 有值时渲染座位信息和表单', () => {
    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    expect(wrapper.text()).toContain('1F-A-001')
    expect(wrapper.text()).toContain('中心图书馆')
    expect(wrapper.find('input[type="date"]').exists()).toBe(true)
  })

  it('未填写日期时确认按钮禁用', () => {
    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    const submitBtn = wrapper.findAll('button').find(b => b.text().includes('确认预约'))
    expect(submitBtn?.attributes('disabled')).toBeDefined()
  })

  it('填写日期后确认按钮可用', async () => {
    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.find('input[type="date"]').setValue('2026-12-01')
    const submitBtn = wrapper.findAll('button').find(b => b.text().includes('确认预约'))
    expect(submitBtn?.attributes('disabled')).toBeUndefined()
  })

  it('提交成功：调用 reservationsApi.create 并触发 success + close emit', async () => {
    vi.mocked(reservationsApi.create).mockResolvedValueOnce({} as any)

    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.find('input[type="date"]').setValue('2026-12-01')
    await wrapper.findAll('button').find(b => b.text().includes('确认预约'))?.trigger('click')
    await flushPromises()

    expect(reservationsApi.create).toHaveBeenCalledWith({
      seatId: 'seat-1',
      date: '2026-12-01',
      startTime: '09:00',
      endTime: '11:00',
    })
    expect(toastSuccess).toHaveBeenCalledWith('预约成功！')
    expect(wrapper.emitted('success')).toBeTruthy()
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('提交失败 B0202：显示错误信息，不触发 success emit', async () => {
    vi.mocked(reservationsApi.create).mockRejectedValueOnce(
      new ApiError('B0202', '单次预约时长不符合规则')
    )

    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.find('input[type="date"]').setValue('2026-12-01')
    await wrapper.findAll('button').find(b => b.text().includes('确认预约'))?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('单次预约时长不符合规则')
    expect(wrapper.emitted('success')).toBeFalsy()
  })

  it('提交失败（无 message）：显示默认错误文案', async () => {
    vi.mocked(reservationsApi.create).mockRejectedValueOnce(new Error())

    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.find('input[type="date"]').setValue('2026-12-01')
    await wrapper.findAll('button').find(b => b.text().includes('确认预约'))?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('预约失败，请重试')
  })

  it('提交过程中按钮显示 loading 状态（spinner + disabled）', async () => {
    let resolveCreate!: () => void
    vi.mocked(reservationsApi.create).mockReturnValueOnce(
      new Promise(resolve => { resolveCreate = () => resolve({} as any) })
    )

    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.find('input[type="date"]').setValue('2026-12-01')
    wrapper.findAll('button').find(b => b.text().includes('确认预约'))?.trigger('click')

    await wrapper.vm.$nextTick()
    const submitBtn = wrapper.findAll('button').find(b => b.text().includes('确认预约'))
    expect(submitBtn).toBeTruthy()
    // AppButton 在 loading 状态下应该 disabled 且显示 spinner
    expect(submitBtn?.attributes('disabled')).toBeDefined()
    expect(submitBtn?.attributes('data-loading')).toBe('true')
    expect(submitBtn?.find('[data-testid="spinner"]').exists()).toBe(true)

    resolveCreate()
    await flushPromises()
  })

  it('点击取消按钮触发 close emit', async () => {
    const wrapper = mount(ReserveModal, { props: { seat: mockSeat } })
    await wrapper.findAll('button').find(b => b.text() === '取消')?.trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })
})
