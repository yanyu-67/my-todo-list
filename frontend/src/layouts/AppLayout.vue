<script setup>
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

function logOut() {
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
        <el-button text @click="logOut">退出登录</el-button>
      </div>
    </el-aside>
    <el-main class="content-area">
      <slot />
    </el-main>
  </el-container>
</template>