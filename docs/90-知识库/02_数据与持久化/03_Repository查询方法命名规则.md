# 03_Repository查询方法命名规则

## 定义
Spring Data JPA 可通过方法名自动生成查询。

## 核心点
- 常见前缀：`findBy`、`existsBy`。
- 支持组合条件：`findByIdAndUserId`。
- 简单查询可读性高。

## 项目落地
- `existsByUsername`
- `findAllByUserId`
- `findByIdAndUserId`

## 面试快问快答
- 问：复杂查询怎么办？
- 答：可用自定义查询或 Specification。