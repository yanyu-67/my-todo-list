//Todo 新增、编辑、详情和完成状态
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

async function openCreateDialog(page) {
    await page.getByRole('button', { name: '+ 添加任务' }).click()
    await expect(
        page.getByRole('dialog', { name: '新建任务' }),
    ).toBeVisible()
}

async function createTodo(page, data = {}) {
    const title = data.title || uniqueName('todo')

    await openCreateDialog(page)

    const dialog = page.getByRole('dialog', { name: '新建任务' })

    await dialog.getByLabel('标题').fill(title)

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

    await expect(page.getByText('任务已创建')).toBeVisible()
    await expect(todoCard(page, title)).toBeVisible()

    return title
}

test.describe('Todo 主流程', () => {
    test.beforeEach(async ({ page }) => {
        await clearBrowserState(page)
        await loginByUi(page, testUser)
    })

    test('Todo 列表可以正常加载', async ({ page }) => {
        const responsePromise = page.waitForResponse((response) =>
            response.url().endsWith('/api/todos') &&
            response.request().method() === 'GET',
        )

        await page.goto('/todos')

        const response = await responsePromise
        expect(response.ok()).toBeTruthy()
        await expect(page.locator('.todo-list')).toBeVisible()
    })

    test('新增带完整字段的 Todo', async ({ page }) => {
        const title = uniqueName('add')

        await createTodo(page, {
            title,
            description: 'Playwright 自动化测试',
            dueDate: '2099-12-31',
            important: true,
        })

        const card = todoCard(page, title)

        await expect(card).toContainText('Playwright 自动化测试')
        await expect(card).toContainText('截止 2099-12-31')
        await expect(card).toContainText('重要')
        await expect(card).toContainText('进行中')
    })

    test('标题为空或仅空格时不能保存', async ({ page }) => {
        await openCreateDialog(page)

        const dialog = page.getByRole('dialog', { name: '新建任务' })
        await dialog.getByLabel('标题').fill('   ')
        await dialog.getByRole('button', { name: '保存' }).click()

        await expect(page.getByText('标题不能为空')).toBeVisible()
        await expect(dialog).toBeVisible()
    })

    test('编辑 Todo', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('before-edit'),
            description: '旧描述',
        })

        const card = todoCard(page, title)
        await card.getByRole('button', { name: '编辑' }).click()

        const dialog = page.getByRole('dialog', { name: '编辑任务' })
        await expect(dialog).toBeVisible()

        await dialog.getByLabel('标题').fill(`${title}-updated`)
        await dialog.getByLabel('描述').fill('新描述')
        await dialog.getByRole('button', { name: '保存' }).click()

        await expect(page.getByText('任务已更新')).toBeVisible()
        await expect(todoCard(page, `${title}-updated`)).toContainText('新描述')
    })

    test('点击任务主体查看详情', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('detail'),
            description: '详情描述',
            important: true,
        })

        const card = todoCard(page, title)
        await card.getByText(title, { exact: true }).click()

        const drawer = page.locator('.el-drawer').filter({ hasText: '任务详情' })

        await expect(drawer).toContainText(title)
        await expect(drawer).toContainText('详情描述')

        const status = drawer.locator('p').filter({ hasText: '状态：' })
        await expect(status).toContainText('进行中')

        const important = drawer.locator('p').filter({ hasText: '重要：' })
        await expect(important).toContainText('是')
    })

    test('空描述和空截止日期显示默认文案', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('empty-fields'),
        })

        await todoCard(page, title).getByText(title, { exact: true }).click()

        const drawer = page.locator('.el-drawer').filter({ hasText: '任务详情' })

        await expect(drawer).toContainText('暂无描述')
        const dueDate = drawer.locator('p').filter({ hasText: '截止日期：' })
        await expect(dueDate).toContainText('未设置')
    })

    test('切换完成状态', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('complete'),
        })

        const card = todoCard(page, title)
        await card.locator('.el-checkbox').click()

        await expect(card).toContainText('已完成')

        await card.locator('.el-checkbox').click()

        await expect(card).toContainText('进行中')
    })

    test('删除 Todo 并确认', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('delete'),
        })

        const card = todoCard(page, title)
        await card.getByRole('button', { name: '删除' }).click()

        const confirmDialog = page.getByRole('dialog', { name: '删除确认' })
        await expect(confirmDialog).toContainText(`确定永久删除“${title}”吗？`)

        await confirmDialog.getByRole('button', { name: '永久删除' }).click()

        await expect(page.getByText('任务已删除')).toBeVisible()
        await expect(todoCard(page, title)).toHaveCount(0)
    })

    test('取消删除不会删除 Todo', async ({ page }) => {
        const title = await createTodo(page, {
            title: uniqueName('cancel-delete'),
        })

        const card = todoCard(page, title)
        await card.getByRole('button', { name: '删除' }).click()

        const confirmDialog = page.getByRole('dialog', { name: '删除确认' })
        await confirmDialog.getByRole('button', { name: '取消' }).click()

        await expect(todoCard(page, title)).toBeVisible()
    })
})
