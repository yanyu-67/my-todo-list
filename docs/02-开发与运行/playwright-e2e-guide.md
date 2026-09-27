# Todo 项目 Playwright E2E 测试指南

## 1. 测试概览

### 1.1 覆盖范围

本项目需要覆盖：

- 用户注册
- 用户登录与退出登录
- 登录态持久化
- 路由鉴权与管理员权限
- Todo 查询、新增、编辑、详情查看
- Todo 完成状态切换
- Todo 删除
- Todo 搜索与状态筛选
- 今日任务、重要事项筛选
- 空状态
- Todo 后端持久化
- 修改密码
- 管理员用户管理

测试优先级：

1. Todo 主流程
2. 登录与注册
3. 修改密码
4. 管理员用户管理

### 1.2 当前未实现、不测试的功能

当前代码中未发现以下功能，因此不编写测试：

- 清空已完成
- 拖拽排序
- 键盘快捷键
- Todo 独立 Pinia store
- Todo 使用 `localStorage` 保存

Todo 数据实际通过后端 API 和数据库持久化；只有登录凭据使用 `localStorage`。

### 1.3 推荐文件组织

```text
frontend/
└── tests/
    ├── helpers.js
    ├── auth.spec.js
    ├── todo.spec.js
    ├── persistence.spec.js
    ├── change-password.spec.js
    └── admin-users.spec.js
```

现有的 `frontend/tests/example.spec.js` 是访问 DeepSeek 的示例，与本项目无关。建议后续删除或移出 `tests/`，否则会导致测试依赖外部网站。

---

## 2. 运行环境与现有配置

### 2.1 当前配置

当前 `frontend/playwright.config.js`：

- `testDir: './tests'`
- 启用 `fullyParallel`
- CI 环境重试 2 次
- CI 环境使用单 worker
- 使用 HTML Reporter
- 只启用 Chromium
- `trace: 'on-first-retry'`
- 未配置 `baseURL`
- 未配置 `webServer`

运行前需要手动启动：

```bash
cd backend
./mvnw spring-boot:run
```

另开终端：

```bash
cd frontend
npm run dev
```

默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- API：`http://localhost:8080/api`

如果端口不同，请修改下面的占位位置：

```text
前端地址：____________________
后端 API 地址：________________
```

### 2.2 建议配置片段

以下内容仅供写入 `playwright.config.js` 时参考，不要求现在修改配置：

```js
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',

  fullyParallel: false,

  forbidOnly: !!process.env.CI,

  retries: process.env.CI ? 2 : 0,

  workers: process.env.CI ? 1 : undefined,

  reporter: 'html',

  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // 只负责启动前端，后端仍需单独启动。
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !process.env.CI,
  },
})
```

注意：

- `webServer` 不会自动启动 Spring Boot。
- 如果使用真实后端，必须先准备 PostgreSQL。
- `fullyParallel: true` 与共享测试账号一起使用时容易产生数据污染，建议使用独立账号或关闭并行。
- `baseURL` 配置后，测试中可以使用 `page.goto('/login')`。

---

## 3. 测试数据管理

### 3.1 推荐账号

优先使用专用测试账号：

```text
普通用户：
用户名：TEST_USER
密码：TEST_PASSWORD

管理员：
用户名：TEST_ADMIN
密码：TEST_ADMIN_PASSWORD
```

可以通过环境变量传入：

```powershell
$env:TEST_USER="TEST_USER"
$env:TEST_PASSWORD="TEST_PASSWORD"
$env:TEST_ADMIN="TEST_ADMIN"
$env:TEST_ADMIN_PASSWORD="TEST_ADMIN_PASSWORD"
```

如果使用 API 预置数据，需要注意：

> 该方案依赖后端接口。后端接口路径、请求结构或鉴权方式变更时，测试代码也必须同步修改。

### 3.2 公共辅助文件

```js
// tests/helpers.js
import { expect } from '@playwright/test'

export const testUser = {
  username: process.env.TEST_USER || 'TEST_USER',
  password: process.env.TEST_PASSWORD || 'TEST_PASSWORD',
}

export const testAdmin = {
  username: process.env.TEST_ADMIN || 'TEST_ADMIN',
  password: process.env.TEST_ADMIN_PASSWORD || 'TEST_ADMIN_PASSWORD',
}

export const apiBase =
  process.env.TEST_API_BASE_URL || 'http://localhost:8080/api'

export function uniqueName(prefix = 'pw') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

export async function clearBrowserState(page) {
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
}

export async function loginByUi(page, account) {
  await page.goto('/login')

  await page.getByLabel('用户名').fill(account.username)
  await page.getByLabel('密码').fill(account.password)
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page).toHaveURL(/\/todos/)
}

export async function registerByApi(request, username, password) {
  const response = await request.post(`${apiBase}/auth/register`, {
    data: {
      username,
      password,
    },
  })

  expect(response.status()).toBe(201)
}

export async function createTodoByApi(request, token, data) {
  const response = await request.post(`${apiBase}/todos`, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    data,
  })

  expect(response.ok()).toBeTruthy()
  return response.json()
}
```

