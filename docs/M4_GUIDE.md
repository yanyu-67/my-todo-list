# M4 手把手指导手册：登录认证与权限控制

> 适合第一次接触 Spring Security、JWT 和前后端认证的同学。
>
> 本手册的目标是完成里程碑 M4：用户可以注册、登录并获得 JWT；任务接口必须登录后才能访问；每个用户只能操作自己的任务；修改密码后必须重新登录。

## 1. 先明确 M4 要解决什么问题

M3 的任务接口已经可以工作，但项目当前的 `SecurityConfig` 中存在下面这行：

```java
.requestMatchers("/api/todos/**").permitAll()
```

它表示所有人都可以访问任务接口，这只是 M3 使用本地测试用户时的临时做法。M4 完成后必须删除或改掉它。

本阶段不做管理员、角色、共享任务、Token 黑名单和第三方登录。只有一种用户：普通用户。

### 1.1 三个容易混淆的词

- **认证（Authentication）**：确认“你是谁”。例如登录时验证用户名和密码。
- **授权（Authorization）**：确认“你能做什么”。例如只有登录用户才能访问 `/api/todos`。
- **数据隔离**：确认“你能访问哪一条数据”。例如用户 A 不能通过修改 URL 中的任务 ID 读取用户 B 的任务。

前端隐藏按钮不算安全控制。真正的认证和授权必须在后端完成，因为用户可以绕过网页，直接用 Postman 调接口。

### 1.2 JWT 登录流程

```text
注册：用户名 + 明文密码
  ↓ 后端用 BCrypt 哈希
数据库：username + password_hash（绝不保存明文密码）

登录：用户名 + 密码
  ↓ PasswordEncoder 校验
后端签发 JWT
  ↓
前端保存 JWT
  ↓ 每次访问受保护接口携带 Authorization: Bearer <JWT>
后端过滤器验证签名和过期时间，并把用户放入 SecurityContext
```

JWT 只是“带签名的登录凭证”，不是密码。不要把密码放进 JWT，也不要把 JWT 打印到日志或提交到 Git。

## 2. 开始前检查项目状态

在 PowerShell 中执行：

```powershell
cd D:\github-copilot\my-todo-list
git status
java -version
cd backend
\.\mvnw.cmd -version
```

本项目使用 Java 17 和 Spring Boot 4.1.1，当前仓库的 `backend/pom.xml` 已采用这套版本。开始 M4 前先确认本机环境与项目一致：

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

并确认 parent 使用：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

不要在本项目中擅自切换到其他 Spring Boot 或 Java 版本。确认版本后先执行：

```powershell
\.\mvnw.cmd clean test
```

如果 M3 还没有通过，先修复 M3 的编译和测试问题，再继续 M4。

## 3. M4 最终要新增或修改哪些文件

建议后端结构如下。包名沿用当前项目的 `com.yanyu.todo`：

```text
backend/src/main/java/com/yanyu/todo/
├─ config/SecurityConfig.java                 修改
├─ config/JwtProperties.java                  新增
├─ controller/AuthController.java             新增
├─ dto/LoginRequest.java                      新增
├─ dto/RegisterRequest.java                   新增
├─ dto/ChangePasswordRequest.java             新增
├─ dto/AuthResponse.java                      新增
├─ security/JwtService.java                   新增
├─ security/JwtAuthenticationFilter.java      新增
├─ service/AuthService.java                    新增
├─ service/SecurityCurrentUserService.java    新增
└─ exception/GlobalExceptionHandler.java      修改
```

现有的 `User`、`UserRepository`、`TodoService` 和 `TodoRepository` 可以继续使用。M4 的关键是：把 `LocalCurrentUserService` 替换为从 Spring Security 认证上下文读取当前用户。

## 4. 第一步：添加 JWT 依赖

在 `backend/pom.xml` 的 `<dependencies>` 中添加 JWT 库。版本以项目统一依赖管理为准，下面是常见的 JJWT 0.12.x 写法：

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

执行：

```powershell
cd D:\github-copilot\my-todo-list\backend
\.\mvnw.cmd dependency:tree
\.\mvnw.cmd test
```

如果公司/课程项目已经规定了 JWT 版本，必须使用规定版本，不要同时混用多个 JWT 库。

## 5. 第二步：准备 JWT 密钥，而且不要提交密钥

在本机创建 `backend/src/main/resources/application-local.yml`。这个文件已被 `.gitignore` 忽略：

