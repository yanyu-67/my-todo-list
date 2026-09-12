# M3：待办事项基础 API——新手手把手指导

> 本手册根据 `REQUIREMENTS.md`、`README.md` 和 `MILESTONES.md` 编写，按当前项目版本 Spring Boot 4.1.1、Java 17 和包名 `com.yanyu.todo` 示例。

## 目标与接口

M3 在 M2 的实体和 Repository 之上，完成 DTO、Service、Controller、请求校验、统一响应和全局异常处理。任务接口为：

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/todos` | 当前用户任务列表 |
| GET | `/api/todos/{id}` | 任务详情 |
| POST | `/api/todos` | 创建任务 |
| PUT | `/api/todos/{id}` | 编辑任务 |
| PATCH | `/api/todos/{id}/complete` | 设置完成状态 |
| DELETE | `/api/todos/{id}` | 永久删除任务 |

调用链：`Controller → DTO 校验 → Service 业务规则 → Repository → DTO 响应`。

## 1. 修正 M2 前置问题

打开 `Todo.java`，如果字段叫 `createAt`，统一改成需求规定的 `createdAt`，包括 getter、setter 和 `@PrePersist`。`updatedAt` 不能写 `updatable = false`：

```java
@Column(nullable = false)
private LocalDateTime updatedAt;
```

`Todo` 还需要：

```java
public User getUser() { return user; }
public void setUser(User user) { this.user = user; }
```

在 IDEA 执行 `Build → Rebuild Project`，确认 M2 没有编译错误。

## 2. 创建 DTO

在 `backend/src/main/java/com/yanyu/todo/dto` 创建以下文件。

### TodoCreateRequest.java

```java
package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TodoCreateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过200个字符") String title,
        @Size(max = 2000, message = "描述不能超过2000个字符") String description,
        Boolean important,
        LocalDate dueDate
) {}
```

### TodoUpdateRequest.java

```java
package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TodoUpdateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过200个字符") String title,
        @Size(max = 2000, message = "描述不能超过2000个字符") String description,
        Boolean important,
        LocalDate dueDate
) {}
```

### TodoCompleteRequest.java

```java
package com.yanyu.todo.dto;

import jakarta.validation.constraints.NotNull;

public record TodoCompleteRequest(
        @NotNull(message = "完成状态不能为空") Boolean completed
) {}
```

请求 DTO 故意不包含 `id`、`userId`、`createdAt`、`updatedAt`。这些字段由后端决定，不能由前端修改。

## 3. 创建响应 DTO

创建 `dto/TodoResponse.java`：

```java
package com.yanyu.todo.dto;

import com.yanyu.todo.entity.Todo;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TodoResponse(
        Long id, String title, String description, boolean completed,
        boolean important, LocalDate dueDate, LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(todo.getId(), todo.getTitle(),
                todo.getDescription(), todo.isCompleted(), todo.isImportant(),
                todo.getDueDate(), todo.getCreatedAt(), todo.getUpdatedAt());
    }
}
```

创建 `dto/ApiResponse.java`：

```java
package com.yanyu.todo.dto;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }
}
```

响应统一为：

```json
{"code": 0, "message": "success", "data": {}}
```

## 4. 封装当前用户

M3 不得在 Controller 接收前端的 `userId`。创建 `service/CurrentUserService.java`：

```java
package com.yanyu.todo.service;

import com.yanyu.todo.entity.User;

public interface CurrentUserService {
    User requireCurrentUser();
}
```

M3 联调时可以提供一个仅本地使用的实现，返回 M2 的演示用户；M4 必须替换为从 Spring Security/JWT 认证上下文读取用户。这个替换点的意义是：后续不会因为改登录方式而改动所有任务业务代码。

### 4.1 创建 M3 本地实现类

仅有上面的接口还不够。Spring 需要一个带有 `@Service` 的具体实现类，才能把它注入到 `TodoService`。创建 `service/LocalCurrentUserService.java`：

```java
package com.yanyu.todo.service;

import com.yanyu.todo.entity.User;
import com.yanyu.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("local")
public class LocalCurrentUserService implements CurrentUserService {
    private final UserRepository userRepository;

    @Value("${app.dev-user-id}")
    private Long devUserId;

    public LocalCurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        return userRepository.findById(devUserId)
                .orElseThrow(() -> new RuntimeException("本地测试用户不存在"));
    }
}
```

然后在 `backend/src/main/resources/application-local.yml` 中添加本地测试用户 ID：

```yaml
app:
  frontend-url: http://localhost:5173
  dev-user-id: 1
```

这里的 `1` 必须是数据库中真实存在的用户 ID。可以在 pgAdmin 的 Query Tool 执行：

```sql
SELECT id, username FROM users;
```

如果实际用户 ID 是 2，就把 `dev-user-id` 改成 2。保存后重启后端。

`@Service` 让 Spring 注册这个实现类，`implements CurrentUserService` 让它符合接口，`@Profile("local")` 保证它只在本地开发环境启用。M4 接入 JWT 后，要删除或替换这个本地实现，不能在正式环境固定使用某个用户 ID。

## 5. 创建业务异常

创建 `exception/ResourceNotFoundException.java`：

```java
package com.yanyu.todo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

任务不存在或不属于当前用户时抛出它。对外统一返回 404，避免泄露其他用户任务是否存在。

## 6. 创建 TodoService

创建 `service/TodoService.java`。核心写法如下：

