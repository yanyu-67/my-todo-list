//Todo 后端持久化
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

test('Todo 刷新后仍然存在', async ({ page }) => {
    await clearBrowserState(page)
    await loginByUi(page, testUser)

    const title = uniqueName('persist')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByLabel('描述').fill('持久化测试')
    await dialog.getByRole('button', { name: '保存' }).click()

    await expect(todoCard(page, title)).toBeVisible()

    await page.reload()

    await expect(todoCard(page, title)).toContainText('持久化测试')
})

test('完成状态刷新后仍然保持', async ({ page }) => {
    await clearBrowserState(page)
    await loginByUi(page, testUser)

    const title = uniqueName('persist-completed')

    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })
    await dialog.getByLabel('标题').fill(title)
    await dialog.getByRole('button', { name: '保存' }).click()

    const card = todoCard(page, title)
    await card.locator('.el-checkbox').click()
    await expect(card).toContainText('已完成')

    await page.reload()

    await expect(todoCard(page, title)).toContainText('已完成')
})
