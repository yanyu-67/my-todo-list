import { computed, ref } from "vue"
import { defineStore} from "pinia"

const TOKEN_KEY = 'todo_token'
const USERNAME_KEY = 'todo_username'
const ROLE_KEY = 'todo_role'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem(TOKEN_KEY) || '')
    const username = ref(localStorage.getItem(USERNAME_KEY) || '')
    const isLoggedIn = computed(() => Boolean(token.value))
    const role = ref(localStorage.getItem(ROLE_KEY) || '')
    const isAdmin = computed(() => role.value === 'ADMIN')

    function setLogin(loginData){
        token.value = loginData.token
        username.value=loginData.username
        role.value=loginData.role
        localStorage.setItem(TOKEN_KEY,token.value)
        localStorage.setItem(USERNAME_KEY,username.value)
        localStorage.setItem(ROLE_KEY,role.value)
    }

    function clearLogin(){
        token.value = ''
        username.value = ''
        role.value = ''
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USERNAME_KEY)
        localStorage.removeItem(ROLE_KEY)
    }
    return { token,username,role,isLoggedIn,isAdmin,setLogin,clearLogin }
})