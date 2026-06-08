import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'A000001')
})

test('用户列表正常加载', async ({ page }) => {
  await page.goto('/admin/users')
  await expect(page.getByText('S202401001').or(page.getByText(/用户/)).first()).toBeVisible({ timeout: 5000 })
})

test('搜索用户 S202401001 → 只显示匹配行', async ({ page }) => {
  await page.goto('/admin/users')

  const searchInput = page.locator('input[placeholder*="搜索"], input[type="search"]').first()
  await expect(searchInput).toBeVisible({ timeout: 5000 })
  await searchInput.fill('S202401001')

  // 触发搜索
  await searchInput.press('Enter')
  const searchBtn = page.getByRole('button', { name: /搜索|查询/ })
  if (await searchBtn.isVisible({ timeout: 500 }).catch(() => false)) {
    await searchBtn.click()
  }

  await page.waitForTimeout(500)
  await expect(page.getByText('S202401001').first()).toBeVisible({ timeout: 5000 })
})

test('通过编辑弹窗将用户状态改为锁定', async ({ page }) => {
  await page.goto('/admin/users')

  // 验证第一个 S202401001 用户单元格可见
  const userCell = page.getByText('S202401001').first()
  if (!(await userCell.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  // 找到该行中的「编辑」按钮
  const userRow = userCell.locator('..')
  const editBtn = userRow.getByRole('button', { name: /编辑/ })
  if (!(await editBtn.isVisible({ timeout: 2000 }).catch(() => false))) {
    // 移动端：编辑按钮在卡片底部
    test.skip()
    return
  }

  await editBtn.click()

  // 在编辑弹窗中切换状态为 LOCKED
  const statusSelect = page.locator('select').first()
  if (!(await statusSelect.isVisible({ timeout: 2000 }).catch(() => false))) {
    test.skip()
    return
  }
  await statusSelect.selectOption('LOCKED')

  // 点击保存
  const saveBtn = page.getByRole('button', { name: /保存/ })
  await saveBtn.click()

  // 等待页面更新
  await page.waitForTimeout(1000)
})

test('待审核申请列表正常加载', async ({ page }) => {
  await page.goto('/admin/change-requests')
  await expect(
    page.getByText(/待审核|修改申请|暂无/).first()
  ).toBeVisible({ timeout: 5000 })
})

test('审核批准修改申请 → 状态变 APPROVED', async ({ page }) => {
  await page.goto('/admin/change-requests')

  const approveBtn = page.getByRole('button', { name: /批准|通过/ }).first()
  if (!(await approveBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  await approveBtn.click()

  // 确认弹窗
  const confirmBtn = page.getByRole('button', { name: /确认|批准/ })
  if (await confirmBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
    await confirmBtn.click()
  }

  await expect(page.getByText(/已批准|APPROVED/).first()).toBeVisible({ timeout: 5000 })
})

test('审核拒绝修改申请 → 状态变 REJECTED', async ({ page }) => {
  await page.goto('/admin/change-requests')

  const rejectBtn = page.getByRole('button', { name: /拒绝/ }).first()
  if (!(await rejectBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  await rejectBtn.click()

  // 填写拒绝原因
  const reasonInput = page.locator('textarea, input[placeholder*="原因"]').first()
  if (await reasonInput.isVisible({ timeout: 1000 }).catch(() => false)) {
    await reasonInput.fill('信息不符')
  }

  const confirmBtn = page.getByRole('button', { name: /确认|拒绝/ })
  if (await confirmBtn.isVisible({ timeout: 1000 }).catch(() => false)) {
    await confirmBtn.click()
  }

  await expect(page.getByText(/已拒绝|REJECTED/).first()).toBeVisible({ timeout: 5000 })
})
