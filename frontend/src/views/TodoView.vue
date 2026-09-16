<script setup>
import { ref } from 'vue';
import AppLayout from "../layouts/AppLayout.vue";

const activeFilter = ref('all')
const keyword = ref('')
</script>

<template>
  <AppLayout>
    <section class="page-header">
      <div><p class="eyebrow">WORKSPACE / TODAY</p><h1>我的任务</h1></div>
      <el-button type="primary">+ 添加任务</el-button>
    </section>
    <section class="toolbar">
      <el-radio-group v-model="activeFilter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="active">进行中</el-radio-button>
        <el-radio-button value="completed">已完成</el-radio-button>
      </el-radio-group>
      <el-radio-group v-model="keyword" clearable placeholder="搜索任务标题或描述" class="search-input" />
    </section>
    <el-empty description="M6 将在这里加载任务列表" />
  </AppLayout>
</template>