```yaml
app:
  jwt:
    secret: "请替换成至少32字节的随机字符串"
    expiration-minutes: 60
```

`secret` 用来签名和验证 JWT。开发阶段可以使用随机长字符串；生产环境应使用环境变量或密钥管理服务。不要把真实密钥写入 `application.yaml`，也不要提交到版本库。

在 `application.yaml` 中保留非敏感的默认配置即可：

```yaml
spring:
  profiles:
    active: local
```

新建 `config/JwtProperties.java`：

```java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {}
```

在启动类上启用配置属性：

```java
@EnableConfigurationProperties(JwtProperties.class)
```

如果使用 `@ConfigurationPropertiesScan`，也可以采用扫描方式，但项目中只选一种。

## 6. 第三步：为密码配置 BCrypt

在 `SecurityConfig` 中增加 Bean：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

注册时调用 `passwordEncoder.encode(rawPassword)`，登录时调用 `passwordEncoder.matches(rawPassword, passwordHash)`。不要自己写 SHA-256，也不要直接比较明文。

`User.passwordHash` 的长度 100 足够保存 BCrypt 结果。API 响应只能返回用户名等必要信息，不能返回 `passwordHash` 或整个 `User` 实体。

## 7. 第四步：定义认证 DTO 和接口

DTO 只描述请求和响应，不让 Controller 直接接收实体对象。

```java
public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password) {}

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {}

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 6, max = 100) String newPassword,
        @NotBlank String confirmPassword) {}

public record AuthResponse(String token, String username) {}
```

认证接口建议保持需求文档中的路径：

| 方法 | 路径 | 登录要求 |
| --- | --- | --- |
| POST | `/api/auth/register` | 不需要 |
| POST | `/api/auth/login` | 不需要 |
| POST | `/api/auth/change-password` | 需要 |

Controller 只负责接收请求、调用 Service 和返回 `ApiResponse`。用户名是否重复、密码是否正确、修改密码后如何处理，都放到 `AuthService`。

## 8. 第五步：实现 JWT 服务

`JwtService` 至少需要完成四件事：生成 Token、读取用户名、校验签名、校验过期时间。

核心逻辑示例：

```java
@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret()));
    }

    public String generate(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(
                        properties.expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String username(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
}
```

上面示例要求配置中的 `secret` 是 Base64 字符串。如果你保存的是普通随机文本，就不要调用 `Decoders.BASE64.decode`，应使用足够长的 UTF-8 字节创建密钥。关键是生成和验证必须使用同一把密钥。

## 9. 第六步：实现 JWT 认证过滤器

创建 `JwtAuthenticationFilter`，继承 `OncePerRequestFilter`，每个请求执行以下步骤：

1. 读取 `Authorization` 请求头。
2. 确认它以 `Bearer ` 开头。
3. 取出后面的 Token。
4. 验证签名和过期时间。
5. 从 Token 的 `subject` 读取用户名。
6. 用户存在且当前上下文尚未认证时，创建 `UsernamePasswordAuthenticationToken` 并放入 `SecurityContext`。
7. Token 缺失、格式错误、签名错误或过期时，不要把请求伪装成已登录。

过滤器中捕获 JWT 解析异常后，可以清空认证上下文并继续过滤链；最终由 Spring Security 拒绝受保护接口。不要在过滤器里返回密码或完整异常堆栈。

将过滤器放在用户名密码认证过滤器之前：

```java
http.addFilterBefore(jwtAuthenticationFilter,
        UsernamePasswordAuthenticationFilter.class);
```

## 10. 第七步：实现注册、登录和修改密码

### 10.1 注册

注册 Service 的顺序：

1. 校验用户名和密码的基本格式。
2. `existsByUsername` 检查重复用户名。
3. 用 BCrypt 对密码哈希。
4. 创建 `User(username, passwordHash)` 并保存。
5. 返回成功消息，不返回密码或哈希。

用户名重复建议返回 HTTP 409；参数不合法返回 HTTP 400。

先确认 `RegisterRequest` 的字段名是 `password`，不要误写成 `pssword`：

```java
public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password) {}
```

下面是 `AuthService` 的注册、登录和修改密码实现。这个 Service 依赖当前项目已有的
`UserRepository`、`User`、`PasswordEncoder` 和前面实现的 `JwtService`：

以下代码片段属于同一个 `AuthService` 类。先复制类声明和构造方法，后面的登录、修改密码方法继续放在同一个类中，不要再次创建 `AuthService`。

