# M2：数据库与持久化层——新手手把手指导

> 本手册根据 `REQUIREMENTS.md`、`README.md` 和 `MILESTONES.md` 编写，示例使用当前项目包名 `com.yanyu.todo`。
>
> M2 的目标是：PostgreSQL 中能够保存用户和待办事项，Spring Data JPA 能完成保存、查询、修改和删除。注册、JWT、Controller 和完整接口属于后续 M3/M4。

## 1. M2 要完成的内容

数据库关系如下：

```text
一个 User 用户 1 ─────── 多个 Todo 待办事项
```

`users` 表需要保存唯一用户名、密码哈希和创建时间；`todos` 表需要保存标题、描述、完成状态、重要标记、截止日期、创建时间、更新时间和所属用户 `user_id`。

## 2. 确认本项目采用的版本

本项目实际采用以下版本：

```text
Java 17
Spring Boot 4.1.1
```

请打开 `backend/pom.xml`，确认配置类似：

```xml
<version>4.1.1</version>
...
<java.version>17</java.version>
```

保存后在 IDEA 的 Maven 面板点击 Reload。后续代码和依赖都按 Spring Boot 4.1.1 编写，不要混用 Spring Boot 3 的教程代码。

说明：项目原始需求文档写的是 Java 21 + Spring Boot 3.x；本项目现在按你的决定改用 Java 17 + Spring Boot 4.1.1。两套版本不要混用，遇到依赖或注解差异时，以当前 `pom.xml` 为准。

## 3. 检查数据库配置

确认 `backend/src/main/resources/application.yaml` 中有：

```yaml
spring:
  profiles:
    active: local
```

