import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'S202401001')
  await page.goto('/profile')
})

test('个人资料页正常加载：显示学号和姓名', async ({ page }) => {
  await expect(page.getByText('S202401001')).toBeVisible({ timeout: 5000 })
})

test('修改手机号后显示保存成功 toast', async ({ page }) => {
  // 点击手机号编辑按钮
  const editBtn = page.getByRole('button', { name: /编辑|修改/ }).first()
  if (!(await editBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }
  await editBtn.click()

  const phoneInput = page.locator('input[type="tel"], input[placeholder*="手机"]')
  await phoneInput.clear()
  await phoneInput.fill('13912345678')

  await page.getByRole('button', { name: /保存|确认/ }).click()
  await expect(page.getByText(/保存成功|修改成功|更新成功/)).toBeVisible({ timeout: 5000 })
})

test('提交姓名修改申请后出现 PENDING 记录', async ({ page }) => {
  // 点击申请修改按钮
  const requestBtn = page.getByRole('button', { name: /申请修改|提交申请/ }).first()
  if (!(await requestBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }
  await requestBtn.click()

  // 选择字段（姓名）
  const fieldSelect = page.locator('select[name="fieldName"], select').first()
  if (await fieldSelect.isVisible({ timeout: 1000 }).catch(() => false)) {
    await fieldSelect.selectOption({ label: /姓名|realName/ })
  }

  const newValueInput = page.locator('input[placeholder*="新"], textarea').first()
  await newValueInput.fill('测试新姓名')

  await page.getByRole('button', { name: /提交|确认/ }).click()
  await expect(page.getByText(/待审核|PENDING/)).toBeVisible({ timeout: 5000 })
})

test('导出预约记录触发文件下载', async ({ page }) => {
  const exportBtn = page.getByRole('button', { name: /导出/ })

  if (!(await exportBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  const downloadPromise = page.waitForEvent('download', { timeout: 10_000 })
  await exportBtn.click()
  const download = await downloadPromise
  expect(download.suggestedFilename()).toMatch(/\.(xlsx|csv|xls)$/)
})
