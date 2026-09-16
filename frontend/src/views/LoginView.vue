<script setup>
import { ref } from "vue"
import { useRoute,useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { login } from "../api/auth"
import { useAuthStore } from "../stores/auth"

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const authState = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit(){
  if(!username.value.trim() || !password.value){
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try{
    const data = await login(username.value, password.value)
    authState.setLogin(data)
    await router.push(route.query.redirect || '/todos')
  }catch(error){
    ElMessage.error(error.response?.data?.message || error.message || '登录失败')
  }finally {
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
          <el-input v-model="password" type="password" show-password placeholder="至少6位" autocomplete="current-password" />
        </el-form-item>
        <el-button class="full-button" type="primary" native-type="submit" :loading="loading">登录</el-button>
      </el-form>
      <p class="auth-link">还没有账号？<router-link to="/register">立即注册</router-link></p>
    </el-card>
  </main>
</template>
