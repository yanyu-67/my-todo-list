# M6 手把手指导手册：前端业务功能与联调

> 面向编程新手，基于当前仓库编写。M5 已完成 Vue、路由、Pinia、Axios 和基础布局，M6 要把页面真正接上后端。

## 1. M6 的目标

完成后，用户可以注册、登录、退出登录、修改密码，查看自己的任务和统计，搜索和筛选任务，并完成新建、详情、编辑、完成/取消完成、二次确认删除。页面还要处理加载中、空列表、搜索无结果、接口失败、重复提交和 Token 过期。

~~~text
登录 → 保存 JWT → Axios 自动携带 Bearer Token → 加载任务
    → 新建/编辑/完成/删除 → 重新加载 → 列表和统计同步
~~~

## 2. 当前项目和联调注意点

| 文件 | 作用 |
| --- | --- |
| frontend/src/api/http.js | Axios、API 地址、JWT 请求头、401 处理 |
| frontend/src/api/auth.js | 注册、登录、修改密码 |
| frontend/src/api/todo.js | 任务接口 |
| frontend/src/stores/auth.js | Pinia 登录状态 |
| frontend/src/router/index.js | 页面路由和登录守卫 |
| frontend/src/views/TodoView.vue | M6 的主要实现文件 |
| frontend/src/layouts/AppLayout.vue | 左侧导航、退出登录 |

当前代码有三个必须记住的事实：

1. 后端路径是 /api/auth/change-password，旧的前端代码写成了 /auth/changePassword，需要改正。
2. 后端目前没有 /api/todos/statistics，第一版 M6 在前端根据任务数组计算统计。
3. DELETE /api/todos/{id} 返回 HTTP 204，没有 JSON body，删除成功时不要读取 response.data.data。

当前 GET /api/todos 也没有搜索和筛选参数，因此第一版先加载当前用户的全部任务，在前端筛选和排序。后端仍然负责权限隔离。

## 3. 启动项目

~~~powershell
cd D:\github-copilot\my-todo-list
git status
node --version
npm --version
cd frontend
npm install
~~~

窗口 A：

~~~powershell
cd D:\github-copilot\my-todo-list\backend
.\mvnw.cmd spring-boot:run
~~~

窗口 B：

~~~powershell
cd D:\github-copilot\my-todo-list\frontend
npm run dev
~~~

浏览器打开 Vite 输出的地址，通常是 http://localhost:5173。

## 4. 必须理解的知识点

### 4.1 Axios 响应有两层 data

后端返回 { "code": 0, "message": "success", "data": [] }。Axios 要这样取：

~~~js
const response = await listTodos()
const result = response.data // HTTP 响应体
const todos = result.data    // 真正的任务数组
~~~

### 4.2 ref、reactive、computed

ref(false) 适合加载状态，reactive({}) 适合表单，computed(() => ...) 适合筛选列表和统计。JavaScript 中修改 ref 要写 .value，模板中不用。

### 4.3 前端筛选不是权限控制

前端只决定显示什么，真正的权限由后端 JWT 和 TodoService 保证。不要传 userId，后端必须类似这样查询：

~~~java
todoRepository.findByIdAndUserId(id, user.getId())
~~~

### 4.4 防止重复提交

~~~js
if (saving.value) return
saving.value = true
try {
  await createTodo(payload)
} finally {
  saving.value = false
}
~~~

## 5. 修正修改密码 API

打开 frontend/src/api/auth.js，把函数改成：

~~~js
export async function changePassword(oldPassword, newPassword, confirmPassword) {
  const response = await http.post('/auth/change-password', {
    oldPassword,
    newPassword,
    confirmPassword,
  })
  return response.data
}
~~~

http.js 的 baseURL 已包含 /api，所以这里只写 /auth/change-password，最终才是 /api/auth/change-password。

## 6. 确认任务 API

将 frontend/src/api/todo.js 改为：

~~~js
import http from './http'

export const listTodos = () => http.get('/todos')
export const getTodo = (id) => http.get('/todos/' + id)
export const createTodo = (data) => http.post('/todos', data)
export const updateTodo = (id, data) => http.put('/todos/' + id, data)
export const setTodoCompleted = (id, completed) =>
  http.patch('/todos/' + id + '/complete', { completed })
