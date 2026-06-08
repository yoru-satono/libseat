import { test, expect } from '@playwright/test'
import { loginAs } from './helpers/auth'

test('管理员登录成功 → 跳转 /admin，侧边栏可见', async ({ page }) => {
  await loginAs(page, 'A000001')
  await expect(page).toHaveURL(/\/admin/)
  // 侧边栏包含「仪表盘」文字
  await expect(page.getByText('仪表盘').first()).toBeVisible()
})

test('普通用户登录成功 → 跳转 /，底部 Tab 可见', async ({ page }) => {
  await loginAs(page, 'S202401001')
  await expect(page).toHaveURL('/')
  // 移动端底部 Tab（viewport 默认为桌面，检查导航链接存在即可）
  await expect(page.getByRole('link', { name: /预约/ }).first()).toBeVisible()
})

test('密码错误 → 显示错误提示', async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('学号 / 工号').fill('S202401001')
  await page.getByLabel('密码').fill('wrongpassword')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByText(/密码错误|账号或密码|WRONG_CREDENTIALS/)).toBeVisible()
})

test('未登录访问 /reservations → 重定向 /login', async ({ page }) => {
  await page.goto('/reservations')
  await expect(page).toHaveURL(/\/login/)
})

test('未登录访问 /admin → 重定向 /login', async ({ page }) => {
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/login/)
})

test('普通用户访问 /admin → 重定向 /', async ({ page }) => {
  await loginAs(page, 'S202401001')
  await page.goto('/admin')
  await expect(page).toHaveURL('/')
})

test('已登录用户访问 /login → 重定向首页', async ({ page }) => {
  await loginAs(page, 'S202401001')
  await page.goto('/login')
  await expect(page).toHaveURL('/')
})