```java
package com.yanyu.todo.service;

import com.yanyu.todo.dto.*;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.exception.AuthException;
import com.yanyu.todo.repository.UserRepository;
import com.yanyu.todo.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.username().trim();

        if (userRepository.existsByUsername(username)) {
            throw new AuthException(HttpStatus.CONFLICT, "用户名已存在");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        userRepository.save(new User(username, passwordHash));
    }
```

注册时只保存 BCrypt 哈希，绝不能保存 `request.password()` 原文。

### 10.2 登录

登录 Service 的顺序：

1. 按用户名查询用户。
2. 用户不存在或密码不匹配时，统一返回“用户名或密码错误”。不要告诉攻击者究竟是哪一个错了。
3. 生成 JWT。
4. 返回 `AuthResponse(token, username)`。

继续在 `AuthService` 中实现登录：

```java
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new AuthException(
                        HttpStatus.UNAUTHORIZED, "用户名或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException(
                    HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtService.generate(user.getUsername());
        return new AuthResponse(token, user.getUsername());
    }
```

用户不存在和密码错误使用同一条消息，避免泄露“某个用户名是否存在”。

登录成功响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJ...",
    "username": "alice"
  }
}
```

### 10.3 修改密码

修改密码必须从认证上下文取得当前用户：

```java
Authentication authentication = SecurityContextHolder
        .getContext().getAuthentication();