### 3.3 测试隔离原则

每个测试都应：

1. 清理浏览器端 `localStorage`。
2. 使用独立账号或唯一任务标题。
3. 不依赖上一个测试创建的数据。
4. 测试结束后删除自己创建的任务或用户。
5. 不删除管理员账号和共享环境中的非测试数据。

---

# 4. 测试代码

## 4.1 登录、退出和路由权限

### 测试目标

验证：

- 未登录访问受保护页面会跳转登录页。
- 登录成功后进入 Todo 页面。
- 登录失败显示错误。
- 登录态写入 `localStorage`。
- 退出登录后清除登录态。
- 普通用户无法进入管理员页面。

### 为什么这样测

使用 `getByLabel` 和 `getByRole` 定位表单控件，因为这些定位器基于可访问名称，不依赖 Element Plus 的内部 class。

```js
// tests/auth.spec.js
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

    await expect(page).toHaveURL(/\/login\?redirect=%2Ftodos/)
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
```

### 常见坑

- 不要直接断言 `.el-input__inner`，这是 Element Plus 的内部结构。
- 不要只判断 URL，登录成功还应检查页面内容。
- 普通用户测试不能使用管理员账号。
- 登录态断言应检查三个 key，而不是只检查 token。
- 401 跳转测试需要真实后端参与，因为无效 token 必须由后端返回 401。

## 4.2 用户注册

### 测试目标

验证：

- 注册页可以正常打开。
- 用户名为空时提示。
- 密码少于 6 位时提示。
- 两次密码不一致时提示。
- 注册成功后跳转登录页。
- 重复用户名显示后端错误。

### 测试代码

```js
// tests/auth-register.spec.js
import { test, expect } from '@playwright/test'
import { uniqueName } from './helpers.js'

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
    await page.getByLabel('密码').fill('12345')
    await page.getByLabel('确认密码').fill('12345')
    await page.getByRole('button', { name: '注册' }).click()

    await expect(page.getByText('密码至少6位')).toBeVisible()
  })

  test('两次密码不一致时不能注册', async ({ page }) => {
    await page.getByLabel('用户名').fill(uniqueName('mismatch'))
    await page.getByLabel('密码').fill('password123')
    await page.getByLabel('确认密码').fill('password456')
    await page.getByRole('button', { name: '注册' }).click()

    await expect(page.getByText('两次密码不一致')).toBeVisible()
  })

  test('注册成功后跳转登录页', async ({ page }) => {
    const username = uniqueName('register')

    await page.getByLabel('用户名').fill(username)
    await page.getByLabel('密码').fill('password123')
    await page.getByLabel('确认密码').fill('password123')
    await page.getByRole('button', { name: '注册' }).click()

    await expect(page.getByText('注册成功，请登录')).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
  })

  test('重复用户名注册失败', async ({ page, request }) => {
    const username = uniqueName('duplicate')

    await registerByApi(request, username, 'password123')

    await page.getByLabel('用户名').fill(username)
    await page.getByLabel('密码').fill('password123')
    await page.getByLabel('确认密码').fill('password123')
    await page.getByRole('button', { name: '注册' }).click()

    await expect(page.getByText('用户名已被使用')).toBeVisible()
    await expect(page).toHaveURL(/\/register/)
  })
})
```

### 常见坑

- 用户名必须使用唯一值，否则会受到数据库已有数据影响。
- Element Plus 的密码框可能包含显示密码按钮，不要使用模糊的 `getByRole('textbox').nth()`。
- 注册成功后需要等待路由跳转，不要立即断言登录页内容。
- 重复用户名测试需要真实后端参与，并且应使用一次性用户名，避免污染共享账号。

## 4.3 Todo 新增、编辑、详情和完成状态

### 测试目标

验证 Todo 的核心增、改、查和状态切换行为。

### 为什么这样测

