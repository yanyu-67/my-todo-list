# 04_事务在Service层的使用

## 定义
事务保证一组操作的一致性。

## 核心点
- 事务边界通常放在 Service 层。
- 查询可用 `readOnly=true`。
- 出现异常时需要回滚。

## 项目落地
- `AuthService`、`TodoService` 使用 `@Transactional`。

## 面试快问快答
- 问：为什么事务不放 Controller？
- 答：Controller 不是业务边界。