String username = authentication.getName();
```

然后依次检查：旧密码正确、新密码至少 6 位、确认密码与新密码一致。通过后保存新的 BCrypt 哈希。

可以把修改密码也放在 `AuthService` 中：

```java
    @Transactional
    public void changePassword(String username,
                               ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.UNAUTHORIZED, "用户不存在"));

        if (!passwordEncoder.matches(
                request.oldPassword(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "旧密码错误");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "两次输入的新密码不一致");
        }

        if (request.oldPassword().equals(request.newPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "新密码不能与旧密码相同");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
```

上面代码中的 `AuthException` 可以单独放在
`exception/AuthException.java`：

```java
package com.yanyu.todo.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

并在 `GlobalExceptionHandler` 中增加处理方法，使 409、400、401 状态码真正返回给前端：

```java
@ExceptionHandler(AuthException.class)
public ResponseEntity<ApiResponse<Void>> auth(AuthException ex) {
    return ResponseEntity.status(ex.getStatus())
            .body(new ApiResponse<>(
                    ex.getStatus().value(), ex.getMessage(), null));
}
```

记得增加这些导入：

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
```

### 10.4 实现 AuthController

Controller 只接收请求、调用 Service 和包装响应，不直接操作数据库：

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return ApiResponse.success(null);
    }
}
```

Controller 需要的导入：

```java
import com.yanyu.todo.dto.*;
import com.yanyu.todo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
```

三个接口的实际调用顺序如下：

```text
POST /api/auth/register       注册用户，不需要 Token
POST /api/auth/login          验证密码并返回 JWT，不需要 Token
POST /api/auth/change-password 携带 JWT，验证旧密码后修改密码
```

测试请求示例：

```http
POST /api/auth/register
Content-Type: application/json

{"username":"alice","password":"123456"}
```

```http
POST /api/auth/login
Content-Type: application/json

{"username":"alice","password":"123456"}
```

```http
POST /api/auth/change-password
Authorization: Bearer eyJ...
Content-Type: application/json

{"oldPassword":"123456","newPassword":"654321","confirmPassword":"654321"}
```

修改成功后，当前 JWT 不需要由服务端加入黑名单（本期不实现黑名单），但接口应返回“请重新登录”，前端必须清除 Token 并跳转登录页。若项目要严格实现“旧 Token 立即失效”，可以给 `User` 增加密码版本或 `passwordChangedAt`，在 JWT 中加入版本/签发时间并在过滤器中校验；这属于增强方案。

## 11. 第八步：替换当前用户服务

当前 `TodoService` 已经通过 `CurrentUserService` 获取用户，这是很好的扩展点。新增正式实现：

```java
@Service
@Profile("!local")
public class SecurityCurrentUserService implements CurrentUserService {
    private final UserRepository userRepository;

    public SecurityCurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}
```

注意 `LocalCurrentUserService` 使用了 `@Profile("local")`，而当前 `application.yaml` 默认激活 `local`。为了验证真实 JWT，需要切换到非 `local` 配置，或者把本地 Profile 改成一个明确的测试 Profile，并确保同一时间不会有两个 `CurrentUserService` Bean。

推荐做法是：M4 手工联调时不激活 `local`；自动化测试可以显式使用测试用户或测试 Profile。不要为了让接口能调用而继续从请求体接收 `userId`。

## 12. 第九步：修改 SecurityConfig

最终授权规则应类似：

```java
http
    .csrf(csrf -> csrf.disable())
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .formLogin(form -> form.disable())
    .httpBasic(basic -> basic.disable())
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/health").permitAll()
        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter,
        UsernamePasswordAuthenticationFilter.class);
```

最重要的三点：

- 删除 `.requestMatchers("/api/todos/**").permitAll()`。
- 使用无状态 Session，因为每次请求都靠 JWT，不靠服务器 Session。
- `/api/auth/change-password` 不要公开，它必须登录。

如果需要统一 JSON 的 401 和 403，可配置 `exceptionHandling` 的 `AuthenticationEntryPoint` 与 `AccessDeniedHandler`。401 表示没有有效身份；403 表示已经识别身份但没有权限。本期只有普通用户，资源归属错误可以按现有设计返回 404，避免泄露资源是否存在。

## 13. 第十步：统一异常响应

目标是让业务异常、参数错误、未登录和无权限都返回相同结构的 JSON。前端只需要读取
`code`、`message` 和 `data`，不需要解析 Spring 默认的错误页面。

### 13.1 统一响应结构

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

当前项目的 `ApiResponse` 可以保持为：

```java
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }
}
```

成功响应使用 `code = 0`；失败响应的 `code` 可以与 HTTP 状态码保持一致，便于排查。

### 13.2 处理 Controller 业务异常

`AuthException` 已经携带 HTTP 状态码：

```java
@ExceptionHandler(AuthException.class)
public ResponseEntity<ApiResponse<Void>> auth(AuthException ex) {
    return ResponseEntity.status(ex.getStatus())
            .body(new ApiResponse<>(
                    ex.getStatus().value(),
                    ex.getMessage(),
                    null));
}
```

参数校验失败时，返回第一个字段的错误信息：

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> validation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("请求参数不合法");

    return new ApiResponse<>(400, message, null);
}
```

未知异常只记录服务端日志，不把堆栈返回给前端：

```java
private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ApiResponse<Void> other(Exception ex) {
    log.error("未处理的服务器异常", ex);
    return new ApiResponse<>(500, "服务器内部错误", null);
}
```

需要增加：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

### 13.3 统一 Spring Security 的 401 和 403

`GlobalExceptionHandler` 无法捕获过滤器链中产生的 401 和 403。需要在
`SecurityConfig` 的 `exceptionHandling` 中配置：

把下面的 `exceptionHandling` 直接放到现有的 `http` 配置链中，通常放在
`httpBasic` 后面、`sessionManagement` 前面：

```java
http
    .csrf(csrf -> csrf.disable())
    .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
            })
            .accessDeniedHandler((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"code\":403,\"message\":\"无权访问该资源\",\"data\":null}");
            }))
    .formLogin(form -> form.disable())
    .httpBasic(basic -> basic.disable())
    .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/health").permitAll()
            .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
            .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);
```

需要导入：

```java
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
```

如果项目使用 `ObjectMapper` 生成 JSON，也可以用它替换上面的字符串拼接。无论采用哪种方式，响应的字段结构必须保持一致。

### 13.4 状态码约定

至少统一以下情况：

| 情况 | HTTP 状态 | 前端可见消息 |
| --- | ---: | --- |
| 用户名或密码错误 | 401 | 用户名或密码错误 |
| 缺少 Token | 401 | 请先登录 |
| Token 签名错误/过期 | 401 | 登录已失效，请重新登录 |
| 参数校验失败 | 400 | 具体字段错误 |
| 用户名已存在 | 409 | 用户名已被使用 |
| 资源不属于当前用户 | 404 或 403 | 任务不存在或无权访问 |

### 13.4.1 新增 `AuthException.java`

如果项目中还没有这个文件，创建：

```text
backend/src/main/java/com/yanyu/todo/exception/AuthException.java
```

写入：

```java
package com.yanyu.todo.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

注意导入的是自己项目的异常：

```java
import com.yanyu.todo.exception.AuthException;
```

