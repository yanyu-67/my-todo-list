# AGENTS.md

## 1. 基本原则
- 变更应最小化，优先修复根因。
- 不做与任务无关的重构。
- 结果要可验证：能运行、能测试、能复现。

## 2. 项目结构
- `backend/`：Spring Boot 后端。
- `frontend/`：Vue 3 前端。
- `docs/`：中文文档与知识库。

## 3. 技术基线（当前）
- 后端：Java 17、Spring Boot 4.1.1、JPA、Security、JWT、PostgreSQL。
- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、Axios。

若文档与代码冲突，以 `backend/pom.xml`、`frontend/package.json` 和实际实现为准。

## 4. 开发约束
- 后端接口保持 RESTful 风格与统一 JSON 响应。
- 涉及认证/权限的修改，必须考虑越权与数据隔离。
- 不在业务代码中直接拼接 SQL 作为主要实现方式。
- 不提交密钥、密码、Token 等敏感信息。

## 5. 文档约束
- 文档使用中文命名和中文内容。
- 过时文档直接删除，不保留重复版本。
- 新增文档应放到 `docs/` 对应分类目录。
- 知识库文件命名格式：`序号_内容.md`。

## 6. 测试与验证
- 后端改动后至少运行：
  - `cd backend && ./mvnw test`
- 前端改动后至少运行：
  - `cd frontend && npm run build`
- 若无法运行测试，需明确说明原因与影响范围。

## 7. 提交规范（建议）
- 提交信息建议使用：`type(scope): summary`
- 常用 type：`feat`、`fix`、`refactor`、`docs`、`test`、`chore`
- 与文档重构相关优先使用：`refactor(docs): ...` 或 `docs: ...`

## 8. 禁止事项
- 未经要求不修改 CI、部署、基础设施配置。
- 未经确认不进行破坏性操作（批量删除、重命名核心目录、重置历史）。
- 不伪造测试结果，不跳过失败说明。