export const deleteTodo = (id) => http.delete('/todos/' + id)
~~~

创建和编辑 body：

~~~json
{
  "title": "学习 Vue",
  "description": "完成 M6 手册",
  "important": true,
  "dueDate": "2026-09-30"
}
~~~

dueDate 只传 YYYY-MM-DD；id、userId、createdAt、updatedAt 都由后端决定。

## 7. 实现 TodoView.vue

当前文件只有占位文字。请整体替换它。下面是核心 script setup 实现；表单弹窗、详情抽屉和任务卡片可以直接按后面的模板接入。

~~~js
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTodo, deleteTodo, getTodo, listTodos,
  setTodoCompleted, updateTodo } from '../api/todo'

const route = useRoute()
const todos = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const selectedTodo = ref(null)
const editingId = ref(null)
const emptyForm = () => ({ title: '', description: '', important: false, dueDate: '' })
const form = reactive(emptyForm())
const pageFilter = computed(() => route.query.filter || 'all')

function isToday(date) {
  if (!date) return false
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return date === now.getFullYear() + '-' + month + '-' + day
}

const pageTodos = computed(() => todos.value.filter(todo => {
  if (pageFilter.value === 'today') return isToday(todo.dueDate)
  if (pageFilter.value === 'important') return todo.important
  return true
}))

const visibleTodos = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return pageTodos.value.filter(todo => {
    if (statusFilter.value === 'active' && todo.completed) return false
    if (statusFilter.value === 'completed' && !todo.completed) return false
    if (!text) return true
    return [todo.title, todo.description].filter(Boolean)
      .some(value => value.toLowerCase().includes(text))
  }).sort((a, b) => {
    if (a.completed !== b.completed) return a.completed ? 1 : -1
    if (a.dueDate && !b.dueDate) return -1
    if (!a.dueDate && b.dueDate) return 1
    if (a.dueDate !== b.dueDate) return a.dueDate.localeCompare(b.dueDate)
    return (b.updatedAt || '').localeCompare(a.updatedAt || '')
  })
})

const statistics = computed(() => {
  const total = todos.value.length
  const completed = todos.value.filter(todo => todo.completed).length
  return {
    active: total - completed,
    today: todos.value.filter(todo => isToday(todo.dueDate)).length,
    completed,
    rate: total ? Math.round(completed / total * 100) + '%' : 'N/A',
  }
})