不要导入 Hibernate 的 `org.hibernate.exception.AuthException`。

### 13.4.2 修改 `GlobalExceptionHandler.java`

在现有的 `GlobalExceptionHandler` 中加入或整理为以下处理方法：

```java
package com.yanyu.todo.exception;

import com.yanyu.todo.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> auth(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiResponse<>(
                        ex.getStatus().value(),
                        ex.getMessage(),
                        null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .orElse("请求参数不合法");

        return new ApiResponse<>(400, message, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(ResourceNotFoundException ex) {
        return new ApiResponse<>(404, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> other(Exception ex) {
        log.error("未处理的服务器异常", ex);
        return new ApiResponse<>(500, "服务器内部错误", null);
    }
}
```

不要把 `ex.printStackTrace()` 的内容返回给前端。异常堆栈只写入后端日志。

### 13.4.3 在 `SecurityConfig.java` 中处理 401 和 403

在 `SecurityConfig` 的 `http` 配置链中加入 `exceptionHandling`。完整结构如下：

```java
http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401);
                    response.setContentType(
                            "application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"code\":401,"
                                    + "\"message\":\"请先登录\","
                                    + "\"data\":null}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403);
                    response.setContentType(
                            "application/json;charset=UTF-8");
                    response.getWriter().write(
                            "{\"code\":403,"
                                    + "\"message\":\"无权访问该资源\","
                                    + "\"data\":null}");
                }))
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login"
                ).permitAll()
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated())
        .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
```

需要确认导入：

```java
import org.springframework.security.config.http.SessionCreationPolicy;
```

不要把下面的修改密码接口放进 `permitAll()`：

```java
"/api/auth/change-password"
```

否则未登录请求会进入 Controller，可能出现 `Authentication` 为 `null` 的空指针异常。

### 13.4.4 在 Service 中抛出业务异常

业务代码按实际情况抛出 `AuthException`：

```java
if (userRepository.existsByUsername(username)) {
    throw new AuthException(
            HttpStatus.CONFLICT,
            "用户名已被使用");
}
```

```java
if (!passwordEncoder.matches(
        request.password(), user.getPasswordHash())) {
    throw new AuthException(
            HttpStatus.UNAUTHORIZED,
            "用户名或密码错误");
}
```

Service 不需要写 `ResponseEntity`，也不需要手动拼接 JSON。

实现时要同时设置两层状态：

1. HTTP 状态码通过 `ResponseEntity.status(...)` 或 `@ResponseStatus` 设置。
2. JSON 中的 `code` 与 HTTP 状态码保持一致；成功时使用 `code = 0`。

例如用户名重复：

```java
throw new AuthException(HttpStatus.CONFLICT, "用户名已被使用");
```

由 `GlobalExceptionHandler` 统一转换为：

```json
{
  "code": 409,
  "message": "用户名已被使用",
  "data": null
}
```

不要在 Service 中直接返回 HTTP 响应，也不要在 Controller 中复制一套异常判断。业务代码只抛出有明确含义的异常，响应格式统一交给异常处理器。

### 13.5 后端完成后的自检顺序

按下面顺序检查，能快速区分问题发生在哪一层：

```text
Controller 是否收到请求
  ↓
DTO 参数校验是否通过（400）
  ↓
JWT 过滤器是否建立认证（401）
  ↓
授权规则是否允许访问（403）
  ↓
Service 业务校验是否通过（400/401/409）
  ↓
数据库操作是否成功（500）
```

建议先用 Postman 验证后端响应，再接入前端。这样可以判断错误究竟来自请求格式、Token、权限还是前端代码。

## 14. 前端需要知道的配合方式

M4 只要求后端完成安全边界，但前端请求层必须保存 Token、自动携带 Token，并在收到 401 时清理登录状态。

### 14.1 配置 API 地址

当前 `frontend/src/api/http.js` 的 `baseURL` 来自 `VITE_API_BASE_URL`。本地环境文件可以写：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

当 `baseURL` 已经包含 `/api` 时，前端请求使用 `/auth/login`、`/todos`；不要再写成
`/api/auth/login`，否则会请求到 `/api/api/auth/login`。

`frontend/.env.local` 示例：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

修改 `.env.local` 后必须重启 Vite 开发服务器，因为 `import.meta.env` 在构建时读取。

### 14.2 请求拦截器：自动携带 JWT

在 `frontend/src/api/http.js` 中配置：

```js
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('todo_token')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
```

