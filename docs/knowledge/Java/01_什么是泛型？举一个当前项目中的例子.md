# 什么是泛型？举一个当前项目中的例子

**泛型：把“类型”也变成一种参数，写代码时先不写死具体类型，用的时候再传进去。**

泛型作用：**只写一份代码就能适配所有类型（实体：User/Todo），同时还不丢失类型安全。**

类型安全：**编译器在编译阶段就能发现用错了类型，而不是等到运行时才崩溃。**

```java
public interface TodoRepository extends JpaRepository<Todo,Long> {
    List<Todo> findAllByUserId(Long userId);
    Optional<Todo> findByIdAndUserId(Long id,Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndCompleted(Long userId,boolean completed);
    long countByUserIdAndImportant(Long userId,boolean important);
```

`JpaRepository` 是一个**泛型接口**，定义（简化）：

```Java
public interface JpaRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    void deleteById(ID id);
    List<T> findAll();
    // ...
}
```

- `T`：这个仓库管理的**实体类型**
- `ID`：这个实体的**主键类型**

例子中JpaRepository<Todo,Long>

- `T` = `Todo` → 这个仓库管的是 `Todo` 实体
- `ID` = `Long` → `Todo` 的主键是 `Long` 类型