function errorText(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadTodos() {
  loading.value = true
  try {
    const result = (await listTodos()).data
    if (result.code !== 0) throw new Error(result.message || '加载失败')
    todos.value = result.data || []
  } catch (error) {
    ElMessage.error(errorText(error, '加载任务失败'))
  } finally {
    loading.value = false
  }
}

async function saveTodo() {
  if (saving.value) return
  if (!form.title.trim()) return ElMessage.warning('标题不能为空')
  saving.value = true
  const payload = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    important: Boolean(form.important),
    dueDate: form.dueDate || null,
  }
  try {
    const response = editingId.value
      ? await updateTodo(editingId.value, payload)
      : await createTodo(payload)
    const result = response.data
    if (result.code !== 0) throw new Error(result.message || '保存失败')
    dialogVisible.value = false
    ElMessage.success(editingId.value ? '任务已更新' : '任务已创建')
    await loadTodos()
  } catch (error) {
    ElMessage.error(errorText(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleCompleted(todo) {
  try {
    const result = (await setTodoCompleted(todo.id, !todo.completed)).data
    if (result.code !== 0) throw new Error(result.message || '更新失败')
    Object.assign(todo, result.data)
  } catch (error) {
    ElMessage.error(errorText(error, '更新状态失败'))
  }
}

async function removeTodo(todo) {
  try {
    await ElMessageBox.confirm('确定永久删除“' + todo.title + '”吗？', '删除确认')
    await deleteTodo(todo.id) // 204，没有响应 body
    ElMessage.success('任务已删除')
    await loadTodos()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close')
      ElMessage.error(errorText(error, '删除失败'))
  }
}

watch(pageFilter, () => { statusFilter.value = 'all'; keyword.value = '' })
onMounted(loadTodos)
~~~

模板至少要把这些状态绑定到组件：

~~~vue
<el-input v-model="keyword" clearable placeholder="搜索标题或描述" />
<el-radio-group v-model="statusFilter">
  <el-radio-button label="all">全部</el-radio-button>
  <el-radio-button label="active">进行中</el-radio-button>
  <el-radio-button label="completed">已完成</el-radio-button>
</el-radio-group>
<div v-loading="loading">
  <el-empty v-if="!loading && visibleTodos.length === 0" description="没有符合条件的任务" />
  <el-card v-for="todo in visibleTodos" :key="todo.id">
    <el-checkbox :model-value="todo.completed" @change="toggleCompleted(todo)" />
    <span>{{ todo.title }}</span>
    <el-button link @click="removeTodo(todo)">删除</el-button>
  </el-card>
</div>
~~~

新建/编辑使用 el-dialog，详情使用 el-drawer：

~~~vue
<el-dialog v-model="dialogVisible" title="新建或编辑任务">
  <el-input v-model="form.title" maxlength="200" />
  <el-input v-model="form.description" type="textarea" maxlength="2000" />
  <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" />
  <el-switch v-model="form.important" />
  <el-button type="primary" :loading="saving" @click="saveTodo">保存</el-button>
</el-dialog>

<el-drawer v-model="drawerVisible" title="任务详情">
  <template v-if="selectedTodo">
    <h2>{{ selectedTodo.title }}</h2>
    <p>{{ selectedTodo.description || '暂无描述' }}</p>
    <p>状态：{{ selectedTodo.completed ? '已完成' : '进行中' }}</p>
    <p>截止日期：{{ selectedTodo.dueDate || '未设置' }}</p>
  </template>
</el-drawer>
~~~

打开编辑前用 Object.assign(form, todo) 的思路填充表单，不要给 reactive 对象重新赋值；详情打开时调用 getTodo(id)，取 response.data.data 后再显示抽屉。

### 7.1 TodoView.vue 完整代码

如果你不想把上面的代码片段逐段拼接，可以直接将下面的完整内容复制，覆盖文件：

~~~vue
<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../layouts/AppLayout.vue'
import {
  createTodo,
  deleteTodo,
  getTodo,
  listTodos,
  setTodoCompleted,
  updateTodo,
} from '../api/todo'

const route = useRoute()
const todos = ref([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const statusFilter = ref('all')
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const selectedTodo = ref(null)
const editingId = ref(null)

const emptyForm = () => ({
  title: '',
  description: '',
  important: false,
  dueDate: '',
})
const form = reactive(emptyForm())
const pageFilter = computed(() => route.query.filter || 'all')

function isToday(date) {
  if (!date) return false
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return date === now.getFullYear() + '-' + month + '-' + day
}

const pageTodos = computed(() => todos.value.filter((todo) => {
  if (pageFilter.value === 'today') return isToday(todo.dueDate)
  if (pageFilter.value === 'important') return todo.important
  return true
}))

const visibleTodos = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return pageTodos.value.filter((todo) => {
    if (statusFilter.value === 'active' && todo.completed) return false
    if (statusFilter.value === 'completed' && !todo.completed) return false
    if (!text) return true
    return [todo.title, todo.description]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(text))
  }).sort((a, b) => {
    if (a.completed !== b.completed) return a.completed ? 1 : -1
    if (a.dueDate && !b.dueDate) return -1
    if (!a.dueDate && b.dueDate) return 1
    if (a.dueDate !== b.dueDate) return a.dueDate.localeCompare(b.dueDate)
    return (b.updatedAt || '').localeCompare(a.updatedAt || '')
  })
})

const statistics = computed(() => {
  const total = todos.value.length
  const completed = todos.value.filter((todo) => todo.completed).length
  return {
    active: total - completed,
    today: todos.value.filter((todo) => isToday(todo.dueDate)).length,
    completed,
    rate: total ? Math.round(completed / total * 100) + '%' : 'N/A',
  }
})

function errorText(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadTodos() {
  loading.value = true
  try {
    const result = (await listTodos()).data
    if (result.code !== 0) throw new Error(result.message || '加载失败')
    todos.value = result.data || []
  } catch (error) {
    ElMessage.error(errorText(error, '加载任务失败'))
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEditDialog(todo) {
  editingId.value = todo.id
  Object.assign(form, {
    title: todo.title,
    description: todo.description || '',
    important: todo.important,
    dueDate: todo.dueDate || '',
  })
  dialogVisible.value = true
}

async function openDetail(todo) {
  try {
    const result = (await getTodo(todo.id)).data
    if (result.code !== 0) throw new Error(result.message || '详情加载失败')
    selectedTodo.value = result.data
    drawerVisible.value = true
  } catch (error) {
    ElMessage.error(errorText(error, '详情加载失败'))
  }
}

async function saveTodo() {
  if (saving.value) return
  if (!form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  saving.value = true
  const payload = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    important: Boolean(form.important),
    dueDate: form.dueDate || null,
  }
  try {
    const response = editingId.value
      ? await updateTodo(editingId.value, payload)
      : await createTodo(payload)
    const result = response.data
    if (result.code !== 0) throw new Error(result.message || '保存失败')
    dialogVisible.value = false
    ElMessage.success(editingId.value ? '任务已更新' : '任务已创建')
    await loadTodos()
  } catch (error) {
    ElMessage.error(errorText(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleCompleted(todo) {
  try {
    const result = (await setTodoCompleted(todo.id, !todo.completed)).data
    if (result.code !== 0) throw new Error(result.message || '更新失败')
    Object.assign(todo, result.data)
  } catch (error) {
    ElMessage.error(errorText(error, '更新状态失败'))
  }
}

async function removeTodo(todo) {
  try {
    await ElMessageBox.confirm(
      '确定永久删除“' + todo.title + '”吗？',
      '删除确认',
      { type: 'warning', confirmButtonText: '永久删除', cancelButtonText: '取消' },
    )
    await deleteTodo(todo.id)
    ElMessage.success('任务已删除')
    await loadTodos()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(errorText(error, '删除失败'))
    }
  }
}

watch(pageFilter, () => {
  statusFilter.value = 'all'
  keyword.value = ''
})

onMounted(loadTodos)
</script>

<template>
  <AppLayout>
    <section class="page-header">
      <div>
        <p class="eyebrow">WORKSPACE / {{ pageFilter.toUpperCase() }}</p>
        <h1>{{ pageFilter === 'today' ? '今天' : pageFilter === 'important' ? '重要事项' : '我的任务' }}</h1>
      </div>
      <el-button type="primary" @click="openCreateDialog">＋ 添加任务</el-button>
    </section>

    <section class="stats-grid">
      <el-card><span>未完成</span><strong>{{ statistics.active }}</strong></el-card>
      <el-card><span>今日到期</span><strong>{{ statistics.today }}</strong></el-card>
      <el-card><span>已完成</span><strong>{{ statistics.completed }}</strong></el-card>
      <el-card><span>完成率</span><strong>{{ statistics.rate }}</strong></el-card>
    </section>

    <section class="toolbar">
      <el-radio-group v-model="statusFilter">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="active">进行中</el-radio-button>
        <el-radio-button label="completed">已完成</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索标题或描述"
        class="search-input"
      />
    </section>

    <div v-loading="loading" class="todo-list">
      <el-empty
        v-if="!loading && visibleTodos.length === 0"
        description="没有符合条件的任务"
      />
      <el-card
        v-for="todo in visibleTodos"
        :key="todo.id"
        class="todo-card"
        shadow="never"
      >
        <div class="todo-main" @click="openDetail(todo)">
          <el-checkbox
            :model-value="todo.completed"
            @click.stop
            @change="toggleCompleted(todo)"
          />
          <div class="todo-copy">
            <h3 :class="{ completed: todo.completed }">{{ todo.title }}</h3>
            <p v-if="todo.description">{{ todo.description }}</p>
            <small>{{ todo.dueDate ? '截止 ' + todo.dueDate : '未设置截止日期' }}</small>
          </div>
          <el-tag v-if="todo.important" type="warning">重要</el-tag>
          <el-tag :type="todo.completed ? 'success' : 'info'">
            {{ todo.completed ? '已完成' : '进行中' }}
          </el-tag>
        </div>
        <div class="todo-actions">
          <el-button link @click="openEditDialog(todo)">编辑</el-button>
          <el-button link type="danger" @click="removeTodo(todo)">删除</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑任务' : '新建任务'"
      width="520px"
    >
      <el-form label-position="top" @submit.prevent="saveTodo">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" maxlength="2000" :rows="4" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="form.dueDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="重要事项">
          <el-switch v-model="form.important" />
        </el-form-item>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-drawer v-model="drawerVisible" title="任务详情" size="420px">
      <template v-if="selectedTodo">
        <h2>{{ selectedTodo.title }}</h2>
        <p>{{ selectedTodo.description || '暂无描述' }}</p>
        <p>状态：{{ selectedTodo.completed ? '已完成' : '进行中' }}</p>
        <p>重要：{{ selectedTodo.important ? '是' : '否' }}</p>
        <p>截止日期：{{ selectedTodo.dueDate || '未设置' }}</p>
        <p>创建时间：{{ selectedTodo.createdAt }}</p>
        <p>更新时间：{{ selectedTodo.updatedAt }}</p>
      </template>
    </el-drawer>
  </AppLayout>
</template>
~~~

复制后保存文件，再运行 npm run dev。若页面出现任务卡片但样式不够美观，再执行第 8 节的 CSS 追加步骤。

## 8. 手工联调顺序

1. 访问 /todos，未登录应跳转 /login。
2. 注册 alice，用至少 6 位密码登录。
3. F12 → Network，找到 GET /api/todos，检查请求头有 Authorization: Bearer eyJ...。
4. 新建任务；再新建重要且今天到期的任务，检查两个导航筛选。
5. 搜索标题和描述，切换进行中/已完成，检查统计仍按全部任务计算。
6. 打开详情抽屉，编辑后刷新浏览器，确认数据仍存在。
7. 删除时确认先出现二次确认；取消确认不能删除。
8. 退出登录后直接访问 /todos，应回到登录页。
9. 修改密码成功后应清除 Token，并要求用新密码登录。

Network 面板重点看 URL、Method、Authorization 和 Response。200 是普通成功，201 是创建，204 是删除，400 是参数错误，401 是未登录/Token 过期，403 是无权限，404 是路径或任务不存在，500 是后端异常。

## 9. 常见问题

- 报 undefined：通常忘了从 response.data.data 取业务数据。
- 任务返回 401：检查 localStorage 的 todo_token、Bearer 前缀和 JWT 有效期。
- 修改密码 404：确认使用 /auth/change-password。
- 删除成功却报错：204 没有响应体，不要读取删除响应的 data.code。
- 日期少一天：dueDate 直接使用 YYYY-MM-DD，不要转 UTC。
- CORS：确认后端允许来源包含 http://localhost:5173。

## 10. M6 验收清单

- [ ] 登录后请求自动带 JWT，未登录不能进首页。
- [ ] 有加载中、空列表、搜索无结果和接口失败状态。
- [ ] 新建、编辑、完成/取消完成、删除均成功。
- [ ] 删除有二次确认并正确处理 204。
- [ ] 新建/编辑是弹窗，详情是抽屉。
- [ ] 标题为空不发送请求，保存期间不能重复提交。
- [ ] 统计不受搜索和状态筛选影响；无任务时完成率为 N/A。
- [ ] 用 alice、bob 验证互相看不到对方任务。
- [ ] 刷新后数据仍从后端加载。
- [ ] 前端构建、后端测试通过。

~~~powershell
cd D:\github-copilot\my-todo-list\frontend
npm run build
cd ..\backend
.\mvnw.cmd clean test
~~~

## 11. 可选升级和提交

当前方案适合 M6 和小规模数据。要完全实现需求中的接口草案，可让后端支持：

~~~text
GET /api/todos?keyword=vue&completed=false&important=true&dueDate=today
GET /api/todos/statistics
~~~

后端要在查询中始终追加当前用户 ID，前端再把筛选条件作为 Axios params 发送。在后端接口真正实现前，不要调用不存在的 statistics，否则会 404。

提交前执行：

~~~powershell
cd D:\github-copilot\my-todo-list
git status
git diff --check
git add docs/M6_GUIDE.md frontend/src/api/auth.js frontend/src/api/todo.js frontend/src/views/TodoView.vue frontend/src/style.css
git diff --cached --stat
git commit -m "docs: add M6 frontend integration guide"
~~~

不要提交 .env.local、数据库密码、JWT 密钥、node_modules 或 dist。M6 后进入 M7，重点补充认证边界、用户隔离、过期 Token 和干净环境启动验收。