请求头必须是：

```http
Authorization: Bearer eyJ...
```

`Bearer` 和 Token 之间必须有一个空格。不要重复拼接 `Bearer Bearer ...`。

不要把 Token 放进 URL、请求体或日志。只放在 `Authorization` 请求头中。

### 14.3 登录成功后保存 Token

登录接口的响应结构为：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJ...",
    "username": "alice"
  }
}
```

前端登录方法示例：

```js
import http from './http'

export async function login(username, password) {
  const response = await http.post('/auth/login', {
    username,
    password,
  })

  const data = response.data
  localStorage.setItem('todo_token', data.data.token)
  localStorage.setItem('todo_username', data.data.username)
  return data.data
}
```

调用登录方法时只在 `code === 0` 时保存 Token：

```js
const result = await login(username, password)
// login 内部已经保存 Token；这里跳转到需要登录的页面
window.location.href = '/todos'
```

如果后端返回 401、400 或 500，Axios 会进入响应错误处理分支，不要把错误响应当成登录成功处理。

注册成功后不一定自动登录，推荐跳转到登录页；只有登录成功拿到 `data.token` 后，才把 Token 保存下来。

### 14.4 401 和 403 的处理

配置响应拦截器：

```js
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      localStorage.removeItem('todo_token')
      localStorage.removeItem('todo_username')
      window.location.href = '/login'
    }

    if (status === 403) {
      console.error('当前用户没有访问权限')
    }

    return Promise.reject(error)
  },
)
```

显示业务错误时优先读取后端统一结构：

```js
function getApiErrorMessage(error) {
  return error.response?.data?.message ?? '请求失败，请稍后重试'
}
```

表单页面可以这样使用：

```js
try {
  await http.post('/auth/register', form)
} catch (error) {
  formError.value = getApiErrorMessage(error)
}
```

处理规则：

| 响应 | 前端动作 |
| --- | --- |
| 200/201 | 使用 `data` 中的业务数据 |
| 400 | 在表单中显示参数错误 |
| 401 | 删除 Token，跳转登录页 |
| 403 | 提示没有权限，不要反复重试登录 |
| 409 | 提示用户名已存在 |
| 500 | 提示服务器异常 |

退出登录时主动清理：

```js
export function logout() {
  localStorage.removeItem('todo_token')
  localStorage.removeItem('todo_username')
  window.location.href = '/login'
}
```

路由守卫只是体验优化，不能代替后端认证。用户仍然可以绕过前端直接调用接口，所以后端必须继续拒绝没有 JWT 的任务请求。

前端开发环境应确保后端允许前端来源 `http://localhost:5173`，并允许 `OPTIONS` 预检请求；这属于 CORS 配置，不要通过关闭认证来解决 CORS 报错。

## 15. 手工验收：按这个顺序测试

启动 PostgreSQL 和后端，然后使用 IDEA HTTP Client、Postman 或 Apifox。

### 15.1 注册

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{"username":"alice","password":"123456"}
```

再次注册 `alice`，应得到 409。使用少于 6 位密码，应得到 400。

成功时应类似：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

重复注册时应类似：

```json
{
  "code": 409,
  "message": "用户名已被使用",
  "data": null
}
```

### 15.2 登录并保存 Token

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{"username":"alice","password":"123456"}
```

复制响应中的 `data.token`，后续请求使用：

```http
Authorization: Bearer eyJ...
```

不要复制整个响应 JSON，也不要复制 `"token":` 或引号，只复制 Token 字符串。

### 15.3 验证任务接口保护

不带 Authorization 调用：

```http
GET http://localhost:8080/api/todos
```

必须返回 401。带正确 Token 后才能返回 alice 的任务列表。

如果返回 403，优先检查是否配置了自定义 `AuthenticationEntryPoint`，以及请求是否实际带有 `Authorization` 请求头；无 Token 通常应是 401，而不是把所有错误都返回 403。

### 15.4 验证用户隔离

1. alice 登录并创建一条任务，记录任务 ID。
2. 注册并登录 bob。
3. bob 请求 alice 的任务 ID。
4. bob 不应看到、修改或删除该任务；返回 404 或 403 均可，但不能返回任务内容。

### 15.5 验证 Token

分别测试：不带 Token、Token 前后多一个字符、过期 Token。三种情况都不能访问任务接口。不要为了通过测试把接口改成公开。

### 15.6 验证修改密码

发送：

