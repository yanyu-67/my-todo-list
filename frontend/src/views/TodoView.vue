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

const viewMeta = computed(() => {
  const meta = {
    all: {
      title: '我的任务',
      scope: 'ALL',
      subtitle: '总览所有任务，把下一步排清楚。',
      introTitle: '任务总览',
      introText: '按完成状态、截止日期和更新时间整理，适合做一次完整梳理。',
      emptyText: '还没有任务，先添加一个待办吧',
      accentLabel: 'Workspace',
    },
    today: {
      title: '今天',
      scope: 'TODAY',
      subtitle: '只看今天要收尾的事，保持节奏轻一点。',
      introTitle: '今日焦点',
      introText: '这里只保留截止日期为今天的任务，用来快速确认今天要推进什么。',
      emptyText: '今天没有到期任务',
      accentLabel: 'Daily',
    },
    important: {
      title: '重要事项',
      scope: 'IMPORTANT',
      subtitle: '把最关键的任务单独拎出来，先处理高价值事项。',
      introTitle: '重要清单',
      introText: '这些任务被标记为重要，适合放在每日计划的最前面。',
      emptyText: '暂无重要任务',
      accentLabel: 'Priority',
    },
  }

  return meta[pageFilter.value] || meta.all
})

function isToday(date) {
  if (!date) return false

  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const today = `${now.getFullYear()}-${month}-${day}`

  return date === today
}

const pageTodos = computed(() => {
  return todos.value.filter((todo) => {
    if (pageFilter.value === 'today') {
      return isToday(todo.dueDate)
    }

    if (pageFilter.value === 'important') {
      return todo.important
    }

    return true
  })
})

const visibleTodos = computed(() => {
  const text = keyword.value.trim().toLowerCase()

  return pageTodos.value
      .filter((todo) => {
        if (statusFilter.value === 'active' && todo.completed) {
          return false
        }

        if (statusFilter.value === 'completed' && !todo.completed) {
          return false
        }

        if (!text) {
          return true
        }

        return [todo.title, todo.description]
            .filter(Boolean)
            .some((value) => value.toLowerCase().includes(text))
      })
      .sort((a, b) => {
        if (a.completed !== b.completed) {
          return a.completed ? 1 : -1
        }

        if (a.dueDate && !b.dueDate) {
          return -1
        }

        if (!a.dueDate && b.dueDate) {
          return 1
        }

        if (a.dueDate !== b.dueDate) {
          return (a.dueDate || '').localeCompare(b.dueDate || '')
        }

        return (b.updatedAt || '').localeCompare(a.updatedAt || '')
      })
})

const statusCounts = computed(() => {
  const completed = pageTodos.value.filter((todo) => todo.completed).length
  const active = pageTodos.value.length - completed

  return {
    all: pageTodos.value.length,
    active,
    completed,
  }
})

const statistics = computed(() => {
  const total = todos.value.length
  const completed = todos.value.filter((todo) => todo.completed).length
  const active = total - completed
  const today = todos.value.filter((todo) => isToday(todo.dueDate)).length
  const important = todos.value.filter((todo) => todo.important).length
  const scopedTotal = pageTodos.value.length
  const scopedCompleted = pageTodos.value.filter((todo) => todo.completed).length
  const scopedActive = scopedTotal - scopedCompleted
  const scopedRate = scopedTotal
      ? `${Math.round((scopedCompleted / scopedTotal) * 100)}%`
      : 'N/A'

  if (pageFilter.value === 'today') {
    return [
      { label: '今日任务', value: scopedTotal },
      { label: '待推进', value: scopedActive },
      { label: '已收尾', value: scopedCompleted },
      { label: '今日完成率', value: scopedRate },
    ]
  }

  if (pageFilter.value === 'important') {
    return [
      { label: '重要任务', value: important },
      { label: '待处理', value: scopedActive },
      { label: '今日到期', value: pageTodos.value.filter((todo) => isToday(todo.dueDate)).length },
      { label: '完成率', value: scopedRate },
    ]
  }

  return [
    { label: '未完成', value: active },
    { label: '今日到期', value: today },
    { label: '已完成', value: completed },
    { label: '完成率', value: total ? `${Math.round((completed / total) * 100)}%` : 'N/A' },
  ]
})

function errorText(error, fallback) {
  return error.response?.data?.message || error.message || fallback
}

async function loadTodos() {
  loading.value = true

  try {
    const result = (await listTodos()).data

    if (result.code !== 0) {
      throw new Error(result.message || '加载失败')
    }

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
    important: Boolean(todo.important),
    dueDate: todo.dueDate || '',
  })

  dialogVisible.value = true
}

