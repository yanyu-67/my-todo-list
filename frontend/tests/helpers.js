import { expect } from "@playwright/test";

export const testUser = {
    username: process.env.TEST_USER || 'TEST_USER',
    password: process.env.TEST_PASSWORD || 'TEST_PASSWORD',
}

export const testAdmin = {
    username: process.env.TEST_ADMIN || 'TEST_ADMIN',
    password: process.env.TEST_ADMIN_PASSWORD || 'TEST_ADMIN_PASSWORD',
}

export const apiBase =
    process.env.TEST_API_BASE_URL || 'http://localhost:8080/api';

export function uniqueName(prefix = 'pw'){
    return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2,7)}`
}

export async function clearBrowserState(page){
    // 先进入项目同源页面，再访问 localStorage，避免 about:blank 的安全限制。
    await page.goto('/login')

    await page.evaluate(() => {
        localStorage.clear()
        sessionStorage.clear()
    })
}
export async function loginByUi(page,account){
    await page.goto('/login')
    await page.getByLabel('用户名').fill(account.username)
    await page.getByLabel('密码').fill(account.password)
    await page.getByRole('button',{name:'登录'}).click()
    await expect(page).toHaveURL(/\/todos/)
}

export async function registerByApi(request,username, password){
    const response = await request.post(`${apiBase}/auth/register`,{
        data:{
            username,
            password,
        },
    })
    expect(response.status()).toBe(201)
}

export async function createTodoByApi(request,token,data){
    const response = await request.post(`${apiBase}/todos`,{
        headers:{
            Authorization:`Bearer ${token}`,
        },
        data,
    })
    expect(response.ok()).toBeTruthy()
    return response.json()
}
