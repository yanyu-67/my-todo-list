# 常见Springboot注解有哪些

## 核心启动与配置

- @SpringBootApplication：启动类的标配，是一个组合注解，

  =@Configuration + @EnableAutoConfiguration （自动配置）+ @ComponentScan（组件扫描）

- @Configuration / @Bean：Java配置方式
  - 类：用@Configuration 声明这是一个配置类
  - 方法：用@Bean吧方法返回值注册为容器管理的Bean
- @ConfigurationProperties：批量绑定配置
  - 把`application.yml` 里的一整组属性绑定到一个Java对象上

## Bean 定义与依赖注入

- **`@Component` / `@Service` / `@Repository` / `@Controller`**：**声明 Bean**。
  - 它们本质都是 `@Component`，分别用于通用组件、业务层、数据层和控制层，让代码结构更清晰。
- **`@Autowired`**：**自动注入**。
- **`@Qualifier`**：**精确制导**。当同类型的 Bean 有多个时（比如有多个 `DataSource`），用它来指定具体要注入哪一个。
- **`@Primary`**：**默认首选**。 Spring 默认优先选它。

## Web 与请求处理

- **`@RestController`**：**声明 REST 控制器**。= `@Controller` + `@ResponseBody` ，
  - 方法返回值会自动序列化成 JSON，是前后端分离项目的首选。
- **`@RequestMapping` 家族**：**映射 URL**。
  - `@RequestMapping`：通用的映射，可指定路径和方法。
  - **`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`**
    - 分别对应 HTTP 的 **GET、POST、PUT、DELETE** 方法
- **参数绑定三剑客**：**取数据**。
  - **`@PathVariable`**：取 URL 路径里的变量（如 `/users/{id}` 里的 `id`）。
  - **`@RequestParam`**：取查询参数（如 `?name=xxx`）。
  - **`@RequestBody`**：取请求体（通常是 JSON），反序列化成 Java 对象

## 数据与事务

- **`@Transactional`**：**声明式事务**。加在方法或类上，Spring 会自动管理事务的开启、提交或回滚。
- **`@Entity` / `@Id` / `@Table`**：**JPA 实体映射**。配合 JPA 使用，把 Java 类映射到数据库表和字段