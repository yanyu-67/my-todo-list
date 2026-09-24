# 什么是 Bean？

## JavaBean（一种编码规范）

在 Java 编程里，Bean 通常指一种**符合特定规范**的 Java 类，用来**封装数据或业务逻辑**。

规范有：

1. **类是 public 的**
2. **有无参构造方法**（方便框架通过反射创建对象）
3. **属性私有**（`private`）
4. **通过 getter / setter 访问属性**
5. **通常实现 `Serializable` 接口**（便于序列化）

~~~java
public class User implements Serializable {
    private String name;
    private int age;

    public User() {}                 // 无参构造

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
}

这种类常被用作 POJO（Plain Old Java Object），在 MVC 里做数据载体，比如 VO、DTO、Entity。
~~~

### 为什么需要这种规范？

因为框架（如Spring、MyBatis）可以通过反射统一操作：

- 看到`getName()`就知道有个`name`属性
- 看到`setName()`就能注入值
- 不需要写额外的配置

## Spring Bean（由容器管理的对象）

**Bean**：**交给Spring IoC容器创建和管理的对象**

~~~Java
@Component
public class UserService {
    public void hello() {
        System.out.println("hello");
    }
}
~~~

这个 `UserService` 被 `@Component` 标记后，Spring 启动时会：

1. 扫描到它
2. 通过反射创建实例
3. 放进 IoC 容器（一个对象池）
4. 需要时注入到别的地方