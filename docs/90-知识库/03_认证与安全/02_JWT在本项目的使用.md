# 02_JWT在本项目的使用

## 定义
JWT 是无状态认证令牌。

## 核心点
- 登录成功后签发 token。
- 前端通过 Bearer 方式携带 token。
- 过滤器解析 token 并建立认证上下文。

## 项目落地
- `JwtService`、`JwtAuthenticationFilter`。

## 面试快问快答
- 问：JWT 最大特点？
- 答：无状态，适合前后端分离。