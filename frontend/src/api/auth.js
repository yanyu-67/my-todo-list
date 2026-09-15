import http from "./http";

export async function login(username, password) {
    const response = await http.post("/auth/login", {
        username,
        password
    });

    const result = response.data;

    if (result.code !== 0) {
        throw new Error(result.message);
    }

    localStorage.setItem("todo_token", result.data.token);
    localStorage.setItem("todo_username", result.data.username);

    return result.data;
}

export async function register(username, password) {
    const response = await http.post("/auth/register", {
        username,
        password
    });

    return response.data;
}

export function logout() {
    localStorage.removeItem("todo_token");
    localStorage.removeItem("todo_username");
    window.location.href = "/login";
}