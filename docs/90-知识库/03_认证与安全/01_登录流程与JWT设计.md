# 什么是 JWT，什么是 Token

**Token 是统称，JWT 是 Token 的一种具体实现格式**

Token 直译是“令牌”，在 Web 开发里，它是一段**由服务器签发、客户端保存、后续请求携带的字符串**，用来证明“你是谁”或“你有权访问”。

**JWT 是通行证的一种具体写法，把信息写进证里并签名，服务器不存底，靠验签放行。**

由

**Header（头部）**：描述令牌类型和签名算法

 **Payload（载荷）：**存放实际数据，叫 claims（声明）。分三类：

- **标准声明**：`iss`（签发者）、`exp`（过期时间）、`sub`（主题）、`iat`（签发时间）
- **公共声明**：自定义但建议避免冲突
- **私有声明**：业务自己定义的，比如 `userId`、`role`

**Signature（签名）：把前两部分 Base64 编码后用 `.` 拼起来，再用密钥和算法加密，防止篡改：**

组成

## JWT 的优缺点

**优点：**

- 无状态，服务器不用存 Session，容易水平扩展
- 自包含，能直接带用户信息，减少查库
- 跨域、跨语言支持好

**缺点：**

- **无法主动失效**：签发后到过期前一直有效。想“登出”或“踢人”，得额外维护黑名单，反而又有状态了
- **Payload 不能放敏感信息**
- **体积比随机 Token 大**，每次请求都带，浪费带宽
- **续期麻烦**：通常靠 refresh token 机制解决



# 用户登录和认证的流程是什么？

**登录 = 验证身份并签发凭证（Session ID 或 JWT）；**

**认证 = 每次请求携带凭证，服务端验证后放行。**

**JWT 方案无状态、易扩展，但要用短过期 + Refresh Token 来弥补无法主动失效的短板。**



# 用到了哪个包？

- **Maven 依赖**：`spring-boot-starter-security`
- **Gradle 依赖**：`implementation 'org.springframework.boot:spring-boot-starter-security'`



# 这个项目的用户登录是如何设计的？

~~~text
LoginView.vue          stores/auth.js         api/auth.js          api/http.js
    │                       │                      │                    │
    │ submit()              │                      │                    │
    │──login(u,p)──────────────────────────────>  │                    │
    │                       │                      │ http.post(...)      │
    │                       │                      │──────────────────>  │
    │                       │                      │                    │ 带 token 请求头
    │                       │                      │<──response.data────│
    │                       │  返回 result.data    │                    │
    │<──data────────────────│                      │                    │
    │ setLogin(data)        │                      │                    │
    │──────────────────────>│                      │                    │
    │                       │ 存 token/username    │                    │
    │                       │ 到 localStorage      │                    │
    │ router.push('/todos') │                      │                    │
~~~

| 步骤 | 位置              | 做什么                                            |
| :--- | :---------------- | :------------------------------------------------ |
| 1    | `LoginView.vue`   | 收集 username/password，前端校验                  |
| 2    | `api/auth.js`     | 发 POST `/auth/login`                             |
| 3    | `api/http.js`     | 请求拦截器附上 token（登录接口本身不需要）        |
| 4    | 后端              | 验证密码，返回 `{code:0, data:{token, username}}` |
| 5    | `api/auth.js`     | 检查 code，返回 data                              |
| 6    | `stores/auth.js`  | `setLogin` 存 token 和 username                   |
| 7    | `LoginView.vue`   | 跳转到 redirect 或 `/todos`                       |
| 8    | `router/index.js` | 后续访问受保护页面时，守卫检查 token              |