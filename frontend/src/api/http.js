import axios from "axios";

const http = axios.create({
    baseURL:import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
    timeout:10000,
    headers:{'Content-Type':'application/json'},
})

http.interceptors.request.use((config) => {
    const token = localStorage.getItem('todo_token')
    if(token){
        config.headers=config.headers  || {}
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

http.interceptors.response.use(
    (respnse) =>{return respnse},
    (error) => {
        const status = error.response?.status;
        if (status === 401){
            localStorage.removeItem("todo_token");
            localStorage.removeItem("todo_username");
            if (window.location.pathname !== '/login') {
                window.location.href = "/login?reason=expired";
            }
        }
        return Promise.reject(error);
    }
)

export function checkHealth(){
    return http.get('/health')
}

export default http