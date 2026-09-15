# M5 手把手指导手册：前端应用骨架

> 适合第一次学习 Vue 3 的同学。
>
> 本手册只完成 M5：把 Vue 前端的页面、路由、Pinia 状态管理、Axios 请求层和基础 Element Plus 布局搭好。M6 才实现完整的登录、任务增删改查和前后端联调流程。

## 1. M5 要完成什么

M5 的成果不是“所有按钮都已经能用”，而是先建立一个可以继续开发的前端骨架：

```text
浏览器地址
  ↓
Vue Router 决定显示哪个页面
  ↓
路由守卫检查 Pinia/localStorage 中是否有 Token
  ↓
页面调用 api/*.js
  ↓
Axios 自动把 Token 放进 Authorization 请求头
  ↓
Spring Boot 后端返回统一 JSON
```

完成后应当能够：

- 访问 `/login`、`/register`、`/todos` 和 `/change-password`；
- 未登录访问 `/todos` 时自动跳转 `/login`；
- 刷新页面后仍能恢复 Token 和用户名；
- Axios 自动发送 `Authorization: Bearer <token>`；
- 后端返回 401 时清除登录状态并回到登录页；
- 首页显示深色主题的导航、统计卡片、筛选栏和任务列表占位结构；
- 使用 Element Plus 的表单、按钮、消息、确认框等组件。

本阶段不实现：真正的任务加载、任务新建/编辑/删除、统计计算和完整的注册登录交互。这些属于 M6。

## 2. 开始前检查

当前项目的前端目录是 `D:\github-copilot\my-todo-list\frontend`。先打开 PowerShell：

```powershell
cd D:\github-copilot\my-todo-list
git status
node --version
npm --version
```

如果 `node` 或 `npm` 找不到，需要先安装 Node.js。不要删除当前工作区已有修改；M4 仍处于认证验收阶段，M5 文档只指导前端。

当前 `frontend/package.json` 已经包含：

- Vue 3：页面组件框架；
- Vue Router：前端地址和页面切换；
- Pinia：保存登录状态等共享数据；
- Element Plus：按钮、表单、布局、消息等 UI 组件；
- Axios：调用后端 HTTP API。

如果 `node_modules` 尚未安装，执行：

```powershell
cd D:\github-copilot\my-todo-list\frontend
npm install
```

## 3. 先理解几个核心知识点

### 3.1 Vue 组件和 `script setup`

一个 `.vue` 文件通常由三部分组成：

```vue
<script setup>
// JavaScript：状态和函数
</script>

<template>
  <!-- HTML：页面结构 -->
</template>

<style scoped>
/* CSS：当前组件样式 */
</style>
```

`ref('')` 创建可变化的响应式值，模板中直接写变量名即可；在 JavaScript 中修改时需要写 `.value`：

```js
import { ref } from 'vue'

const username = ref('')
username.value = 'alice'
```

### 3.2 Router 和路由守卫

前端路由不是后端接口路由。`/todos` 是浏览器页面地址，`/api/todos` 才是后端接口地址。

路由守卫在进入页面前执行：

```js
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})
```

它只改善用户体验，不是安全边界。真正的权限校验仍由 M4 的 Spring Security 完成。

### 3.3 Pinia 和 localStorage 的分工

- Pinia：当前页面运行期间，组件可以方便地读取和修改状态；
- `localStorage`：浏览器刷新后仍保留字符串数据；
- 两者结合：应用启动时从 `localStorage` 恢复 Token，登录/退出时同时更新两处。

本项目使用两个 key：`todo_token` 和 `todo_username`。后续所有文件必须保持 key 一致。

### 3.4 Axios 拦截器

- 请求拦截器：每个请求发送前执行，适合统一添加 Token；
- 响应拦截器：后端响应回来后执行，适合统一处理 401、403 和错误提示；
- `baseURL`：所有 API 的公共前缀。

