import { computed, ref } from "vue"
import { defineStore} from "pinia"

const TOKEN_KEY = 'todo_token'
const USERNAME_KEY = 'todo_username'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem(TOKEN_KEY) || '')
    const username = ref(localStorage.getItem(USERNAME_KEY) || '')
    const isLoggedIn = computed(() => Boolean(token.value))

    function setLogin(loginData){
        token.value = loginData.token
        username.value=loginData.username
        localStorage.setItem(TOKEN_KEY,token.value)
        localStorage.setItem(USERNAME_KEY,username.value)
    }

    function clearLogin(){
        token.value = ''
        username.value = ''
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USERNAME_KEY)
    }
    return { token,username,isLoggedIn,setLogin,clearLogin }
})