import http from './http'

export async function listUsers(){
    const response = await http.get('/admin/users')
    return response.data.data
}

export async function updateUserStatus(userId,enabled){
    const response = await http.patch(`/admin/users/${userId}/status`,{enabled})
    return response.data.data
}

export async function updateUserRole(userId,role){
    const response = await http.patch(`/admin/users/${userId}/role`,{role})
    return response.data.data
}

export async function deleteUser(userId){
    const response = await http.delete(`/admin/users/${userId}`)
    return response.data
}