本项目的后端接口真实路径为 `/api/auth/login` 和 `/api/todos`，所以本地开发环境建议把 `VITE_API_BASE_URL` 设置为 `http://localhost:8080/api`，API 文件中只写 `/auth/login`、`/todos`。

## 4. 创建前端目录

在 `frontend/src` 下创建以下目录和文件：

```text
src/
├─ api/
│  ├─ auth.js
│  ├─ http.js
│  └─ todo.js
├─ layouts/
│  └─ AppLayout.vue
├─ router/
│  └─ index.js
├─ stores/
│  └─ auth.js
├─ views/
│  ├─ LoginView.vue
│  ├─ RegisterView.vue
│  ├─ TodoView.vue
│  └─ ChangePasswordView.vue
├─ App.vue
├─ main.js
└─ style.css
```

可以在 VS Code 中右键 `src` 创建文件夹和文件，也可以使用 PowerShell：

```powershell
cd D:\github-copilot\my-todo-list\frontend
New-Item -ItemType Directory -Force src\api,src\layouts,src\router,src\stores,src\views
```

## 5. 配置 API 地址

在 `frontend` 目录新建 `.env.local`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

注意：

- Vite 只有以 `VITE_` 开头的变量才会暴露给前端代码；
- `.env.local` 已在 `.gitignore` 中，不要把真实密钥写入前端；
- 修改环境变量后必须重启 `npm run dev`。

## 6. 编写 Pinia 登录状态

把下面代码保存为 `frontend/src/stores/auth.js`：

```js
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

const TOKEN_KEY = 'todo_token'
const USERNAME_KEY = 'todo_username'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref(localStorage.getItem(USERNAME_KEY) || '')
  const isLoggedIn = computed(() => Boolean(token.value))

  function setLogin(loginData) {
    token.value = loginData.token
    username.value = loginData.username
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USERNAME_KEY, username.value)
  }

  function clearLogin() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  return { token, username, isLoggedIn, setLogin, clearLogin }
})
```

这里使用的是 Pinia Setup Store：`ref` 是状态，`computed` 是根据状态计算出来的值，函数是修改状态的动作。组件不要到处直接操作 localStorage，而应统一调用 `setLogin` 和 `clearLogin`。

## 7. 编写 Axios 请求层

### 7.1 `http.js`

将 `frontend/src/api/http.js` 改为：

```js
import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('todo_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('todo_token')
      localStorage.removeItem('todo_username')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login?reason=expired'
      }
    }
    return Promise.reject(error)
  },
)

export function checkHealth() {
  return http.get('/health')
}

export default http
```

这里的默认地址和 `.env.local` 一致；`||` 是为了即使环境变量暂时没读到，也能得到清晰的本地默认值。

### 7.2 `auth.js`

将 `frontend/src/api/auth.js` 改为：

```js
import http from './http'

export async function login(username, password) {
  const response = await http.post('/auth/login', { username, password })
  const result = response.data
  if (result.code !== 0) throw new Error(result.message || '登录失败')
  return result.data
}

export async function register(username, password) {
  const response = await http.post('/auth/register', { username, password })
  return response.data
}

export async function changePassword(oldPassword, newPassword, confirmPassword) {
  const response = await http.post('/auth/change-password', {
    oldPassword,
    newPassword,
    confirmPassword,
  })
  return response.data
}
```

后端登录成功格式是 `{ code: 0, message: 'success', data: { token, username } }`，因此登录函数返回 `result.data`，交给 Pinia 保存。

### 7.3 `todo.js`

先创建一个供 M6 继续使用的 API 文件：

```js
import http from './http'

export const listTodos = () => http.get('/todos')
export const getTodo = (id) => http.get(`/todos/${id}`)
export const createTodo = (data) => http.post('/todos', data)
export const updateTodo = (id, data) => http.put(`/todos/${id}`, data)
export const setTodoCompleted = (id, completed) =>
  http.patch(`/todos/${id}/complete`, { completed })
export const deleteTodo = (id) => http.delete(`/todos/${id}`)
```

