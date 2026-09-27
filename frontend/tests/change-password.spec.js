//修改密码
import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    registerByApi,
    uniqueName,
} from './helpers.js'

test.describe('修改密码', () => {
    test('新密码少于 6 位时不能提交', async ({ page, request }) => {
        const username = uniqueName('change-password-short')
        const oldPassword = 'oldpass123'

        await registerByApi(request, username, oldPassword)
        await clearBrowserState(page)
        await loginByUi(page, { username, password: oldPassword })

        await page.getByRole('button', { name: '修改密码' }).click()

        await page.getByLabel('旧密码').fill(oldPassword)
        await page.getByLabel('新密码').fill('12345')
        await page.getByLabel('确认密码').fill('12345')
        await page.getByRole('button', { name: '保存并重新登录' }).click()

        await expect(page.getByText('新密码至少6位')).toBeVisible()
    })

    test('两次新密码不一致时不能提交', async ({ page, request }) => {
        const username = uniqueName('change-password-mismatch')
        const oldPassword = 'oldpass123'

        await registerByApi(request, username, oldPassword)
        await clearBrowserState(page)
        await loginByUi(page, { username, password: oldPassword })

        await page.getByRole('button', { name: '修改密码' }).click()

        await page.getByLabel('旧密码').fill(oldPassword)
        await page.getByLabel('新密码').fill('newpass123')
        await page.getByLabel('确认密码').fill('different123')
        await page.getByRole('button', { name: '保存并重新登录' }).click()

        await expect(page.getByText('两次密码不一致')).toBeVisible()
    })

    test('旧密码错误时显示错误且不跳转', async ({ page, request }) => {
        const username = uniqueName('change-password-wrong-old')
        const oldPassword = 'oldpass123'

        await registerByApi(request, username, oldPassword)
        await clearBrowserState(page)
        await loginByUi(page, { username, password: oldPassword })

        await page.getByRole('button', { name: '修改密码' }).click()

        await page.getByLabel('旧密码').fill('wrong-old-password')
        await page.getByLabel('新密码').fill('newpass123')
        await page.getByLabel('确认密码').fill('newpass123')
        await page.getByRole('button', { name: '保存并重新登录' }).click()

        await expect(page.getByText('旧密码错误')).toBeVisible()
        await expect(page).toHaveURL(/\/change-password/)
    })

    test('新密码与旧密码相同时显示错误且不跳转', async ({ page, request }) => {
        const username = uniqueName('change-password-same')
        const oldPassword = 'oldpass123'

        await registerByApi(request, username, oldPassword)
        await clearBrowserState(page)
        await loginByUi(page, { username, password: oldPassword })

        await page.getByRole('button', { name: '修改密码' }).click()

        await page.getByLabel('旧密码').fill(oldPassword)
        await page.getByLabel('新密码').fill(oldPassword)
        await page.getByLabel('确认密码').fill(oldPassword)
        await page.getByRole('button', { name: '保存并重新登录' }).click()

        await expect(
            page.getByText('新密码不能与旧密码相同'),
        ).toBeVisible()
        await expect(page).toHaveURL(/\/change-password/)
    })

    test('修改密码成功后需要重新登录', async ({ page, request }) => {
        const username = uniqueName('change-password-success')
        const oldPassword = 'oldpass123'
        const newPassword = 'newpass123'

        await registerByApi(request, username, oldPassword)
        await clearBrowserState(page)
        await loginByUi(page, { username, password: oldPassword })

        await page.getByRole('button', { name: '修改密码' }).click()

        await page.getByLabel('旧密码').fill(oldPassword)
        await page.getByLabel('新密码').fill(newPassword)
        await page.getByLabel('确认密码').fill(newPassword)
        await page.getByRole('button', { name: '保存并重新登录' }).click()

        await expect(page.getByText('密码已修改，请重新登录')).toBeVisible()
        await expect(page).toHaveURL(/\/login/)

        await page.getByLabel('用户名').fill(username)
        await page.getByLabel('密码').fill(newPassword)
        await page.getByRole('button', { name: '登录' }).click()

        await expect(page).toHaveURL(/\/todos/)
    })
})