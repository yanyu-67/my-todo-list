<script setup>
import { computed, onMounted, ref } from "vue"
import {ElMessage,ElMessageBox} from "element-plus"
import AppLayout from "../layouts/AppLayout.vue"
import { deleteUser, listUsers, updateUserRole, updateUserStatus} from "../api/admin"

const users = ref([])
const loading = ref(false)
const keyword = ref('')

const filteredUsers = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return users.value

  return users.value.filter((user) => user.username.toLowerCase().includes(text))
})

const enabledCount = computed(() => users.value.filter((user) => user.enabled).length)
const adminCount = computed(() => users.value.filter((user) => user.role === 'ADMIN').length)

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadUsers(){
  loading.value = true
  try{
    users.value=await listUsers()
  }catch(error){
    ElMessage.error(error.response?.data?.message || '加载用户列表失败')
  }finally {
    loading.value = false
  }
}

async function toggleStatus(user){
  const nextEnabled = !user.enabled
  await ElMessageBox.confirm(
      `确定要${nextEnabled ? '启用' : '禁用'}用户“${user.username}”吗？`,
      '确认操作'
  )
  try {
    await updateUserStatus(user.id,nextEnabled)
    ElMessage.success('操作成功')
    await loadUsers()
  }catch(error){
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function toggleRole(user){
  const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
  await ElMessageBox.confirm(
      `确定要将用户“${user.username}”设置为${nextRole}吗？`,
      '确认操作'
  )
  try {
    await updateUserRole(user.id,nextRole)
    ElMessage.success('操作成功')
    await loadUsers()
  }catch(error){
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function removeUser(user){
  try {
    await ElMessageBox.confirm(
        `删除用户“${user.username}”后，该用户的全部待办也会被删除，确定继续吗？`,
        '删除账号',
        {
          type: 'warning',
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
        },
    )
    await deleteUser(user.id)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(loadUsers)
</script>

<template>
  <AppLayout>
    <section class="admin-page">
    <div class="page-header admin-header">
      <div>
        <p class="eyebrow">ADMIN / ACCESS</p>
        <h1>用户管理</h1>
        <p class="muted">管理账号状态与系统访问角色。</p>
      </div>
      <el-button class="refresh-button" type="primary" :loading="loading" @click="loadUsers">刷新列表</el-button>
    </div>

    <div class="admin-stats">
      <div class="admin-stat">
        <span>全部用户</span>
        <strong>{{ users.length }}</strong>
        <small>已注册账号</small>
      </div>
      <div class="admin-stat admin-stat--green">
        <span>正常使用</span>
        <strong>{{ enabledCount }}</strong>
        <small>当前启用账号</small>
      </div>
      <div class="admin-stat admin-stat--blue">
        <span>管理员</span>
        <strong>{{ adminCount }}</strong>
        <small>拥有管理权限</small>
      </div>
    </div>

    <div class="admin-toolbar">
      <div>
        <h2>账号列表</h2>
        <span>{{ filteredUsers.length }} 个结果</span>
      </div>
      <el-input v-model="keyword" clearable class="user-search" placeholder="搜索用户名" />
    </div>

    <div class="admin-table-wrap">
    <el-table v-loading="loading" :data="filteredUsers" class="admin-table" empty-text="暂无用户">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column label="用户" min-width="220">
        <template #default="{ row }">
          <div class="user-cell">
            <span class="user-avatar">{{ row.username.slice(0, 1).toUpperCase() }}</span>
            <strong>{{ row.username }}</strong>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <span :class="['role-badge', row.role === 'ADMIN' ? 'role-badge--admin' : 'role-badge--user']">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130" >
        <template #default="{ row }">
          <span :class="['status-badge', row.enabled ? 'status-badge--enabled' : 'status-badge--disabled']">
            <i></i>{{ row.enabled ? '已启用' : '已禁用' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="350" fixed="right">
        <template #default="{ row }">
          <el-button class="table-action" size="small" @click="toggleStatus(row)">
            {{ row.enabled ? '禁用账号' : '启用账号' }}
          </el-button>
          <el-button class="table-action table-action--primary" size="small" @click="toggleRole(row)">
            {{ row.role === 'ADMIN' ? '撤销管理员' : '设为管理员' }}
          </el-button>
          <el-button class="table-action table-action--danger" size="small" @click="removeUser(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>
    </section>
  </AppLayout>
</template>

<style scoped>
.admin-page {
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
}

.admin-header {
  margin-bottom: 24px;
}

.admin-header h1 {
  margin-bottom: 6px;
}

.admin-header .muted {
  margin: 0;
}

.refresh-button {
  min-width: 106px;
}

.admin-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 28px;
}

.admin-stat {
  position: relative;
  min-height: 118px;
  padding: 20px 22px;
  overflow: hidden;
  background: #15181d;
  border: 1px solid #2a2e36;
  border-radius: 10px;
}

.admin-stat::after {
  position: absolute;
  right: -22px;
  bottom: -32px;
  width: 110px;
  height: 110px;
  content: '';
  border: 1px solid rgba(201, 164, 92, 0.2);
  border-radius: 50%;
}

.admin-stat span,
.admin-stat small {
  display: block;
  color: #969ca7;
}

.admin-stat strong {
  display: block;
  margin: 8px 0 3px;
  color: #f5f0e6;
  font-size: 30px;
  line-height: 1;
}

.admin-stat--green strong {
  color: #7fd1a3;
}

.admin-stat--blue strong {
  color: #8bb9ed;
}

.admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 12px;
}

.admin-toolbar h2 {
  margin: 0 0 4px;
  color: #f5f0e6;
  font-size: 18px;
}

.admin-toolbar span {
  color: #858b96;
  font-size: 13px;
}

.user-search {
  width: 240px;
}

.admin-table-wrap {
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid #2a2e36;
  border-radius: 10px;
  background: #15181d;
}

.admin-table {
  --el-table-bg-color: #15181d;
  --el-table-tr-bg-color: #15181d;
  --el-table-row-hover-bg-color: #1d2229;
  --el-table-header-bg-color: #1b2027;
  --el-table-border-color: #2a2e36;
  --el-table-text-color: #c8cbd1;
  --el-table-header-text-color: #8e96a3;
}

.admin-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.admin-table :deep(.el-table__header th) {
  height: 48px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.admin-table :deep(.el-table__row td) {
  height: 68px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.user-cell strong {
  color: #f0f1f2;
  font-weight: 600;
}

.user-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border: 1px solid #6b5734;
  border-radius: 50%;
  color: #e4c47a;
  background: #28231b;
  font-size: 13px;
  font-weight: 700;
}

.role-badge,
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.role-badge--admin {
  border-color: rgba(201, 164, 92, 0.35);
  color: #e4c47a;
  background: rgba(201, 164, 92, 0.12);
}

.role-badge--user {
  border-color: rgba(139, 185, 237, 0.28);
  color: #a9c9ef;
  background: rgba(139, 185, 237, 0.1);
}

.status-badge--enabled {
  color: #7fd1a3;
  background: rgba(127, 209, 163, 0.1);
}

.status-badge--disabled {
  color: #e28c8c;
  background: rgba(226, 140, 140, 0.1);
}

.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.table-action {
  border-color: #3a404b;
  color: #b1b6bf;
  background: #1b2027;
}

.table-action:hover {
  border-color: #6a7380;
  color: #f5f0e6;
  background: #252b34;
}

.table-action--primary {
  border-color: rgba(201, 164, 92, 0.45);
  color: #e4c47a;
}

.table-action--danger {
  border-color: rgba(226, 140, 140, 0.35);
  color: #e28c8c;
}

.table-action--danger:hover {
  border-color: #e28c8c;
  color: #ffd0d0;
  background: rgba(226, 140, 140, 0.14);
}

@media (max-width: 760px) {
  .admin-stats {
    grid-template-columns: 1fr;
  }

  .admin-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .user-search {
    width: 100%;
  }
}

</style>