当前后端 `TodoController` 的删除接口返回 204，所以 M6 删除成功后不要强行读取 `response.data`。

## 8. 配置路由

保存为 `frontend/src/router/index.js`：

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/todos' },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    {
      path: '/todos',
      name: 'todos',
      component: () => import('../views/TodoView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('../views/ChangePasswordView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'register') && authStore.isLoggedIn) {
    return { name: 'todos' }
  }
})

export default router
```

`component: () => import(...)` 是懒加载：只有真正访问页面时才加载该页面代码。`meta.requiresAuth` 是我们自己给路由添加的标记。

## 9. 入口文件和 Element Plus

将 `frontend/src/main.js` 改为：

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

应用必须先注册 Pinia 和 Router，页面组件中才能调用 `useAuthStore()` 和 `useRouter()`。

## 10. 编写页面骨架

### 10.1 `App.vue`

```vue
<template>
  <router-view />
</template>
```

`router-view` 是路由页面的插槽。当前 URL 对应的页面会显示在这里。

### 10.2 登录页

保存为 `frontend/src/views/LoginView.vue`：

```vue
<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value.trim() || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await login(username.value.trim(), password.value)
    authStore.setLogin(data)
    await router.push(route.query.redirect || '/todos')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card">
      <p class="eyebrow">MY TODO LIST</p>
      <h1>欢迎回来</h1>
      <p class="muted">登录后管理属于你的每一项任务。</p>
      <el-form @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" show-password placeholder="至少 6 位" autocomplete="current-password" />
        </el-form-item>
        <el-button class="full-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
      </el-form>
      <p class="auth-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </el-card>
  </main>
