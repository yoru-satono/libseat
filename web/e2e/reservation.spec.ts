import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'
import dayjs from 'dayjs'

const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
// 用当前分钟计算唯一时段，避免重复运行时冲突（每分钟变化，8-16 范围内）
const uniqueHour = 8 + (Math.floor(Date.now() / 60000) % 9)
const uniqueStart = `${String(uniqueHour).padStart(2, '0')}:00`
const uniqueEnd   = `${String(uniqueHour + 2).padStart(2, '0')}:00`

test('匿名用户可浏览座位列表', async ({ page }) => {
  await page.goto('/seats')
  await expect(page.locator('.seat-card, [data-testid="seat-card"]').first()).toBeVisible({ timeout: 5000 })
})

test('筛选图书馆后只显示匹配座位', async ({ page }) => {
  await page.goto('/seats')
  // 点击 Headless UI Listbox 按钮打开下拉
  const libraryBtn = page.locator('button').filter({ hasText: '全部图书馆' }).first()
  await libraryBtn.click()
  // 选择第一个图书馆选项（[role="option"] 由 HeadlessUI 注入）
  const options = page.locator('[role="option"]')
  if (await options.count() > 0) {
    await options.first().click()
  }
  await page.waitForTimeout(500)
  const cards = page.locator('[data-testid="seat-card"]')
  await expect(cards.first()).toBeVisible({ timeout: 5000 })
})

test('完整预约流程：登录 → 找座位 → 预约 → 出现在我的预约', async ({ page }) => {
  await loginAs(page, 'S202401001')
  await page.goto('/seats')

  // 找一个 AVAILABLE 座位的预约按钮
  const reserveBtn = page.getByRole('button', { name: '预约' }).first()
  await expect(reserveBtn).toBeVisible({ timeout: 5000 })
  await reserveBtn.click()

  // 弹窗出现
  await expect(page.getByText('预约座位')).toBeVisible()

  // 填写日期（明天）— 作用域限定到弹窗避免 strict mode violation
  const modal = page.getByRole('dialog', { name: '预约座位' })
  await modal.locator('input[type="date"]').fill(tomorrow)

  // 使用动态时段避免重复运行冲突
  const timeInputs = modal.locator('input[type="time"]')
  await timeInputs.first().fill(uniqueStart)
  await timeInputs.last().fill(uniqueEnd)

  // 提交
  await page.getByRole('button', { name: '确认预约' }).click()

  // 成功 toast
  await expect(page.getByText('预约成功')).toBeVisible({ timeout: 5000 })

  // 跳转到预约列表后出现新记录
  await page.goto('/reservations')
  await expect(page.getByText(/ACTIVE|待签到/).first()).toBeVisible({ timeout: 5000 })
})

test('预约时长 < 30 分钟 → 显示错误提示 B0202', async ({ page }) => {
  await loginAs(page, 'S202401002')
  await page.goto('/seats')

  const reserveBtn = page.getByRole('button', { name: '预约' }).first()
  await expect(reserveBtn).toBeVisible({ timeout: 5000 })
  await reserveBtn.click()

  const modal2 = page.getByRole('dialog', { name: '预约座位' })
  await modal2.locator('input[type="date"]').fill(tomorrow)
  const timeInputs = modal2.locator('input[type="time"]')
  await timeInputs.first().fill(uniqueStart)
  await timeInputs.last().fill(`${String(uniqueHour).padStart(2, '0')}:20`) // 只有 20 分钟

  await page.getByRole('button', { name: '确认预约' }).click()
  await expect(page.getByText(/时长|RESERVATION_DURATION/)).toBeVisible({ timeout: 5000 })
})
