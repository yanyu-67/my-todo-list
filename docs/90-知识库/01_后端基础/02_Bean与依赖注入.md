# 02_Bean与依赖注入

## 定义
Bean 是 Spring 管理的对象；依赖注入是由容器自动提供依赖。

## 核心点
- 常见注解：`@Component`、`@Service`、`@RestController`。
- 推荐构造器注入。
- 避免在业务代码中手动 `new` 依赖对象。

## 项目落地
- `AuthController(AuthService)`
- `TodoService(TodoRepository, CurrentUserService)`

## 面试快问快答
- 问：构造器注入优势？
- 答：依赖清晰、可测试性好。