</template>
```

### 10.3 注册页

保存为 `frontend/src/views/RegisterView.vue`：

```vue
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value.trim()) return ElMessage.warning('用户名不能为空')
  if (password.value.length < 6) return ElMessage.warning('密码至少 6 位')
  if (password.value !== confirmPassword.value) return ElMessage.warning('两次密码不一致')
  loading.value = true
  try {
    const result = await register(username.value.trim(), password.value)
    if (result.code !== 0) throw new Error(result.message || '注册失败')
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <el-card class="auth-card">
      <p class="eyebrow">CREATE ACCOUNT</p>
      <h1>创建账号</h1>
      <el-form @submit.prevent="submit">
        <el-form-item label="用户名"><el-input v-model="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="password" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password /></el-form-item>
        <el-button class="full-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
      </el-form>
      <p class="auth-link"><router-link to="/login">返回登录</router-link></p>
    </el-card>
  </main>
</template>
```

### 10.4 首页布局

保存为 `frontend/src/layouts/AppLayout.vue`：

```vue
<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.clearLogin()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="sidebar">
      <div class="brand">TODO<span>.</span></div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/todos">我的任务</el-menu-item>
        <el-menu-item index="/todos?filter=today">今天</el-menu-item>
        <el-menu-item index="/todos?filter=important">重要事项</el-menu-item>
      </el-menu>
      <div class="sidebar-bottom">
        <div class="user-name">{{ authStore.username }}</div>
        <el-button text @click="router.push('/change-password')">修改密码</el-button>
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="content-area">
      <slot />
    </el-main>
  </el-container>
</template>
```

保存为 `frontend/src/views/TodoView.vue`：

```vue
<script setup>
import { ref } from 'vue'
import AppLayout from '../layouts/AppLayout.vue'

const activeFilter = ref('all')
const keyword = ref('')
</script>

<template>
  <AppLayout>
    <section class="page-header">
      <div><p class="eyebrow">WORKSPACE / TODAY</p><h1>我的任务</h1></div>
      <el-button type="primary">＋ 添加任务</el-button>
    </section>
    <section class="stats-grid">
      <el-card><span>未完成</span><strong>—</strong></el-card>
      <el-card><span>今日到期</span><strong>—</strong></el-card>
      <el-card><span>已完成</span><strong>—</strong></el-card>
      <el-card><span>完成率</span><strong>N/A</strong></el-card>
    </section>
    <section class="toolbar">
      <el-radio-group v-model="activeFilter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="active">进行中</el-radio-button>
        <el-radio-button value="completed">已完成</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" clearable placeholder="搜索任务标题或描述" class="search-input" />
    </section>
    <el-empty description="M6 将在这里加载任务列表" />
  </AppLayout>
</template>
```

`TodoView.vue` 保留上面的 `AppLayout` 包裹即可；布局组件通过 `<slot />` 显示页面内容。

### 10.5 修改密码占位页

保存为 `frontend/src/views/ChangePasswordView.vue`：

```vue
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'

const router = useRouter()
const authStore = useAuthStore()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

async function submit() {
  if (newPassword.value.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (newPassword.value !== confirmPassword.value) return ElMessage.warning('两次密码不一致')
  try {
    await changePassword(oldPassword.value, newPassword.value, confirmPassword.value)
    authStore.clearLogin()
    ElMessage.success('密码已修改，请重新登录')
    await router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改密码失败')
  }
}
</script>

<template>
  <AppLayout>
    <el-card class="form-card"><h1>修改密码</h1>
      <el-form @submit.prevent="submit">
        <el-form-item label="旧密码"><el-input v-model="oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password /></el-form-item>
        <el-button type="primary" native-type="submit">保存并重新登录</el-button>
      </el-form>
    </el-card>
  </AppLayout>
</template>
```

## 11. 添加基础深色主题

将 `frontend/src/style.css` 替换为以下起步样式：

```css
:root {
  font-family: Inter, ui-sans-serif, system-ui, sans-serif;
  color: #f4f7ec;
  background: #111410;
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* { box-sizing: border-box; }
body { margin: 0; min-width: 320px; background: #111410; }
button, input { font: inherit; }
#app { min-height: 100vh; }
.auth-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; }
.auth-card { width: min(100%, 430px); background: #1b2019; border: 1px solid #30382c; }
.auth-card h1, .page-header h1, .form-card h1 { margin: 8px 0 12px; color: #f4f7ec; }
.muted, .auth-link, .eyebrow { color: #9ba893; }
.eyebrow { font-size: 12px; letter-spacing: .15em; }
.full-button { width: 100%; margin-top: 8px; }
.auth-link { margin-top: 20px; text-align: center; }
.auth-link a { color: #d6f56a; }
.app-shell { min-height: 100vh; background: #111410; }
.sidebar { display: flex; flex-direction: column; padding: 28px 16px; background: #171b15; border-right: 1px solid #30382c; }
.brand { padding: 0 16px 30px; font-size: 24px; font-weight: 800; letter-spacing: .08em; }
.brand span { color: #d6f56a; }
.sidebar .el-menu { border-right: 0; background: transparent; }
.sidebar-bottom { margin-top: auto; padding: 16px; display: grid; gap: 8px; }
.user-name { color: #d6f56a; font-weight: 600; }
.content-area { padding: 42px clamp(24px, 5vw, 72px); }
.page-header, .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 32px 0; }
.stats-grid .el-card { background: #1b2019; border-color: #30382c; }
.stats-grid span { display: block; color: #9ba893; }
.stats-grid strong { display: block; margin-top: 12px; font-size: 28px; color: #d6f56a; }
.toolbar { margin-bottom: 24px; }
.search-input { max-width: 320px; }
.form-card { max-width: 600px; background: #1b2019; border-color: #30382c; }
@media (max-width: 800px) {
  .sidebar { width: 180px !important; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .toolbar { align-items: stretch; flex-direction: column; }
  .search-input { max-width: none; }
}
```

这一步只建立视觉基调：深色背景、左侧导航和荧光黄绿色主色。不要在 M5 花时间追求像素级还原，M6 功能稳定后再集中调整样式。

## 12. 启动和验证

在一个 PowerShell 窗口启动后端，在另一个窗口启动前端：

```powershell
cd D:\github-copilot\my-todo-list\backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd D:\github-copilot\my-todo-list\frontend
npm run dev
```

浏览器打开 `http://localhost:5173`，按下面顺序验收：

1. 直接访问 `http://localhost:5173/todos`，应跳转到 `/login`；
2. 访问 `/register`，注册页面能显示；
3. 打开开发者工具 Application → Local Storage，手动加入 `todo_token=test` 和 `todo_username=alice`；
4. 刷新 `/todos`，应显示首页骨架和用户名；
5. 打开 Network，调用任意 API 时检查 Request Headers 是否出现 `Authorization: Bearer test`；
6. 删除 `todo_token` 后刷新 `/todos`，应重新跳回登录页；
7. 在 Console 执行 `fetch('http://localhost:8080/api/todos')`，如果后端运行正常，应得到 401，而不是前端页面崩溃；
8. 执行 `npm run build`，必须成功完成生产构建。

注意：手动伪造的 `test` Token 会被后端拒绝，这是正确现象。它只用于检查请求拦截器有没有附加请求头。真正登录要使用 M4 后端返回的 JWT。

## 13. 常见错误排查

### 页面空白或控制台提示找不到模块

检查文件路径和大小写：`../stores/auth`、`../router` 等必须与目录名称一致。Windows 对大小写不敏感，但构建工具和部署环境可能敏感。

### 点击链接没有切换页面

确认 `main.js` 中有 `app.use(router)`，`App.vue` 中有 `<router-view />`，并且使用的是 `<router-link>` 或 `router.push()`，不是普通的后端跳转。

### 一直被跳回登录页

检查 localStorage 是否存在非空的 `todo_token`。还要确认 `main.js` 先执行 `app.use(createPinia())`，再挂载应用。

### 请求地址变成 `undefined/auth/login`

确认 `.env.local` 在 `frontend` 根目录，不是 `frontend/src`；变量名必须是 `VITE_API_BASE_URL`；修改后重启 Vite。

### 浏览器出现 CORS 错误

后端当前允许的开发来源是 `http://localhost:5173`。不要使用 `127.0.0.1:5173` 混用，或者把端口改了却忘记同步 `WebConfig` 和 `SecurityConfig`。

### 401 后弹窗不断出现

响应拦截器只能负责清理状态和跳转一次。不要在每个页面又重复写一套 401 跳转逻辑，否则容易形成重复提示或跳转循环。

### Element Plus 组件没有样式

确认 `main.js` 中导入了：

```js
import 'element-plus/dist/index.css'
```

## 14. M5 验收清单

- [ ] `frontend/src/router/index.js` 已创建，登录、注册、首页、修改密码路由可访问。
- [ ] `/todos` 和 `/change-password` 配置了 `meta.requiresAuth`。
- [ ] 未登录访问受保护页面会跳转 `/login`。
- [ ] Pinia 中保存了 Token、用户名和登录状态。
- [ ] 刷新浏览器后能从 localStorage 恢复登录状态。
- [ ] Axios 请求拦截器会添加 `Authorization: Bearer <token>`。
- [ ] Axios 响应拦截器能处理 401 并清理本地登录信息。
- [ ] Element Plus 已全局注册并加载样式。
- [ ] 页面具备深色主题、左侧导航、顶部标题、统计卡片、筛选栏和空状态。
- [ ] `npm run build` 成功。
- [ ] 没有提交 `.env.local`、Token、密码、`node_modules` 或 `dist`。

## 15. 提交 M5

先查看只与本阶段有关的变更：

```powershell
cd D:\github-copilot\my-todo-list
git diff -- frontend/src
git status
```

确认没有敏感信息后提交：

```powershell
git add frontend/src docs/M5_GUIDE.md
git diff --cached --check
git commit -m "feat: scaffold frontend application"
```

提交完成后，M6 的第一项工作就是把 `TodoView.vue` 中的占位统计和空状态替换为真实 API 请求，再逐步加入任务表单、详情抽屉、删除确认和状态切换。
