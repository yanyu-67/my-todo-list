//Todo 搜索、状态筛选、今日和重要事项
import { test, expect } from '@playwright/test'
import {
    clearBrowserState,
    loginByUi,
    registerByApi,
    testUser,
    uniqueName,
} from './helpers.js'

function todoCard(page, title) {
    return page.locator('.todo-card').filter({ hasText: title })
}

async function createTodo(page, data) {
    await page.getByRole('button', { name: '+ 添加任务' }).click()

    const dialog = page.getByRole('dialog', { name: '新建任务' })

    await dialog.getByLabel('标题').fill(data.title)

    if (data.description) {
        await dialog.getByLabel('描述').fill(data.description)
    }

    if (data.dueDate) {
        await dialog.locator('input[type="date"]').fill(data.dueDate)
    }

    if (data.important) {
        await dialog.locator('.el-switch').click()
    }

    await dialog.getByRole('button', { name: '保存' }).click()
    await expect(todoCard(page, data.title)).toBeVisible()
}

test.describe('Todo 搜索与筛选', () => {
    test.beforeEach(async ({ page }) => {
        await clearBrowserState(page)
        await loginByUi(page, testUser)
    })

    test('按标题搜索 Todo', async ({ page }) => {
        const target = uniqueName('search-title')
        const other = uniqueName('other-title')

        await createTodo(page, { title: target })
        await createTodo(page, { title: other })

        await page.getByPlaceholder('搜索标题或描述').fill(target)

        await expect(todoCard(page, target)).toBeVisible()
        await expect(todoCard(page, other)).toHaveCount(0)
    })

    test('按描述搜索 Todo', async ({ page }) => {
        const target = uniqueName('search-description')
        const other = uniqueName('other-description')

        await createTodo(page, {
            title: target,
            description: '唯一描述关键字',
        })

        await createTodo(page, {
            title: other,
            description: '其他描述',
        })

        await page.getByPlaceholder('搜索标题或描述').fill('唯一描述关键字')

        await expect(todoCard(page, target)).toBeVisible()
        await expect(todoCard(page, other)).toHaveCount(0)
    })

    test('搜索忽略大小写并去除首尾空格', async ({ page }) => {
        const title = uniqueName('CaseSensitive')

        await createTodo(page, { title })
        await page
            .getByPlaceholder('搜索标题或描述')
            .fill(`  ${title.toUpperCase()}  `)

        await expect(todoCard(page, title)).toBeVisible()
    })

    test('进行中筛选只显示未完成任务', async ({ page }) => {
        const activeTitle = uniqueName('active')
        const completedTitle = uniqueName('completed')

        await createTodo(page, { title: activeTitle })
        await createTodo(page, { title: completedTitle })

    await todoCard(page, completedTitle).locator('.el-checkbox').click()
        await page
            .locator('.status-switch .el-radio-button__inner')
            .filter({ hasText: '进行中' })
            .click()

        await expect(todoCard(page, activeTitle)).toBeVisible()
        await expect(todoCard(page, completedTitle)).toHaveCount(0)
    })

    test('已完成筛选只显示已完成任务', async ({ page }) => {
        const activeTitle = uniqueName('active-only')
        const completedTitle = uniqueName('completed-only')

        await createTodo(page, { title: activeTitle })
        await createTodo(page, { title: completedTitle })

    await todoCard(page, completedTitle).locator('.el-checkbox').click()
        await page
            .locator('.status-switch .el-radio-button__inner')
            .filter({ hasText: '已完成' })
            .click()

        await expect(todoCard(page, completedTitle)).toBeVisible()
        await expect(todoCard(page, activeTitle)).toHaveCount(0)
    })

    test('今天页面只显示今天到期任务', async ({ page }) => {
        const today = new Date()
        const date = [
            today.getFullYear(),
            String(today.getMonth() + 1).padStart(2, '0'),
            String(today.getDate()).padStart(2, '0'),
        ].join('-')

        const todayTitle = uniqueName('today')
        const laterTitle = uniqueName('later')

        await createTodo(page, { title: todayTitle, dueDate: date })
        await createTodo(page, { title: laterTitle, dueDate: '2099-12-31' })

        await page.getByRole('menuitem', { name: '今天' }).click()

        await expect(page).toHaveURL(/filter=today/)
        await expect(page.getByRole('heading', { name: '今天' })).toBeVisible()
        await expect(todoCard(page, todayTitle)).toBeVisible()
        await expect(todoCard(page, laterTitle)).toHaveCount(0)
    })

    test('重要事项页面只显示重要任务', async ({ page }) => {
        const importantTitle = uniqueName('important')
        const normalTitle = uniqueName('normal')

        await createTodo(page, { title: importantTitle, important: true })
        await createTodo(page, { title: normalTitle, important: false })

        await page.getByRole('menuitem', { name: '重要事项' }).click()

        await expect(page).toHaveURL(/filter=important/)
        await expect(
            page.getByRole('heading', { name: '重要事项' }),
        ).toBeVisible()
        await expect(todoCard(page, importantTitle)).toBeVisible()
        await expect(todoCard(page, normalTitle)).toHaveCount(0)
    })

    test('无匹配结果时显示空状态', async ({ page }) => {
        await page
            .getByPlaceholder('搜索标题或描述')
            .fill('不存在的任务关键字')

        await expect(
            page.getByText('还没有任务，先添加一个待办吧'),
        ).toBeVisible()
    })

    test('新账号没有任务时显示空状态', async ({ page, request }) => {
        const username = uniqueName('empty-todos')
        const password = 'password123'

        await registerByApi(request, username, password)
        await clearBrowserState(page)
        await loginByUi(page, { username, password })

        await expect(
            page.getByText('还没有任务，先添加一个待办吧'),
        ).toBeVisible()
    })
})
