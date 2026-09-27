//登录、退出和路由权限
import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    testAdmin,
    testUser,
} from './helpers.js'

test.describe('登录与路由权限', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/login')
        await clearBrowserState(page)
    })

    test('未登录访问 Todo 页面会跳转登录页', async ({ page }) => {
        await page.goto('/todos')

        await expect(page).toHaveURL((url) => {
            return (
                url.pathname === '/login' &&
                url.searchParams.get('redirect') === '/todos'
            )
        })
        await expect(
            page.getByRole('heading', { name: '欢迎回来' }),
        ).toBeVisible()
    })

    test('使用正确账号登录成功', async ({ page }) => {
        await page.getByLabel('用户名').fill(testUser.username)
        await page.getByLabel('密码').fill(testUser.password)
        await page.getByRole('button', { name: '登录' }).click()

        await expect(page).toHaveURL(/\/todos/)
        await expect(
            page.getByRole('heading', { name: '我的任务' }),
        ).toBeVisible()

        await expect
            .poll(() =>
                page.evaluate(() => ({
                    token: localStorage.getItem('todo_token'),
                    username: localStorage.getItem('todo_username'),
                    role: localStorage.getItem('todo_role'),
                })),
            )
            .toEqual({
                token: expect.any(String),
                username: testUser.username,
                role: expect.any(String),
            })
    })

    test('错误账号密码登录失败', async ({ page }) => {
        await page.getByLabel('用户名').fill(testUser.username)
        await page.getByLabel('密码').fill('wrong-password')
        await page.getByRole('button', { name: '登录' }).click()

        await expect(page.getByText('用户名或密码错误')).toBeVisible()
        await expect(page).toHaveURL(/\/login/)
    })

    test('空表单不能提交', async ({ page }) => {
        await page.getByRole('button', { name: '登录' }).click()

        await expect(page.getByText('请输入用户名和密码')).toBeVisible()
    })

    test('退出登录后清除登录态', async ({ page }) => {
        await loginByUi(page, testUser)

        await page.getByRole('button', { name: '退出登录' }).click()

        await expect(page).toHaveURL(/\/login/)
        await expect
            .poll(() =>
                page.evaluate(() => ({
                    token: localStorage.getItem('todo_token'),
                    username: localStorage.getItem('todo_username'),
                    role: localStorage.getItem('todo_role'),
                })),
            )
            .toEqual({
                token: null,
                username: null,
                role: null,
            })
    })

    test('普通用户不能访问管理员页面', async ({ page }) => {
        await loginByUi(page, testUser)
        await page.goto('/admin/users')

        await expect(page).toHaveURL(/\/todos/)
        await expect(
            page.getByRole('heading', { name: '我的任务' }),
        ).toBeVisible()
    })

    test('管理员可以访问用户管理页面', async ({ page }) => {
        await loginByUi(page, testAdmin)
        await page.goto('/admin/users')

        await expect(page).toHaveURL(/\/admin\/users/)
        await expect(
            page.getByRole('heading', { name: '用户管理' }),
        ).toBeVisible()
    })

    test('失效 token 返回 401 后跳转登录页', async ({ page }) => {
        await loginByUi(page, testUser)

        await page.evaluate(() => {
            localStorage.setItem('todo_token', 'invalid-token')
        })

        await page.goto('/todos')

        await expect(page).toHaveURL(/\/login\?reason=expired/)
        await expect
            .poll(() =>
                page.evaluate(() => ({
                    token: localStorage.getItem('todo_token'),
                    username: localStorage.getItem('todo_username'),
                    role: localStorage.getItem('todo_role'),
                })),
            )
            .toEqual({
                token: null,
                username: null,
                role: null,
            })
    })
})
