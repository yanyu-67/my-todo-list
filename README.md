# 我的待办事项（my-todo-list）

一个前后端分离的 TODO 练手项目，适合测试开发与 Java Web 学习。

## 技术栈

- 后端：Java 17、Spring Boot 4.1.1、Spring Security、Spring Data JPA、JWT、PostgreSQL
- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios

## 目录结构

- `backend/`：后端服务
- `frontend/`：前端应用
- `docs/`：开发文档

## 当前功能

- 用户注册、登录、修改密码
- JWT 鉴权与接口权限控制
- Todo 列表、详情、新增、编辑、完成状态切换、删除

## 快速开始

### 1) 配置 PostgreSQL（本地）

在 `backend/src/main/resources/` 下准备本地配置（建议 `application-local.yaml`），示例：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/todo_db
    username: postgres
    password: 你的数据库密码
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
```

> 说明：当前仓库仅有 `application.yaml`，未提交数据库账号密码。请按本地环境配置。

### 2) 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

### 3) 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 4) 访问地址

- 前端：`http://localhost:5173`
- 后端健康检查：`http://localhost:8080/api/health`

## 测试命令

### 后端测试

```bash
cd backend
./mvnw test
```

### 前端构建校验

```bash
cd frontend
npm run build
```

## 文档入口

- 总导航：`docs/00-文档导航.md`
- 知识库：`docs/90-知识库/00-知识库导航.md`
