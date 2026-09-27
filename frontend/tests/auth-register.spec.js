//用户注册
import { test, expect } from '@playwright/test'
import { registerByApi, uniqueName } from './helpers.js'

test.describe('用户注册', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/register')
    })

    test('用户名为空时不能注册', async ({ page }) => {
        await page.getByRole('button', { name: '注册' }).click()

        await expect(page.getByText('用户名不能为空')).toBeVisible()
    })

    test('密码少于 6 位时不能注册', async ({ page }) => {
        await page.getByLabel('用户名').fill(uniqueName('short'))
        await page.getByLabel('密码', { exact: true }).fill('12345')
        await page.getByLabel('确认密码').fill('12345')
        await page.getByRole('button', { name: '注册' }).click()

        await expect(page.getByText('密码至少6位')).toBeVisible()
    })

    test('两次密码不一致时不能注册', async ({ page }) => {
        await page.getByLabel('用户名').fill(uniqueName('mismatch'))
        await page.getByLabel('密码', { exact: true }).fill('password123')
        await page.getByLabel('确认密码').fill('password456')
        await page.getByRole('button', { name: '注册' }).click()

        await expect(page.getByText('两次密码不一致')).toBeVisible()
    })

    test('注册成功后跳转登录页', async ({ page }) => {
        const username = uniqueName('register')

        await page.getByLabel('用户名').fill(username)
        await page.getByLabel('密码', { exact: true }).fill('password123')
        await page.getByLabel('确认密码').fill('password123')
        await page.getByRole('button', { name: '注册' }).click()

        await expect(page.getByText('注册成功，请登录')).toBeVisible()
        await expect(page).toHaveURL(/\/login/)
    })

    test('重复用户名注册失败', async ({ page, request }) => {
        const username = uniqueName('duplicate')

        await registerByApi(request, username, 'password123')

        await page.getByLabel('用户名').fill(username)
        await page.getByLabel('密码', { exact: true }).fill('password123')
        await page.getByLabel('确认密码').fill('password123')
        await page.getByRole('button', { name: '注册' }).click()

        await expect(page.getByText('用户名已被使用')).toBeVisible()
        await expect(page).toHaveURL(/\/register/)
    })
})