`TodoView.vue` 没有独立 store，任务数据保存在组件内部的 `todos` ref 中。因此：

- 不通过 Pinia 或组件内部变量操作数据。
- 通过用户可见的页面行为创建和修改任务。
- 使用任务标题定位 `.todo-card`，再在卡片范围内查找按钮。
- `.todo-card` 是业务组件明确提供的 class，比 Element Plus 内部 class 更稳定。

```js
// tests/todo.spec.js
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
    await dialog.getByRole('switch').click()
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
    await expect(drawer).toContainText('状态：进行中')
    await expect(drawer).toContainText('重要：是')
  })

  test('空描述和空截止日期显示默认文案', async ({ page }) => {
    const title = await createTodo(page, {
      title: uniqueName('empty-fields'),
    })

    await todoCard(page, title).getByText(title, { exact: true }).click()

    const drawer = page.locator('.el-drawer').filter({ hasText: '任务详情' })

    await expect(drawer).toContainText('暂无描述')
    await expect(drawer).toContainText('截止日期：未设置')
  })

  test('切换完成状态', async ({ page }) => {
    const title = await createTodo(page, {
      title: uniqueName('complete'),
    })

    const card = todoCard(page, title)
    await card.getByRole('checkbox').check()

    await expect(card).toContainText('已完成')

    await card.getByRole('checkbox').uncheck()

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
```

### 常见坑

- 点击任务标题会打开详情抽屉，点击复选框不会打开详情，因为代码使用了 `@click.stop`。
- 编辑和新建使用不同的对话框标题。
- 删除操作必须等待确认弹窗，不能直接断言任务消失。
- 任务标题可能重复，因此测试数据必须使用唯一标题。
- 不要使用 `.el-card:nth-child()` 定位具体任务，排序会导致顺序变化。
- 列表加载、空字段默认文案依赖真实接口返回，相关测试需要真实后端参与。

## 4.4 Todo 搜索、状态筛选、今日和重要事项

### 测试目标

验证：

- 全部、进行中、已完成筛选。
- 标题搜索。
- 描述搜索。
- 搜索大小写和首尾空格处理。
- 今日任务筛选。
- 重要事项筛选。
- 筛选无结果时的空状态。

### 测试代码

```js
// tests/todo-filter.spec.js
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
    await dialog.getByRole('switch').click()
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

    await todoCard(page, completedTitle).getByRole('checkbox').check()
    await page.getByRole('radio', { name: /进行中/ }).click()

    await expect(todoCard(page, activeTitle)).toBeVisible()
    await expect(todoCard(page, completedTitle)).toHaveCount(0)
  })

  test('已完成筛选只显示已完成任务', async ({ page }) => {
    const activeTitle = uniqueName('active-only')
    const completedTitle = uniqueName('completed-only')

    await createTodo(page, { title: activeTitle })
    await createTodo(page, { title: completedTitle })

    await todoCard(page, completedTitle).getByRole('checkbox').check()
    await page.getByRole('radio', { name: /已完成/ }).click()

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
```

### 常见坑

- “今天”筛选依赖运行机器的本地日期，测试不要写死当前日期。
- 进入 `/todos?filter=today` 或 `/todos?filter=important` 后，`watch(pageFilter)` 会清空搜索和状态筛选。
- 空状态文案会随页面范围变化：
  - 全部：`还没有任务，先添加一个待办吧`
  - 今天：`今天没有到期任务`
  - 重要事项：`暂无重要任务`
- 不要只断言统计数字，必须同时断言任务卡片是否正确显示。
- “无任务空状态”必须使用没有任何 Todo 的一次性账号，不能用已有任务的账号代替。

## 4.5 Todo 后端持久化

### 测试目标

验证新增、编辑和完成状态切换结果刷新页面后仍然存在。

### 为什么这样测

Todo 数据由后端保存，刷新页面会重新执行 `listTodos()`。因此持久化测试必须真实刷新页面，不能只检查 Vue 当前内存状态。

### 测试代码

```js
// tests/persistence.spec.js
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
  await card.getByRole('checkbox').check()
  await expect(card).toContainText('已完成')

  await page.reload()

  await expect(todoCard(page, title)).toContainText('已完成')
})
```

### 常见坑

- `page.reload()` 前必须等待保存请求完成。
- 测试使用的账号必须拥有数据库访问权限。
- 后端数据库未启动时，该测试无法通过。
- 不能用 `localStorage` 模拟 Todo 数据来代替真实接口测试。

## 4.6 登录态持久化

