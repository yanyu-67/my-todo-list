<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { changePassword } from "../api/auth";
import { useAuthStore } from "../stores/auth";
import AppLayout from "../layouts/AppLayout.vue";

const router = useRouter();
const authStore = useAuthStore();
const oldPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

async function submit(){
  if (newPassword.value.length < 6) return ElMessage.warning('新密码至少6位')
  if (newPassword.value !== confirmPassword.value) return  ElMessage.warning('两次密码不一致')
  try {
    await changePassword(oldPassword.value,newPassword.value,confirmPassword.value)
    authStore.clearLogin()
    ElMessage.success('密码已修改，请重新登录')
    await router.push('/login')
  }catch(error){
    ElMessage.error(error.response?.data?.message || '修改密码失败')
  }
}
</script>

<template>
  <AppLayout>
    <el-card class="form-card"><h1>修改密码</h1>
      <el-form @submit.prevent="submit">
        <el-form-item label="旧密码"><el-input v-model="oldPassword" type="password" show-password /> </el-form-item>
        <el-form-item label="新密码"><el-input v-model="newPassword" type="password" show-password /> </el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" type="password" show-password /> </el-form-item>
        <el-button type="primary" native-type="submit">保存并重新登录</el-button>
      </el-form>
    </el-card>
  </AppLayout>
</template>

