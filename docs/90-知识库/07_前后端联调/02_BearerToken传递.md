# 02_BearerToken传递

## 定义
Bearer Token 是标准的认证头传递方式。

## 核心点
- 格式：`Authorization: Bearer <token>`。
- 统一由拦截器注入，减少漏传。

## 项目落地
- `api/http.js` 请求拦截器设置 Authorization。

## 面试快问快答
- 问：为什么不把 token 放 URL？
- 答：容易泄漏在日志和历史记录中。