### 测试目标

验证：

- 登录信息写入 `todo_token`、`todo_username`、`todo_role`。
- 页面刷新后仍保持登录状态。
- 清除 token 后访问受保护页面会跳回登录页。

### 测试代码

```js
// tests/auth-persistence.spec.js
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
```

### 常见坑

- 清除 `localStorage` 后，当前 Vue Pinia 实例中的状态不会自动同步清空，因此应重新导航或刷新页面。
- 不要只删除 token 而保留用户名和角色，否则无法验证完整退出行为。
- 401 过期处理应单独通过接口异常或后端失效 token 测试。

## 4.7 修改密码

### 测试目标

验证：

- 新密码少于 6 位。
- 两次密码不一致。
- 旧密码错误。
- 新旧密码相同。
- 修改成功后清除登录态并跳转登录页。

### 前置条件

建议为此测试创建一次性账号：

```text
修改密码测试账号：每次使用唯一用户名
```

### 测试代码

```js
// tests/change-password.spec.js
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
```

### 常见坑

- 旧密码错误和新旧密码相同依赖后端校验，需要真实后端参与。
- 修改密码成功后，前端会主动清除登录态，不应继续使用旧 token。
- 成功测试使用一次性账号，避免改变共享测试账号密码。
- “旧密码错误”和“新旧密码相同”属于后端校验，需要真实后端参与。

## 4.8 管理员用户管理

### 测试目标

验证：

- 管理员可以查看用户列表。
- 可以搜索用户名。
- 可以启用和禁用用户。
- 可以设置和撤销管理员角色。
- 可以删除用户。
- 取消确认时不执行操作。

### 前置条件

所有测试必须使用管理员账号：

```text
管理员账号：TEST_ADMIN
管理员密码：TEST_ADMIN_PASSWORD
```

角色切换测试使用一次性测试用户，绝不修改默认管理员账号的角色或状态。

### 测试代码

```js
// tests/admin-users.spec.js
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
    await expect(page.getByText('管理员')).toBeVisible()
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
    await confirmDialog.getByRole('button', { name: '确定' }).click()

    await expect(page.getByText('操作成功')).toBeVisible()
    await expect(userRow(page, username)).toContainText('已禁用')

    await userRow(page, username)
      .getByRole('button', { name: '启用账号' })
      .click()

    await page
      .getByRole('dialog', { name: '确认操作' })
      .getByRole('button', { name: '确定' })
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
    await confirmDialog.getByRole('button', { name: '确定' }).click()

    await expect(page.getByText('操作成功')).toBeVisible()
    await expect(userRow(page, username)).toContainText('管理员')

    await userRow(page, username)
      .getByRole('button', { name: '撤销管理员' })
      .click()

    await page
      .getByRole('dialog', { name: '确认操作' })
      .getByRole('button', { name: '确定' })
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
```

### 常见坑

- 所有管理员测试都需要管理员账号。
- 角色切换测试使用一次性用户，绝不修改默认管理员账号的角色或状态。
- 管理员测试需要真实后端参与，因为角色和状态都由后端保存。
- 不要修改默认管理员账号的角色或状态。
- 管理员测试不能使用普通账号。
- 用户状态和角色操作都有确认弹窗。
- 测试删除操作时必须使用一次性用户。
- 直接调用注册 API 是测试数据准备手段，不应替代注册页面本身的 E2E 测试。

---

# 5. 定位器策略

## 5.1 推荐优先级

优先使用：

```js
page.getByRole('button', { name: '登录' })
page.getByLabel('用户名')
page.getByPlaceholder('搜索标题或描述')
page.getByText('任务已创建')
```

推荐顺序：

1. `getByRole`
2. `getByLabel`
3. `getByPlaceholder`
4. `getByText`
5. 组件明确提供的业务 class
6. CSS 结构定位，仅作为最后手段

## 5.2 Todo 卡片定位

当前 Todo 卡片没有 `data-testid`，但有明确的 `.todo-card`：

```js
const card = page
  .locator('.todo-card')
  .filter({ hasText: todoTitle })
```

这样比：

```js
page.locator('.todo-card').nth(0)
```

更稳定，因为页面会按照完成状态、截止日期和更新时间排序。

## 5.3 不推荐的定位方式

```js
page.locator('.el-input__inner')
page.locator('.el-button--primary')
page.locator('.todo-card:nth-child(1)')
page.locator('div > div:nth-child(3)')
```

这些定位器依赖 Element Plus 内部结构或当前 DOM 顺序，容易因样式和组件版本变化而失效。

