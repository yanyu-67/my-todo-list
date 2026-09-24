https://pinia.vuejs.org/zh/introduction.html

# 什么是 pinia

pinia是 Vue 3 官方推荐的**状态管理库**,就是帮忙在**多个页面/组件之间共享和管理数据**的

数据需要跨组件、跨页面共享时，就可以用

（如果数据只在一个组件里面，就用ref就行）

## ref

ref**是 Vue 3 里定义“响应式数据”的工具——数据变了页面自动更新。**

要是想让一个数据变了页面就跟着变就用ref包一下

``````vue
<script setup>
import { ref } from 'vue'

// 定义一个会变的数据
const count = ref(0)

// 改它（注意：要加 .value）
function add() {
  count.value++
}
</script>

<template>
  <!-- 页面上直接用，不用写 .value -->
  <p>{{ count }}</p>
  <button @click="add">加一</button>
</template>
``````

### **为什么要有 `.value`？** 

因为 `ref` 返回的是一个“包装盒”，真正的值在盒子里（`.value`）。Vue 靠这个盒子来**监听数据变化**。

能存数字、字符串、布尔、数组、对象

# 区别

- **`ref`**：在**单个组件**里用
- **Pinia**：把数据放到**全局仓库**，多个组件共享