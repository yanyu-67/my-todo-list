//管理员用户管理
import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    registerByApi,
    testAdmin,
    uniqueName,
} from './helpers.js'

function userRow(page, username) {
    return page.locator('.el-table__row').filter({ hasText: username })
}

test.describe('管理员权限场景：用户管理', () => {
    test.beforeEach(async ({ page }) => {
        await clearBrowserState(page)
        await loginByUi(page, testAdmin)
        await page.goto('/admin/users')
    })

    test('管理员可以查看用户列表', async ({ page }) => {
        await expect(
            page.getByRole('heading', { name: '用户管理' }),
        ).toBeVisible()

        await expect(page.getByText('全部用户')).toBeVisible()
        await expect(page.getByText('正常使用')).toBeVisible()
        await expect(
            page.locator('.admin-stats').getByText('管理员', { exact: true }),
        ).toBeVisible()
    })

    test('可以搜索用户名', async ({ page, request }) => {
        const username = uniqueName('admin-search')

        await registerByApi(request, username, 'password123')
        await page.reload()

        await page.getByPlaceholder('搜索用户名').fill(username)

        await expect(userRow(page, username)).toBeVisible()
    })

    test('可以禁用并重新启用用户', async ({ page, request }) => {
        const username = uniqueName('admin-status')

        await registerByApi(request, username, 'password123')
        await page.reload()

        const row = userRow(page, username)

        await row.getByRole('button', { name: '禁用账号' }).click()

        const confirmDialog = page.getByRole('dialog', { name: '确认操作' })
        await confirmDialog
            .getByRole('button', { name: /^(确定|OK)$/ })
            .click()

        await expect(page.getByText('操作成功')).toBeVisible()
        await expect(userRow(page, username)).toContainText('已禁用')

        await userRow(page, username)
            .getByRole('button', { name: '启用账号' })
            .click()

        await page
            .getByRole('dialog', { name: '确认操作' })
            .getByRole('button', { name: /^(确定|OK)$/ })
            .click()

        await expect(userRow(page, username)).toContainText('已启用')
    })

    test('设置管理员角色后可以撤销管理员角色', async ({ page, request }) => {
        const username = uniqueName('admin-role')

        // 使用一次性账号，绝不修改默认管理员账号的角色。
        await registerByApi(request, username, 'password123')
        await page.reload()

        const row = userRow(page, username)

        await row.getByRole('button', { name: '设为管理员' }).click()

        const confirmDialog = page.getByRole('dialog', { name: '确认操作' })
        await confirmDialog
            .getByRole('button', { name: /^(确定|OK)$/ })
            .click()

        await expect(page.getByText('操作成功')).toBeVisible()
        await expect(userRow(page, username)).toContainText('管理员')

        await userRow(page, username)
            .getByRole('button', { name: '撤销管理员' })
            .click()

        await page
            .getByRole('dialog', { name: '确认操作' })
            .getByRole('button', { name: /^(确定|OK)$/ })
            .click()

        await expect(userRow(page, username)).toContainText('普通用户')
    })

    test('取消删除用户不会执行删除', async ({ page, request }) => {
        const username = uniqueName('admin-cancel-delete')

        await registerByApi(request, username, 'password123')
        await page.reload()

        const row = userRow(page, username)
        await row.getByRole('button', { name: '删除' }).click()

        const confirmDialog = page.getByRole('dialog', { name: '删除账号' })
        await confirmDialog.getByRole('button', { name: '取消' }).click()

        await expect(userRow(page, username)).toBeVisible()
    })

    test('确认后可以删除用户', async ({ page, request }) => {
        const username = uniqueName('admin-delete')

        await registerByApi(request, username, 'password123')
        await page.reload()

        await userRow(page, username)
            .getByRole('button', { name: '删除' })
            .click()

        const confirmDialog = page.getByRole('dialog', { name: '删除账号' })
        await confirmDialog.getByRole('button', { name: '确认删除' }).click()

        await expect(page.getByText('用户已删除')).toBeVisible()
        await expect(userRow(page, username)).toHaveCount(0)
    })
})
