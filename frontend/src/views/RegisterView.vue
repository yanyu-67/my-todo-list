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

async function submit(){
  if(!username.value.trim()) return ElMessage.warning('用户名不能为空')
  if(password.value.length < 6) return ElMessage.warning('密码至少6位')
  if(password.value !== confirmPassword.value) return ElMessage.warning('两次密码不一致')
  loading.value = true
  try {
    const result = await register(username.value.trim(), password.value)
    if (result.code !== 0) throw new Error(result.message || '注册失败')
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  }catch(error){
    ElMessage.error(error.response?.data?.message || error.message || '注册失败')
  }finally {
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
        <el-form-item label="用户名"><el-input v-model="username" /> </el-form-item>
        <el-form-item label="密码"><el-input v-model="password" type="password" show-password /> </el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password /> </el-form-item>
        <el-button class="full-button" type="primary" native-type="submit" :loading="loading">注册</el-button>
      </el-form>
      <p class="auth-link"><router-link to="/login">返回登录</router-link></p>
    </el-card>
  </main>
</template>