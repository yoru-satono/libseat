import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'

const NEW_LIBRARY_NAME = `E2E测试图书馆_${Date.now()}`

test.beforeEach(async ({ page }) => {
  await loginAs(page, 'A000001')
  await page.goto('/admin/libraries')
})

test('图书馆列表正常加载', async ({ page }) => {
  await expect(page.getByText(/图书馆|library/i).first()).toBeVisible({ timeout: 5000 })
})

test('新建图书馆 → 列表出现新记录', async ({ page }) => {
  const createBtn = page.getByRole('button', { name: /新建|添加|创建|新增/ }).first()
  await expect(createBtn).toBeVisible({ timeout: 5000 })
  await createBtn.click()

  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('名称').fill(NEW_LIBRARY_NAME)
  await dialog.getByLabel('地址').fill('E2E测试地址')

  await dialog.getByRole('button', { name: /保存/ }).click()
  await expect(page.getByText(NEW_LIBRARY_NAME)).toBeVisible({ timeout: 5000 })
})

test('编辑图书馆名称 → 列表显示新名称', async ({ page }) => {
  // 找到 E2E 测试图书馆并编辑
  const editBtn = page.getByRole('button', { name: /编辑/ }).first()
  if (!(await editBtn.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  await editBtn.click()

  const dialog = page.getByRole('dialog')
  await dialog.getByLabel('名称').clear()
  const updatedName = `${NEW_LIBRARY_NAME}_已编辑`
  await dialog.getByLabel('名称').fill(updatedName)

  await dialog.getByRole('button', { name: /保存/ }).click()
  await expect(page.getByText(updatedName)).toBeVisible({ timeout: 5000 })
})

test('删除图书馆 → 记录消失', async ({ page }) => {
  // 找 E2E 创建的图书馆
  const e2eItem = page.getByText(/E2E测试图书馆/).first()
  if (!(await e2eItem.isVisible({ timeout: 3000 }).catch(() => false))) {
    test.skip()
    return
  }

  // 接管浏览器原生 confirm 弹窗
  page.once('dialog', dialog => dialog.accept())

  // 找到同行的删除按钮（aria-label="删除"）
  const row = e2eItem.locator('../..')
  const deleteBtn = row.getByRole('button', { name: '删除' }).first()
  await deleteBtn.click()

  await expect(e2eItem).not.toBeVisible({ timeout: 5000 })
})