```java
package com.yanyu.todo.service;

import com.yanyu.todo.dto.*;
import com.yanyu.todo.entity.Todo;
import com.yanyu.todo.entity.User;
import com.yanyu.todo.exception.ResourceNotFoundException;
import com.yanyu.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TodoService {
    private final TodoRepository todoRepository;
    private final CurrentUserService currentUserService;

    public TodoService(TodoRepository todoRepository,
                       CurrentUserService currentUserService) {
        this.todoRepository = todoRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> list() {
        User user = currentUserService.requireCurrentUser();
        return todoRepository.findAllByUserId(user.getId()).stream()
                .map(TodoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TodoResponse get(Long id) {
        return TodoResponse.from(findOwnedTodo(id));
    }

    public TodoResponse create(TodoCreateRequest request) {
        User user = currentUserService.requireCurrentUser();
        Todo todo = new Todo(request.title(), request.description(), user);
        todo.setImportant(Boolean.TRUE.equals(request.important()));
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(todoRepository.save(todo));
    }

    public TodoResponse update(Long id, TodoUpdateRequest request) {
        Todo todo = findOwnedTodo(id);
        todo.setTitle(request.title());
        todo.setDescription(request.description());
        todo.setImportant(Boolean.TRUE.equals(request.important()));
        todo.setDueDate(request.dueDate());
        return TodoResponse.from(todoRepository.save(todo));
    }

    public TodoResponse complete(Long id, TodoCompleteRequest request) {
        Todo todo = findOwnedTodo(id);
        todo.setCompleted(request.completed());
        return TodoResponse.from(todoRepository.save(todo));
    }

    public void delete(Long id) {
        todoRepository.delete(findOwnedTodo(id));
    }

    private Todo findOwnedTodo(Long id) {
        User user = currentUserService.requireCurrentUser();
        return todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
    }
}
```

所有任务操作都经过 `findByIdAndUserId`。Service 不接收前端 `userId`，也不允许修改任务所属用户、创建时间和更新时间。

## 7. 创建 TodoController

创建 `controller/TodoController.java`：

```java
package com.yanyu.todo.controller;

import com.yanyu.todo.dto.*;
import com.yanyu.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> list() {
        return ApiResponse.success(todoService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<TodoResponse> get(@PathVariable Long id) {
        return ApiResponse.success(todoService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TodoResponse> create(
            @Valid @RequestBody TodoCreateRequest request) {
        return ApiResponse.success(todoService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TodoResponse> update(@PathVariable Long id,
            @Valid @RequestBody TodoUpdateRequest request) {
        return ApiResponse.success(todoService.update(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ApiResponse<TodoResponse> complete(@PathVariable Long id,
            @Valid @RequestBody TodoCompleteRequest request) {
        return ApiResponse.success(todoService.complete(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        todoService.delete(id);
    }
}
```

`@Valid` 会让 Spring 根据 DTO 注解校验标题和字段长度。Controller 不写复杂业务。

## 8. 全局异常处理

创建 `exception/GlobalExceptionHandler.java`：

```java
package com.yanyu.todo.exception;

import com.yanyu.todo.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> notFound(ResourceNotFoundException ex) {
        return new ApiResponse<>(404, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");
        return new ApiResponse<>(400, message, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> other(Exception ex) {
        return new ApiResponse<>(500, "服务器内部错误", null);
    }
}
```

不要把堆栈、密码或 Token 返回给前端，详细异常只写后端日志。

## 9. API 验证

启动 PostgreSQL 和后端，使用 IDEA HTTP Client、Postman 或 Apifox 测试。

创建任务：

```http
POST http://localhost:8080/api/todos
Content-Type: application/json

{
  "title": "学习 M3 API",
  "description": "完成 DTO、Service 和 Controller",
  "important": true,
  "dueDate": "2026-09-30"
}
```

查询、更新、完成和删除：

```http
GET http://localhost:8080/api/todos
GET http://localhost:8080/api/todos/1
PUT http://localhost:8080/api/todos/1
PATCH http://localhost:8080/api/todos/1/complete
DELETE http://localhost:8080/api/todos/1
```

更新请求体示例：

```json
{"title":"完成 M3 API","description":"已完成","important":true,"dueDate":"2026-09-30"}
```

完成状态请求体：

```json
{"completed":true}
```

M4 前真实 JWT 尚未完成；如果任务接口返回 401，不要把接口永久改成公开，也不要通过前端传 `userId` 绕过鉴权。用本地当前用户实现或 Service 测试完成 M3 验证，M4 再接入 JWT。

## 10. 必测错误场景

1. `title` 为空或超过 200 字符：返回 HTTP 400 和统一 JSON。
2. `description` 超过 2000 字符：返回 HTTP 400。
3. 访问不存在的任务：返回 HTTP 404，不返回堆栈。
4. 用户 A 访问用户 B 的任务：返回 404 或 403，不能泄露任务内容。
5. 不带登录信息访问任务接口：M4 完成后必须返回 401。

## 11. M3 验收清单

- [ ] Controller 不直接接收或返回实体。
- [ ] 创建、更新、完成和查询使用 DTO。
- [ ] 六个任务接口都能调用。
- [ ] 标题和长度校验有效。
- [ ] 错误响应统一且不包含堆栈。
- [ ] 任务不存在返回 404。
- [ ] 所有任务查询按当前用户过滤。
- [ ] 请求中没有 `userId`、`createdAt`、`updatedAt` 的可修改入口。
- [ ] 响应中没有 `passwordHash` 或完整 `User` 对象。
- [ ] OpenAPI 页面能看到任务接口。
- [ ] 用户隔离场景不会泄露数据。

## 12. 提交 M3

```powershell
cd D:\github-copilot\my-todo-list
git status
git add backend/src/main/java docs/M3_GUIDE.md
git status
git commit -m "feat: add todo REST API"
```

确认没有提交 `application-local.yml`、`.env.local`、`node_modules` 或 `target`。M3 完成后进入 M4：用 JWT 和 Spring Security 替换本地当前用户实现。