async function openDetail(todo) {
  try {
    const result = (await getTodo(todo.id)).data

    if (result.code !== 0) {
      throw new Error(result.message || '详情加载失败')
    }

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

  const isEditing = Boolean(editingId.value)

  const payload = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    important: Boolean(form.important),
    dueDate: form.dueDate || null,
  }

  try {
    const response = isEditing
        ? await updateTodo(editingId.value, payload)
        : await createTodo(payload)

    const result = response.data

    if (result.code !== 0) {
      throw new Error(result.message || '保存失败')
    }

    dialogVisible.value = false

    ElMessage.success(isEditing ? '任务已更新' : '任务已创建')

    await loadTodos()
  } catch (error) {
    ElMessage.error(errorText(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleCompleted(todo) {
  try {
    const result = (
        await setTodoCompleted(todo.id, !todo.completed)
    ).data

    if (result.code !== 0) {
      throw new Error(result.message || '更新失败')
    }

    Object.assign(todo, result.data)
  } catch (error) {
    ElMessage.error(errorText(error, '更新状态失败'))
  }
}

async function removeTodo(todo) {
  try {
    await ElMessageBox.confirm(
        `确定永久删除“${todo.title}”吗？`,
        '删除确认',
        {
          type: 'warning',
          confirmButtonText: '永久删除',
          cancelButtonText: '取消',
        },
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
    <div :class="['todo-page', `todo-page--${pageFilter}`]">
      <section class="page-header">
        <div class="page-title-block">
          <p class="eyebrow">
            PERSONAL WORKSPACE / {{ viewMeta.scope }}
          </p>

          <div class="title-row">
            <h1>{{ viewMeta.title }}</h1>
            <span class="view-chip">{{ viewMeta.accentLabel }}</span>
          </div>

          <p class="muted">
            {{ viewMeta.subtitle }}
          </p>
        </div>

        <el-button type="primary" @click="openCreateDialog">
          + 添加任务
        </el-button>
      </section>

      <section class="stats-grid">
        <el-card
            v-for="item in statistics"
            :key="item.label"
            shadow="never"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </el-card>
      </section>

      <section class="view-intro">
        <div>
          <span>{{ viewMeta.accentLabel }}</span>
          <h2>{{ viewMeta.introTitle }}</h2>
          <p>{{ viewMeta.introText }}</p>
        </div>

        <strong>{{ statusCounts.all }}</strong>
      </section>

      <section class="toolbar">
        <el-radio-group v-model="statusFilter" class="status-switch">
          <el-radio-button value="all">
            全部 {{ statusCounts.all }}
          </el-radio-button>

          <el-radio-button value="active">
            进行中 {{ statusCounts.active }}
          </el-radio-button>

          <el-radio-button value="completed">
            已完成 {{ statusCounts.completed }}
          </el-radio-button>
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
            :description="viewMeta.emptyText"
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
              <h3 :class="{ completed: todo.completed }">
                {{ todo.title }}
              </h3>

              <p v-if="todo.description">
                {{ todo.description }}
              </p>

              <small>
                {{ todo.dueDate ? `截止 ${todo.dueDate}` : '未设置截止日期' }}
              </small>
            </div>

            <el-tag v-if="todo.important" type="warning">
              重要
            </el-tag>

            <el-tag :type="todo.completed ? 'success' : 'info'">
              {{ todo.completed ? '已完成' : '进行中' }}
            </el-tag>
          </div>

          <div class="todo-actions">
            <el-button link @click="openEditDialog(todo)">
              编辑
            </el-button>

            <el-button link type="danger" @click="removeTodo(todo)">
              删除
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog
        v-model="dialogVisible"
        :title="editingId ? '编辑任务' : '新建任务'"
        width="520px"
    >
      <el-form label-position="top" @submit.prevent="saveTodo">
        <el-form-item label="标题" required>
          <el-input
              v-model="form.title"
              maxlength="200"
              show-word-limit
              placeholder="例如：完成产品设计"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
              v-model="form.description"
              type="textarea"
              maxlength="2000"
              :rows="4"
              placeholder="添加任务描述"
          />
        </el-form-item>

        <el-form-item label="截止日期">
          <el-input
              v-model="form.dueDate"
              type="date"
          />
        </el-form-item>

        <el-form-item label="重要事项">
          <el-switch
              v-model="form.important"
              active-text="标记为重要"
          />
        </el-form-item>

        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">
            取消
          </el-button>

          <el-button
              type="primary"
              native-type="submit"
              :loading="saving"
          >
            保存
          </el-button>
        </div>
      </el-form>
    </el-dialog>

    <el-drawer
        v-model="drawerVisible"
        title="任务详情"
        size="420px"
    >
      <template v-if="selectedTodo">
        <h2>{{ selectedTodo.title }}</h2>

        <p>
          {{ selectedTodo.description || '暂无描述' }}
        </p>

        <p>
          状态：
          {{ selectedTodo.completed ? '已完成' : '进行中' }}
        </p>

        <p>
          重要：
          {{ selectedTodo.important ? '是' : '否' }}
        </p>

        <p>
          截止日期：
          {{ selectedTodo.dueDate || '未设置' }}
        </p>

        <p>
          创建时间：
          {{ selectedTodo.createdAt || '暂无' }}
        </p>

        <p>
          更新时间：
          {{ selectedTodo.updatedAt || '暂无' }}
        </p>
      </template>
    </el-drawer>
  </AppLayout>
</template>
