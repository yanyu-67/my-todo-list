import http from "./http";

export const listTodos = () => http.get('/todos');
export const getTodo = (id) => http.get('/todos/' + id);
export const createTodo = (data) => http.post('/todos', data);
export const updateTodo = (id,data) => http.put('/todos/' + id, data);
export const setTodoCompleted = (id,completed) =>
    http.patch('/todos/' + id + '/complete',{ completed })
export const deleteTodo = (id) => http.delete('/todos/' + id,);