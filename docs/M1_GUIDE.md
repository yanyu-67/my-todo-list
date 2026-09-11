# M1：开发环境与工程骨架——新手手把手指导

> 本手册根据 `REQUIREMENTS.md`、`README.md` 和 `MILESTONES.md` 编写。M1 只搭建可运行骨架，不实现完整登录、JWT 或任务 CRUD。

## 1. 先理解项目结构

这是一个前后端分离项目：

```text
浏览器 ←→ Vue 3 + Vite 前端（5173）
             ↓ Axios / HTTP
          Spring Boot 后端（8080）
             ↓ Spring Data JPA
          PostgreSQL 数据库（5432）
```

- 前端负责页面和交互。
- 后端负责接口、业务规则、身份认证和权限。不能只依赖前端限制权限。
- PostgreSQL 负责长期保存用户和待办事项。
- Git 保存每一个可恢复的开发版本。

本期不安装 Docker、Redis、消息队列、云服务或 CI/CD，也不实现管理员、协作、共享和移动端专适配。

M1 完成后应有：

```text
my-todo-list/
├─ backend/
│  ├─ pom.xml
│  └─ src/main/java/com/example/todo/
│     ├─ TodoApplication.java
│     ├─ controller/ service/ repository/
│     ├─ entity/ dto/ security/ exception/ config/
├─ frontend/
│  ├─ package.json
│  └─ src/views/ components/ stores/ api/ router/ styles/
├─ docs/
└─ .gitignore
```

## 2. 检查开发工具

在 Windows PowerShell 执行：

```powershell
cd D:\github-copilot\my-todo-list
java -version
mvn -version
node -v
npm -v
psql --version
git --version
```

要求如下：

| 工具 | 要求 | 用途 |
| --- | --- | --- |
| Java | 17 | 运行后端 |
| Maven | 3.9+ | 编译、下载依赖、启动后端 |
| Node.js | LTS | 运行 Vue 工具链 |
| npm | 随 Node.js 安装 | 安装前端依赖 |
| PostgreSQL | 15+ | 保存数据 |
| Git | 可用即可 | 版本管理 |

`java -version` 的主版本必须是 17。如果提示“不是命令”，先安装软件或修复 PATH。

## 3. 创建本地数据库

先在 Windows“服务”或 pgAdmin 中启动 PostgreSQL。然后执行：

```powershell
psql -U postgres
```

在出现的 `postgres=#` 提示符中逐行执行：

```sql
CREATE USER todo_dev WITH PASSWORD '替换为自己的本机密码';
CREATE DATABASE todo_db OWNER todo_dev;
\q
```

验证连接：

```powershell
psql -U todo_dev -d todo_db -h localhost
```

看到 `todo_db=>` 即成功，输入 `\q` 退出。如果提示对象已存在，不要重复创建，直接验证连接即可。

## 4. 创建 Spring Boot 后端

打开 <https://start.spring.io>，选择：

| 选项 | 值 |
| --- | --- |
| Project | Maven |
| Language | Java |
| Spring Boot | 3.x 稳定版本 |
| Group | `com.example` |
| Artifact / Name | `todo-backend` |
| Packaging | Jar |
| Java | 21 |

添加依赖：Spring Web、Validation、Spring Data JPA、Spring Security、PostgreSQL Driver；Spring Boot Actuator 可选。

点击 Generate，解压并将文件夹改名为 `backend`，放到项目根目录：

```text
D:\github-copilot\my-todo-list\backend
```

### 4.1 预留 OpenAPI 和 JWT

M1 不实现认证，但需求要求后续使用 JWT，并保留 OpenAPI 文档能力。在 `backend/pom.xml` 的 `<dependencies>` 中添加 OpenAPI 依赖；JWT 可以等 M4 添加：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>填写与当前 Spring Boot 4.1.1 兼容的稳定版本</version>
</dependency>
```

M4 使用 JJWT 时需要 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 三个依赖，并统一版本。不要从多个旧教程复制冲突的版本。

### 4.2 配置数据库

创建 `backend/src/main/resources/application-local.yml`：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/todo_db
    username: todo_dev
    password: 替换为自己的本机密码
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
app:
  frontend-url: http://localhost:5173
```

创建或修改 `application.yml`：

```yaml
spring:
  profiles:
    active: local
```

`ddl-auto: update` 只用于本地开发骨架。密码放在本机配置，不能提交到 Git。

### 4.3 添加健康检查接口

创建 `backend/src/main/java/com/example/todo/controller/HealthController.java`：

```java
package com.example.todo.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("code", 0, "message", "success",
                "data", Map.of("status", "UP"));
    }
}
```

启动后端：

```powershell
cd D:\github-copilot\my-todo-list\backend
mvn spring-boot:run
```

另开 PowerShell 检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