确认 `application-local.yml` 中有：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/todo_db
    username: todo_dev
    password: 你的本机密码
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
```

`ddl-auto: update` 仅用于本地学习。密码文件必须被 `.gitignore` 忽略，不要提交。

验证 PostgreSQL：

```powershell
psql -U todo_dev -d todo_db -h localhost -W
```

出现 `todo_db=>` 就成功，输入 `\q` 退出。

## 4. 创建实体目录

在 IDEA 中创建：

```text
backend/src/main/java/com/yanyu/todo/entity
```

实体（Entity）就是“Java 类和数据库表的映射”。`User` 对应用户表，`Todo` 对应任务表。

## 5. 创建 User 实体

新建 `entity/User.java`：

```java
package com.yanyu.todo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    // 这里只保存哈希，M4 使用 BCrypt 生成哈希。
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Todo> todos = new ArrayList<>();

    protected User() {}

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Todo> getTodos() { return todos; }
}
```

关键点：`@Entity` 建立映射，`@Id` 是主键，`unique=true` 保证用户名不重复，`nullable=false` 不允许为空，`@OneToMany` 表示一个用户拥有多个任务。`passwordHash` 不是明文密码。

## 6. 创建 Todo 实体

新建 `entity/Todo.java`：

```java
package com.yanyu.todo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false)
    private boolean important = false;

    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Todo() {}

    public Todo(String title, String description, User user) {
        this.title = title;
        this.description = description;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isImportant() { return important; }
    public void setImportant(boolean important) { this.important = important; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
```

`LocalDate` 只表示日期，符合需求中的 `dueDate`；`@PrePersist` 自动写创建和更新时间，`@PreUpdate` 在更新时刷新 `updatedAt`。`@ManyToOne` 表示多个任务属于一个用户，数据库列名是 `user_id`。

## 7. 创建 Repository

创建目录：

```text
backend/src/main/java/com/yanyu/todo/repository
```

新建 `UserRepository.java`：

```java
package com.yanyu.todo.repository;

import com.yanyu.todo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

新建 `TodoRepository.java`：

```java
package com.yanyu.todo.repository;

import com.yanyu.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findAllByUserId(Long userId);
    Optional<Todo> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndCompleted(Long userId, boolean completed);
    long countByUserIdAndImportant(Long userId, boolean important);
}
```

Spring Data JPA 会根据方法名生成查询，不需要在业务代码中拼接 SQL。特别注意 `findByIdAndUserId`：只按任务 ID 查询可能查到别人的任务，必须同时带上用户 ID。

## 8. 启动并检查建表

在 IDEA 中运行 `TodoApplication`。启动日志不应出现：

```text
password authentication failed
Connection refused
Unable to acquire JDBC Connection
```

然后连接数据库：

```powershell
psql -U todo_dev -d todo_db -h localhost -W
```

查看表：

```sql
\dt
```

应该看到：

```text
users
todos
```

查看字段：

```sql
\d users
\d todos
```

确认 `users` 有 `username`、`password_hash`、`created_at`，`todos` 有 `title`、`completed`、`important`、`due_date`、`created_at`、`updated_at` 和 `user_id`。

## 9. 验证 Repository CRUD

M2 还没有 Controller，可以临时使用 `CommandLineRunner` 调用 Repository。验证逻辑应包含：

```java
User user = userRepository.save(new User("m2_demo", "演示用哈希字符串"));
Todo todo = todoRepository.save(new Todo("完成 M2", "验证 JPA", user));
todo.setCompleted(true);
todoRepository.save(todo);
todoRepository.findAllByUserId(user.getId());
todoRepository.findByIdAndUserId(todo.getId(), user.getId());
todoRepository.deleteById(todo.getId());
```

看到保存、查询、更新和删除均没有异常，即可证明基本 CRUD 可用。验证完成后删除临时 `CommandLineRunner`，否则每次启动都会重复创建演示数据。

注意：`"演示用哈希字符串"` 只用于验证字段能保存，不是安全密码。M4 必须使用 BCrypt，不能把用户输入的明文密码直接保存。

## 10. 验证用户数据隔离

准备两个用户 `userA` 和 `userB`。给 `userA` 创建任务后执行：

```java
todoRepository.findAllByUserId(userB.getId());
todoRepository.findByIdAndUserId(todoOfA, userB.getId());
```

结果必须分别是空列表和 `Optional.empty()`。真正的 HTTP 权限测试属于 M4/M7，但 M2 的 Repository 从现在起就必须按用户过滤。

## 11. 常见问题

### 表没有生成

检查实体是否位于 `TodoApplication` 包 `com.yanyu.todo` 的子包下；检查 `application-local.yml` 是否被正确激活；检查数据库连接参数。

### 找不到 `jakarta.persistence`

确认 `pom.xml` 有 `spring-boot-starter-data-jpa`，并在 IDEA Maven 面板点击 Reload。Spring Boot 4 使用 `jakarta.persistence`，不要照旧教程写成 `javax.persistence`。

### 出现循环 JSON 或密码泄露风险

M3 开始必须使用 DTO，不要直接把 `User` 或 `Todo` 实体作为 API 响应返回；用户响应中绝不能包含 `passwordHash`。

## 12. M2 验收清单

- [ ] 使用 Java 17 和 Spring Boot 4.1.1。
- [ ] 应用能够连接 PostgreSQL。
- [ ] 数据库中出现 `users` 和 `todos` 表。
- [ ] 用户名非空且唯一。
- [ ] 用户表保存的是 `password_hash`，不是明文密码。
- [ ] 任务标题非空，完成和重要状态默认是 `false`。
- [ ] 任务表包含 `user_id` 关系。
- [ ] 创建时间和更新时间由实体自动维护。
- [ ] UserRepository 能按用户名查询和判断重复。
- [ ] TodoRepository 能按用户查询和按任务 ID + 用户 ID 联合查询。
- [ ] 能完成 Repository 保存、查询、更新和删除。
- [ ] userA 查询不到 userB 的任务。
- [ ] 本地密码文件、Token 和依赖目录没有提交到 Git。
- [ ] 已删除临时测试数据代码。

## 13. 提交 M2

确认没有敏感文件后，在项目根目录执行：

```powershell
cd D:\github-copilot\my-todo-list
git status
git add backend/pom.xml backend/src/main/java backend/src/main/resources docs/M2_GUIDE.md
git status
git commit -m "feat: add user and todo persistence layer"
```

如果 `git status` 中出现 `application-local.yml`、`.env.local`、`node_modules` 或 `target`，先不要提交，检查 `.gitignore`。完成 M2 后再进入 M3：DTO、Service、Controller 和请求校验。