---

# 6. 稳定性与反模式

不要：

- 使用 `waitForTimeout()` 等待页面稳定。
- 依赖固定的 `nth()` 选择任务。
- 依赖 Element Plus 私有 class。
- 在多个测试之间共享会被修改的账号。
- 使用固定任务标题。
- 只断言 URL，不断言页面实际内容。
- 直接操作 Vue 内部变量或 Pinia 状态。
- 用 `localStorage` 模拟 Todo 数据持久化。
- 让测试依赖测试执行顺序。
- 在真实数据库中删除非测试数据。
- 忽略删除、禁用、角色切换的确认弹窗。
- 把后端 API 预置数据误当成注册、创建 Todo 的完整 UI 测试。

可以：

- 使用 `expect.poll()` 等待异步状态。
- 使用唯一用户名和唯一任务标题。
- 使用专用测试账号。
- 使用 API 快速准备测试数据。
- 使用 `page.reload()` 验证后端持久化。
- 对失败测试启用 trace、截图和视频。

---

# 7. 进阶主题

## 7.1 组件测试与全链路 E2E

### 全链路 E2E

优点：

- 验证真实浏览器行为。
- 覆盖 Vue Router、Axios、Spring Boot、JWT 和数据库。
- 能发现前后端字段不一致问题。

缺点：

- 依赖后端和 PostgreSQL。
- 执行速度较慢。
- 测试数据隔离复杂。

适合测试：

- 登录闭环。
- Todo 增删改查。
- 权限控制。
- 数据持久化。

### 组件测试

优点：

- 执行速度快。
- 可以单独测试空状态、筛选和条件渲染。
- 不依赖真实后端。

缺点：

- 当前 Todo 状态直接定义在 `TodoView.vue` 内，没有独立 store。
- 需要 mock `api/todo`、路由和 Element Plus 行为。
- 不能证明真实 API 和数据库工作正常。

适合测试：

- `v-if` 分支。
- 搜索和筛选计算逻辑。
- 重要标签、完成标签。
- 空状态显示。

当前项目建议：核心功能优先使用全链路 E2E，纯展示分支再考虑组件测试。

## 7.2 视觉回归

可以针对以下页面截图：

```js
await expect(page).toHaveScreenshot('todo-list.png')
```

适合场景：

- Todo 页面整体布局。
- 登录页。
- 管理员用户表格。
- 空状态页面。

注意：

- 固定浏览器、窗口大小和字体环境。
- 视觉测试不应替代语义断言。
- 日期、时间和动态任务数据需要固定或隐藏。

## 7.3 CI 集成

CI 中至少需要：

1. 启动 PostgreSQL。
2. 启动 Spring Boot。
3. 启动 Vite。
4. 注入测试账号环境变量。
5. 安装 Playwright 浏览器。
6. 执行测试。
7. 保存 HTML 报告和 trace。

示例：

```bash
npm ci
npx playwright install --with-deps chromium
npx playwright test
```

## 7.4 并行配置

由于测试会修改真实数据库，建议：

- 每个测试使用独立账号。
- 或每个 worker 使用独立数据库。
- 或设置 `workers: 1`。
- 不要让多个测试同时操作同一个 Todo。

---

# 8. 验收清单

## 认证

- [✅] 注册成功 —— 对应 auth-register.spec.js › 注册成功后跳转登录页
- [✅] 用户名为空校验 —— 对应 auth-register.spec.js › 用户名为空时不能注册
- [✅] 密码长度校验 —— 对应 auth-register.spec.js › 密码少于 6 为时不能注册
- [✅] 两次密码一致性校验 —— 对应 auth-register.spec.js › 两次密码不一致时不能注册
- [✅] 重复用户名 —— 对应 auth-register.spec.js › 重复用户名注册失败
- [✅] 登录成功 —— 对应 auth.spec.js › 使用正确账号登录成功
- [✅] 登录失败 —— 对应 auth.spec.js › 错误账号密码登录失败
- [✅] 退出登录 —— 对应 auth.spec.js › 退出登录后清除登录态
- [✅] 登录态刷新后保持 —— 对应 auth-persistence.spec.js › 登录态刷新后仍然存在
- [✅] 清除登录态后回到登录页 —— 对应 auth-persistence.spec.js › 清除登录态后访问 Todo 会跳转登录页
- [✅] 401 后跳转登录页 —— 对应 auth.spec.js › 失效 token 返回 401 后跳转登录页

