import axios from "axios";

const http = axios.create({
    baseURL:import.meta.env.VITE_API_BASE_URL,
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
        const message =
            error.response?.data?.message || "请求失败";
        if (status === 401){
            localStorage.removeItem("todo_token");
            localStorage.removeItem("todo_username");
            alert(message || "登录已失效，请重新登录");

            window.location.href = "/login";
        }
        if(status == 403){
            alert(message || "无权访问该资源");
        }
        return Promise.reject(error);
    }
)

export function checkHealth(){
    return http.get('/health')
}

export default http