# 派生查询

Spring Data JPA 的“派生查询”（Derived Query）知识点，也叫“查询方法名解析”。

你只需要在 Repository 接口中按照固定规则命名方法，Spring Data JPA 就会解析方法名并生成查询。

例如实体类：

```
@Entity
public class Todo {

    private Long id;
    private String title;
    private Boolean completed;
    private LocalDate dueDate;
}
```

Repository：

```
public interface TodoRepository
        extends JpaRepository<Todo, Long> {

    List<Todo> findByCompleted(Boolean completed);

    List<Todo> findByTitleContaining(String keyword);

    Optional<Todo> findById(Long id);

    long countByCompleted(Boolean completed);

    boolean existsByTitle(String title);

    void deleteByCompleted(Boolean completed);
}
```

大致会解析成：

```
where completed = ?
where title like '%?%'
where id = ?
select count(...)
select exists(...)
delete ...
```

## 基本命名结构

```
查询类型 + By + 实体属性 + 条件关键字
```

例如：

```
findByTitleContaining
│    │  │     │
│    │  │     └─ 包含匹配
│    │  └────── 实体属性 title
│    └──────── 条件开始
└──────────── 查询类型
```

## 常见查询前缀

| 方法前缀    | 作用         |
| ----------- | ------------ |
| `findBy`    | 查询         |
| `findAllBy` | 查询多个     |
| `getBy`     | 查询         |
| `readBy`    | 查询         |
| `queryBy`   | 查询         |
| `countBy`   | 统计数量     |
| `existsBy`  | 判断是否存在 |
| `deleteBy`  | 删除         |
| `removeBy`  | 删除         |

## 常见条件关键字

```
findByTitle(String title)
```

等于：

```
title = ?
findByTitleAndCompleted(String title, Boolean completed)
```

等于：

```
title = ? and completed = ?
findByTitleOrDescription(String title, String description)
```

等于：

```
title = ? or description = ?
findByTitleContaining(String keyword)
```

等于：

```
title like %keyword%
findByTitleStartingWith(String prefix)
findByTitleEndingWith(String suffix)
findByTitleLike(String pattern)
findByPriorityGreaterThan(Integer priority)
findByPriorityLessThan(Integer priority)
findByPriorityBetween(Integer min, Integer max)
findByCompletedIsTrue()
findByCompletedIsFalse()
findByDescriptionIsNull()
findByDescriptionIsNotNull()
findByIdIn(List<Long> ids)
```

等于：

```
id in (...)
findByTitleIgnoreCase(String title)
```

忽略大小写。

```
findByCompletedOrderByDueDateAsc(Boolean completed)
findByCompletedOrderByDueDateDesc(Boolean completed)
```

按截止日期升序或降序排列。

## 是否有命名限制？

有，而且限制比较严格。

### 1. 属性名必须存在于实体中

如果实体是：

```
private String title;
```

应该写：

```
findByTitle(String title)
```

不能写：

```
findByName(String name)
```

否则启动时可能报错：

```
No property 'name' found for type 'Todo'
```

### 2. 方法名必须符合 Spring Data 关键字规则

正确：

```
findByTitleContaining
```

不正确：

```
findTitleLike
```

因为 Spring Data 需要通过 `By` 判断条件部分从哪里开始。

### 3. Java 属性名使用驼峰规则

实体：

```
private LocalDateTime createdAt;
```

方法：

```
findByCreatedAtAfter(LocalDateTime time)
```

不是：

```
findByCreatedatAfter(...)
```

### 4. 多条件参数顺序必须一致

```
findByTitleAndCompleted(String title, Boolean completed)
```

参数顺序必须对应：

```
title → String
completed → Boolean
```

### 5. 关联对象可以继续向下查询

例如：

```
class Todo {
    private User user;
}
```

可以写：

```
List<Todo> findByUserUsername(String username);
```

它表示：

```
Todo.user.username
```

如果属性名边界不明确，可以使用下划线：

```
findByUser_Username(String username)
```

下划线在这里不是数据库字段名，而是告诉 Spring Data 如何拆分属性路径。

### 6. 方法名太长时不适合继续派生

例如：

```
findByTitleContainingAndCompletedAndPriorityGreaterThanAndDueDateBetweenOrderByCreatedAtDesc(...)
```

虽然可能合法，但可读性很差。复杂查询建议使用：

```
@Query
```

或者使用 Specification、QueryDSL 等方式。

## 结合 Todo 项目的例子

```
public interface TodoRepository
        extends JpaRepository<Todo, Long> {

    // 查询当前用户的所有未完成任务，并按创建时间倒序排列
    List<Todo> findByUserIdAndCompletedOrderByCreatedAtDesc(
            Long userId,
            Boolean completed
    );

    // 根据标题进行模糊查询
    List<Todo> findByUserIdAndTitleContaining(
            Long userId,
            String keyword
    );

    // 统计用户的已完成任务数量
    long countByUserIdAndCompleted(
            Long userId,
            Boolean completed
    );

    // 判断用户是否存在指定标题的任务
    boolean existsByUserIdAndTitle(
            Long userId,
            String title
    );
}
```

核心记忆方式是：

```
findBy + 属性名 + 条件关键字
```

例如：

```
findByTitleContainingAndCompletedOrderByCreatedAtDesc
```

可以拆成：

```
查询
  ↓
title 包含某内容
  ↓
并且 completed 等于某值
  ↓
按照 createdAt 倒序
```

这套规则由 Spring Data JPA 的 Repository 代理解析，官方文档也明确说明，Repository 方法会根据方法名推导查询，或者使用手写的 `@Query` 查询。[Spring Data JPA 官方文档](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html)