## Todo 主流程

- [✅] Todo 列表加载 —— 对应 todo.spec.js › Todo 列表可以正常加载
- [✅] 新增 Todo —— 对应 todo.spec.js › 新增带完整字段的 Todo
- [✅] 标题为空 —— 对应 todo.spec.js › 标题为空或仅空格时不能保存
- [✅] 编辑 Todo —— 对应 todo.spec.js › 编辑 Todo
- [✅] 查看详情 —— 对应 todo.spec.js › 点击任务主体查看详情
- [✅] 空描述 —— 对应 todo.spec.js › 空描述和空截止日期显示默认文案
- [✅] 空截止日期 —— 对应 todo.spec.js › 空描述和空截止日期显示默认文案
- [✅] 切换完成状态 —— 对应 todo.spec.js › 切换完成状态
- [✅] 删除并确认 —— 对应 todo.spec.js › 删除 Todo 并确认
- [✅] 删除并取消 —— 对应 todo.spec.js › 取消删除不会删除 Todo
- [✅] 标题搜索 —— 对应 todo-filter.spec.js › 按标题搜索 Todo
- [✅] 描述搜索 —— 对应 todo-filter.spec.js › 按描述搜索 Todo
- [✅] 进行中筛选 —— 对应 todo-filter.spec.js › 进行中筛选只显示未完成任务
- [✅] 已完成筛选 —— 对应 todo-filter.spec.js › 已完成筛选只显示已完成任务
- [✅] 今日筛选 —— 对应 todo-filter.spec.js › 今天页面只显示今天到期任务
- [✅] 重要事项筛选 —— 对应 todo-filter.spec.js › 重要事项页面只显示重要任务
- [✅] 搜索无结果空状态 —— 对应 todo-filter.spec.js › 无匹配结果时显示空状态
- [✅] 无任务空状态 —— 对应 todo-filter.spec.js › 新账号没有任务时显示空状态
- [✅] 刷新后数据持久化 —— 对应 persistence.spec.js › Todo 刷新后仍然存在；persistence.spec.js › 完成状态刷新后仍然保持

## 修改密码

- [✅] 新密码少于 6 位 —— 对应 change-password.spec.js › 新密码少于 6 位时不能提交
- [✅] 两次密码不一致 —— 对应 change-password.spec.js › 两次新密码不一致时不能提交
- [✅] 旧密码错误且不跳转 —— 对应 change-password.spec.js › 旧密码错误时显示错误且不跳转
- [✅] 新旧密码相同且不跳转 —— 对应 change-password.spec.js › 新密码与旧密码相同时显示错误且不跳转
- [✅] 修改成功后重新登录 —— 对应 change-password.spec.js › 修改密码成功后需要重新登录

## 管理员

- [✅] 普通用户禁止访问管理员页面 —— 对应 auth.spec.js › 普通用户不能访问管理员页面
- [✅] 管理员可以访问用户管理 —— 对应 auth.spec.js › 管理员可以访问用户管理页面
- [✅] 用户列表加载 —— 对应 admin-users.spec.js › 管理员可以查看用户列表
- [✅] 用户搜索 —— 对应 admin-users.spec.js › 可以搜索用户名
- [✅] 禁用用户 —— 对应 admin-users.spec.js › 可以禁用并重新启用用户
- [✅] 启用用户 —— 对应 admin-users.spec.js › 可以禁用并重新启用用户
- [✅] 设置管理员角色 —— 对应 admin-users.spec.js › 设置管理员角色后可以撤销管理员角色
- [✅] 撤销管理员角色 —— 对应 admin-users.spec.js › 设置管理员角色后可以撤销管理员角色
- [✅] 角色切换使用一次性账号 —— 对应 admin-users.spec.js › 设置管理员角色后可以撤销管理员角色
- [✅] 删除用户 —— 对应 admin-users.spec.js › 确认后可以删除用户
- [✅] 取消删除 —— 对应 admin-users.spec.js › 取消删除用户不会执行删除

统计：已实现 46 条 / 待补 0 条 / 共 46 条

---

# 9. 最小可运行版本

以下 5 个测试可以作为第一阶段冒烟测试：

```js
// tests/smoke.spec.js
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
  await card.getByRole('checkbox').check()

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
```

这 5 个测试分别覆盖：

- 登录前置条件
- Todo 新增
- 完成状态切换
- 搜索
- 后端持久化

后续再按前文分节补充注册、删除、编辑、权限、修改密码和管理员管理测试。
