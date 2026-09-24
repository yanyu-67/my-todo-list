import { createRouter,createWebHistory} from 'vue-router'
import { useAuthStore} from "../stores/auth";

const router = createRouter({
    history:createWebHistory(),
    routes: [
        { path: '/', redirect: '/todos'},
        { path: '/login',name: 'login',component: () => import('../views/LoginView.vue')},
        { path: '/register',name: 'register',component: () => import('../views/RegisterView.vue')},
        {
            path: '/todos',
            name: 'todos',
            component: () => import('../views/TodoView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/change-password',
            name: 'change-password',
            component: () => import('../views/ChangePasswordView.vue'),
            meta: { requiresAuth: true },
        },
        {
            path:'/admin/users',
            name:'admin-users',
            component: () => import('../views/AdminUsersView.vue'),
            meta: { requiresAuth: true,requiresAdmin: true },
        }
    ],
})

router.beforeEach((to) => {
    const authStore = useAuthStore()
    if(to.meta.requiresAuth && !authStore.isLoggedIn){
        return { name: 'login', query: { redirect: to.fullPath }}
    }
    if(to.meta.requiresAdmin && !authStore.isAdmin){
        return {name:'todos'}
    }
    if((to.name === 'login' || to.name === 'register') && authStore.isLoggedIn){
        return { name: 'todos' }
    }

})

export default router
