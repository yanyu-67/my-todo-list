# 02_RESTful接口路径设计

## 定义
用资源路径 + HTTP 方法表达接口语义。

## 核心点
- 集合资源：`/api/todos`
- 单项资源：`/api/todos/{id}`
- 局部更新：`PATCH`

## 项目落地
- 任务完成状态使用 `PATCH /api/todos/{id}/complete`。

## 面试快问快答
- 问：PUT 和 PATCH 区别？
- 答：PUT 偏整体更新，PATCH 偏部分更新。