```http
POST http://localhost:8080/api/auth/change-password
Authorization: Bearer eyJ...
Content-Type: application/json

{"oldPassword":"123456","newPassword":"654321","confirmPassword":"654321"}
```

旧密码错误、确认密码不一致、密码少于 6 位都必须失败。成功后前端清除 Token；使用新密码登录，旧密码不能再登录。

修改密码请求必须同时满足：

- URL 是 `/api/auth/change-password`。
- 请求头有有效的 `Authorization: Bearer <token>`。
- `oldPassword` 与数据库中的 BCrypt 哈希匹配。
- `newPassword` 和 `confirmPassword` 完全一致。
- 新密码满足 DTO 的长度约束。

成功后返回 `200`，并建议前端删除旧 Token、跳转登录页。当前实现没有 Token 黑名单，因此仅修改密码不会自动让已经签发的旧 JWT 立即失效。

## 16. 自动化测试清单

至少补充以下测试，测试名字要能表达业务规则：

- `registerStoresBCryptHashInsteadOfPlainPassword`
- `duplicateUsernameReturnsConflict`
- `loginWithCorrectPasswordReturnsJwt`
- `loginWithWrongPasswordReturnsUnauthorized`
- `todoApiWithoutTokenReturnsUnauthorized`
- `todoApiWithValidTokenReturnsCurrentUserTodos`
- `userCannotReadAnotherUsersTodo`
- `expiredOrInvalidTokenReturnsUnauthorized`
- `changePasswordRequiresOldPassword`
- `changePasswordRequiresMatchingConfirmation`

测试数据库中不要写真实密码或真实 JWT 密钥。使用随机测试用户名，测试结束后清理数据，或使用专门的测试数据库。

每个测试至少断言三项：HTTP 状态码、响应中的 `code`、关键业务结果。例如登录失败不能只断言“请求失败”，还要断言状态为 401 且消息不能暴露用户名是否存在。

MockMvc 测试请求的形状示例：

```java
mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"username":"alice","password":"wrong"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401));
```

带 JWT 的接口测试应把登录结果中的 Token 放入请求头：

```java
mockMvc.perform(get("/api/todos")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
```

## 17. 常见问题排查

### 一直返回 401

依次检查：请求头是否叫 `Authorization`；值是否是 `Bearer ` 加一个空格再加 Token；签名密钥是否与生成 Token 时相同；Token 是否过期；JWT 过滤器是否加入过滤器链；当前 Profile 是否仍启用了本地用户实现。

还要检查：

1. 登录接口返回的字段路径是否为 `data.token`。
2. 前端保存和读取 Token 时使用的 localStorage key 是否一致。
3. `JwtAuthenticationFilter` 是否查询到了 Token 中的用户名。
4. `JwtService` 生成和解析时是否使用同一把密钥。
5. 请求是否真的经过了当前运行的后端，而不是另一个端口上的旧进程。

### 一直返回 403

403 表示请求被拒绝。依次检查：

1. 是否已经通过 JWT 认证；没有身份通常应返回 401。
2. 是否配置了 `AccessDeniedHandler`，响应体是否包含统一的 `code = 403`。
3. 访问路径是否命中了错误的 `requestMatchers`。
4. 是否误把接口放进了错误的角色权限规则。
5. POST、PUT、DELETE 请求是否受到 CSRF 保护；纯 JWT 无状态 API 通常需要明确关闭 CSRF。

不要为了消除 403 而把 `.anyRequest().authenticated()` 改成 `.permitAll()`。

### 浏览器报 CORS 错误

当前项目同时有 `WebConfig` 和 `SecurityConfig` 的 CORS 配置。开发环境来源应准确包含 `http://localhost:5173`，并允许 `OPTIONS` 预检请求。生产环境不能使用 `*` 配合凭证，也不能无条件放开所有来源。

### 启动时报两个 CurrentUserService Bean

说明 `LocalCurrentUserService` 和正式实现同时生效。检查 `@Profile`，并确认当前激活的 Profile；不要通过 `@Primary` 掩盖配置错误。

### 数据库里仍能看到明文密码

停止继续测试，检查注册逻辑是否调用了 `passwordEncoder.encode`，并检查保存的是 `passwordHash` 而不是原始 password。已有测试数据应删除后重新注册。

正确流程是：

```text
注册请求 password
  ↓ passwordEncoder.encode(password)
数据库保存 passwordHash
  ↓ passwordEncoder.matches(rawPassword, passwordHash)
登录成功后签发 JWT
```

