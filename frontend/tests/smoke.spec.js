import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    testUser,
    uniqueName,
} from './helpers.js'

function todoCard(page, title) {
    return page.locator('.todo-card').filter({ hasText: title })
}

test.beforeEach(async ({ page }) => {
    await clearBrowserState(page)
    await loginByUi(page, testUser)
})

test('登录后可以看到 Todo 页面', async ({ page }) => {
    await expect(page).toHaveURL(/\/todos/)
    await expect(
        page.getByRole('heading', { name: '我的任务' }),
    ).toBeVisible()
})

test('可以新增 Todo', async ({ page }) => {
    const title = uniqueName('smoke-add')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByRole('button', { name: '保存' }).click()

    await expect(todoCard(page, title)).toBeVisible()
})

test('可以切换 Todo 完成状态', async ({ page }) => {
    const title = uniqueName('smoke-complete')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByRole('button', { name: '保存' }).click()

    const card = todoCard(page, title)
    await card.locator('.el-checkbox').click()

    await expect(card).toContainText('已完成')
})

test('可以搜索 Todo', async ({ page }) => {
    const title = uniqueName('smoke-search')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByRole('button', { name: '保存' }).click()

    await page.getByPlaceholder('搜索标题或描述').fill(title)

    await expect(todoCard(page, title)).toBeVisible()
})

test('刷新后 Todo 仍然存在', async ({ page }) => {
    const title = uniqueName('smoke-persist')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByRole('button', { name: '保存' }).click()

    await page.reload()

    await expect(todoCard(page, title)).toBeVisible()
})
