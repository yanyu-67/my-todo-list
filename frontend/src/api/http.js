import axios from "axios";

const http = axios.create({
    baseURL:import.meta.env.VITE_API_BASE_URL,
    timeout:10000,
    headers:{'Content-Type':'application/json'},
})

export function checkHealth(){
    return http.get('/health')
}

export default http