## 18. M4 完成检查表

- [x] 使用 Java 17 和 Spring Boot 4.1.1（项目 `pom.xml` 配置已确认）。
- [ ] JWT 依赖已添加，项目可以通过命令行 `mvn test`（当前命令行复验受 Maven 下载网络权限阻塞）。
- [x] JWT 密钥只存在本机配置或环境变量，没有提交到 Git。
- [x] 注册用户名唯一，密码至少 6 位（集成测试已覆盖）。
- [x] 数据库保存 BCrypt 哈希，不保存明文密码（集成测试已覆盖）。
- [x] 登录成功返回 JWT，错误密码返回统一 401（集成测试已覆盖）。
- [x] JWT 校验签名和有效期（过滤器和登录测试已通过）。
- [x] 注册和登录公开，修改密码和任务接口受保护。
- [x] 已删除任务接口的 `permitAll()`。
- [ ] TodoService 从认证上下文识别用户，不信任前端 `userId`（需要完成用户隔离手工/自动化验收）。
- [ ] 用户 A 无法读取、修改或删除用户 B 的任务。
- [ ] 缺失、过期、伪造 Token 都会被拒绝（无 Token 已测试，过期/伪造 Token 仍需单独验收）。
- [x] 修改密码校验旧密码和确认密码，成功后要求重新登录（集成测试已覆盖）。
- [ ] 401、403、400、404/409 响应格式统一且不泄露堆栈。
- [ ] 已完成核心认证失败、未登录访问和用户隔离自动化测试（认证基础测试已通过，用户隔离测试尚未完成）。
- [x] 前端 `auth.js` 语法已通过检查，使用临时输出目录执行 Vite production build 成功。

完成检查时至少保存以下验收记录：

| 验收项 | 预期结果 |
| --- | --- |
| 注册新用户 | 201，`code = 0` |
| 重复注册 | 409，`code = 409` |
| 正确登录 | 200，响应包含 `data.token` |
| 错误密码 | 401，不能说明用户名是否存在 |
| 无 Token 访问任务 | 401 |
| 伪造或过期 Token | 401 |
| 带 Token 访问任务 | 200，只能看到自己的任务 |
| 修改密码 | 200，前端清除旧 Token |
| 无权限访问 | 403，返回统一 JSON |

当前验收记录（2026-09-15）：

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| 后端 `AuthIntegrationTest` | 通过 | IntelliJ 显示 11/11 tests passed |
| 前端 JavaScript 构建 | 通过 | 临时输出目录构建成功；原 `frontend/dist` 目录存在权限/占用问题 |
| 命令行 Maven 测试 | 阻塞 | 无法下载 `spring-boot-starter-parent:4.1.1`，报网络权限错误 |
| 用户隔离验收 | 未完成 | 还需要 alice/bob 两个用户的手工或自动化测试 |
| 过期/伪造 Token 验收 | 未完成 | 还需要分别发送过期和篡改后的 Token |
| 统一 401/403 JSON 验收 | 未完成 | 还需要通过 Postman 检查实际响应体 |

因此当前 M4 状态为：**核心认证功能通过，完整验收尚未完成**。只有未完成项目全部验证通过后，才能把本节剩余复选框全部勾选。

## 19. 推荐提交方式

确认没有提交本地密钥、密码、`target` 或 `node_modules`：

```powershell
cd D:\github-copilot\my-todo-list
git status
git add backend/pom.xml backend/src/main docs/M4_GUIDE.md
git status
git commit -m "feat: add JWT authentication and authorization"
```

提交前重点看 `git status` 和 `git diff --cached`。如果看到 `application-local.yml` 或任何真实密钥，先取消暂存并检查 `.gitignore`。

如果只修改了文档，可以单独检查：

```powershell
git diff -- docs/M4_GUIDE.md
git diff --check
```

如果修改了后端代码，再执行：

```powershell
cd D:\github-copilot\my-todo-list\backend
.\mvnw.cmd clean test
```

如果修改了前端代码，再执行：

```powershell
cd D:\github-copilot\my-todo-list\frontend
npm run build
```

只有后端测试和前端构建都通过，并且手工验收中的认证边界符合预期，才算 M4 完成。完成本手册后，进入 M5 时可以在现有 Axios 请求层加入 Token，并使用路由守卫改善登录页面和首页之间的跳转体验。
