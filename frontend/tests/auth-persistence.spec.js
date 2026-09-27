//登录态持久化
import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    testUser,
} from './helpers.js'

test('登录态刷新后仍然存在', async ({ page }) => {
    await clearBrowserState(page)
    await loginByUi(page, testUser)

    await expect(page).toHaveURL(/\/todos/)

    const beforeReload = await page.evaluate(() => ({
        token: localStorage.getItem('todo_token'),
        username: localStorage.getItem('todo_username'),
        role: localStorage.getItem('todo_role'),
    }))

    expect(beforeReload.token).toBeTruthy()
    expect(beforeReload.username).toBe(testUser.username)
    expect(beforeReload.role).toBeTruthy()

    await page.reload()

    await expect(page).toHaveURL(/\/todos/)
    await expect(
        page.getByRole('heading', { name: '我的任务' }),
    ).toBeVisible()
})

test('清除登录态后访问 Todo 会跳转登录页', async ({ page }) => {
    await clearBrowserState(page)
    await loginByUi(page, testUser)

    await page.evaluate(() => {
        localStorage.removeItem('todo_token')
        localStorage.removeItem('todo_username')
        localStorage.removeItem('todo_role')
    })

    await page.goto('/todos')

    await expect(page).toHaveURL(/\/login/)
})