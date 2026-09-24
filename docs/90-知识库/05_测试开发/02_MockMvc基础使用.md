# 02_MockMvc基础使用

## 定义
MockMvc 用于测试 Spring MVC 接口，不必启动真实端口。

## 核心点
- 发请求：`get/post/patch/delete`。
- 断言：状态码、`jsonPath`。
- 适合集成测试接口行为。

## 项目落地
- `AuthIntegrationTest` 使用 MockMvc 验证认证接口。

## 面试快问快答
- 问：MockMvc 和手工调接口差别？
- 答：MockMvc 可自动化回归。