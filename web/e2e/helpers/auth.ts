import type { Page } from '@playwright/test'

export async function loginAs(page: Page, userNo: string, password = 'password') {
  await page.goto('/login')
  await page.getByLabel('学号 / 工号').fill(userNo)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await page.waitForURL(url => {
    const pathname = new URL(url).pathname
    return userNo.startsWith('A') ? pathname.startsWith('/admin') : pathname === '/'
  }, { timeout: 15000 })
}

export async function logout(page: Page) {
  // 桌面端：顶部用户菜单 → 退出
  await page.goto('/')
  const logoutBtn = page.getByRole('button', { name: /退出/ })
  if (await logoutBtn.isVisible()) {
    await logoutBtn.click()
  } else {
    // 清除 localStorage 代替 UI 退出
    await page.evaluate(() => {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    })
  }
}
