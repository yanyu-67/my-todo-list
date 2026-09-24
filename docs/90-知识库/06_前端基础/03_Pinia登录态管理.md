# 03_Pinia登录态管理

## 定义
Pinia 用于管理跨页面共享状态。

## 核心点
- 统一保存 token 与 username。
- 暴露登录态判断。
- 提供登录写入与退出清理方法。

## 项目落地
- `stores/auth.js` 管理登录态并同步 localStorage。

## 面试快问快答
- 问：为什么登录态放全局 store？
- 答：多个页面都依赖该状态。