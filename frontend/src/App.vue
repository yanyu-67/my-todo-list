<script setup>
import {ref} from 'vue'
import {checkHealth} from "./api/http";
const result = ref('尚未检查')
async function testBackend(){
  try {
    result.value=JSON.stringify((await checkHealth()).data)
  }catch (error){
    result.value=`请求失败：${error.message}`
  }
}
</script>

<template>
  <main><h1>我的待办事项</h1><button @click="testBackend">测试后端连接</button><p>{{result}}</p></main>
</template>