看到 `code : 0`、`status : UP` 即成功。8080 被占用就更换端口；数据库认证失败就检查用户名和密码；Connection refused 通常是 PostgreSQL 没启动。

## 5. 创建 Vue 3 + Vite 前端

```powershell
cd D:\github-copilot\my-todo-list
npm create vite@latest frontend -- --template vue
cd frontend
npm install
npm install vue-router pinia element-plus axios
npm run dev
```

打开命令输出的地址，通常是 `http://localhost:5173`。看到默认 Vue 页面，说明前端骨架成功。

在 `frontend/src` 下建立这些目录：`api/`（Axios）、`components/`（组件）、`router/`（路由）、`stores/`（Pinia）、`views/`（页面）、`styles/`（样式）。M1 只需建立目录，M5 再实现页面。

### 5.1 配置前端 API 地址

创建 `frontend/.env.example`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

复制为本机配置：

```powershell
Copy-Item .env.example .env.local
```

创建 `frontend/src/api/http.js`：

```javascript
import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

export function checkHealth() {
  return http.get('/health')
}

export default http
```

把 `src/App.vue` 暂时改为测试页面：

```vue
<script setup>
import { ref } from 'vue'
import { checkHealth } from './api/http'
const result = ref('尚未检查')
async function testBackend() {
  try { result.value = JSON.stringify((await checkHealth()).data) }
  catch (error) { result.value = `请求失败：${error.message}` }
}
</script>
<template>
  <main><h1>我的待办事项</h1><button @click="testBackend">测试后端连接</button><p>{{ result }}</p></main>
</template>
```

点击按钮应看到包含 `"status":"UP"` 的 JSON，这证明 Axios 能访问后端。

## 6. 配置开发环境跨域

5173 和 8080 是不同源，创建 `backend/src/main/java/com/example/todo/config/WebConfig.java`：

```java
package com.example.todo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

不要使用 `allowedOrigins("*")`。M4 加入 Spring Security 后，还要在 Security 配置中启用 CORS；生产环境应改成正式域名。

## 7. Git 和 `.gitignore`

在项目根目录创建 `.gitignore`：

```gitignore
backend/target/
frontend/node_modules/
frontend/dist/
backend/src/main/resources/application-local.yml
frontend/.env.local
*.log
.idea/
*.iml
```

初始化并检查：

```powershell
cd D:\github-copilot\my-todo-list
git init
git status
git add .
git status
```

确认列表中没有密码配置、`.env.local`、`node_modules` 和 `target` 后提交：

```powershell
git commit -m "chore: initialize development skeleton"
```

## 8. M1 验收清单

- [ ] Java 主版本为 21，Maven、Node.js、npm、PostgreSQL、Git 可用。
- [ ] PostgreSQL 正在运行，`todo_dev` 可以连接 `todo_db`。
- [ ] 后端 `mvn spring-boot:run` 成功启动。
- [ ] `GET http://localhost:8080/api/health` 返回统一 JSON。
- [ ] 前端 `npm run dev` 成功启动并显示 Vue 页面。
- [ ] 点击测试按钮能显示后端 `UP` 响应。
- [ ] 后端已预留 `controller/service/repository/entity/dto/security/exception/config` 分层。
- [ ] 前端已预留 `views/components/stores/api/router/styles` 分层。
- [ ] CORS 只允许 `http://localhost:5173`。
- [ ] `.gitignore` 已忽略本机配置、依赖和构建产物。
- [ ] Git 提交中没有密码、Token 或本地依赖目录。

## 9. 后续里程碑怎么接上

1. M2：创建 `User`、`Todo` 实体和 JPA Repository。
2. M3：用 DTO、Service、Controller 完成任务 API。
3. M4：加入密码哈希、JWT、Spring Security 和用户数据隔离。
4. M5：完善 Router、Pinia、Axios 拦截器和 Element Plus 布局。
5. M6：完成登录、首页任务列表、弹窗、抽屉、筛选和联调。
6. M7：补齐 JUnit 5 / Spring Boot Test、README 和最终验收。

每完成一个可运行的小功能就提交一次 Git，例如 `feat: add JWT login` 或 `test: cover user data isolation`。M1 的健康接口只是连通性检查，不代表已经完成注册、登录或任务功能。

## 10. 新手避坑

1. 不要把数据库密码或 JWT 密钥提交到 Git。
2. 不要让前端传 `userId` 决定任务归属；后续必须从认证上下文取得当前用户。
3. 不要直接返回 `User` 实体；后续使用 DTO，避免暴露密码哈希。
4. 不要用静态 HTML 替代 Vue 3 + Vite。
5. 不要为了本项目额外安装 Docker、Redis 或消息队列。
6. 不要用前端路由守卫替代后端鉴权。
7. 不要为了解决 CORS 把来源永久改为 `*`。
