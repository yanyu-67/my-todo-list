<script setup>
import {ref} from 'vue'
import {login} from "./api/auth";

const username = ref("");
const password = ref("");
const errorMessage = ref("");

async function handleLogin(){
    try{
        errorMessage.value="";

        await  login(
            username.value,password.value
        );
        window.location.href = "/todos";
    }catch (error){
        errorMessage.value = error.response?.data?.message || error.message || "登录失败";
    }

}
</script>

<template>
  <main>
      <h1>登录</h1>
      <input
              v-model="username"
              placeholder="用户名"
      />
      <input
              v-model="password"
              type="password"
              placeholder="密码"
      />
      <button @click="handleLogin">
          登录
      </button>
      <p v-if="errorMessage">
          {{errorMessage}}
      </p>
  </main>
</template>
