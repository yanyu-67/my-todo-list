import http from './http'

export async function login(username,password){
    const response = await http.post('/auth/login', { username,password })
    const result = response.data
    if(result.code !== 0) throw new Error(result.message || '登录失败')
    return result.data
}

export async function register(username,password){
    const response = await http.post('/auth/register', { username,password })
    return  response.data
}

export async function changePassword(oldPassword,newPassword,confirmPassword){
    const response = await http.post('/auth/change-password', {
        oldPassword,newPassword,confirmPassword, })
    return response.data
}