# Playwright 常用命令速查

## 核心命令

| 命令                                     | 作用                                             |
| ---------------------------------------- | ------------------------------------------------ |
| `npx playwright test`                    | 运行所有 E2E 测试                                |
| `npx playwright test --ui`               | 打开交互式 UI 模式（可视化调试，推荐写测试时用） |
| `npx playwright test --project=chromium` | 只在 Desktop Chrome 上运行                       |
| `npx playwright test example`            | 只运行指定文件（如 `example.spec.js`）           |
| `npx playwright test --debug`            | 调试模式，逐步执行                               |
| `npx playwright codegen`                 | 自动生成测试代码（录制操作）                     |

## 关键文件

- `tests/example.spec.js` — 示例端到端测试
- `playwright.config.js` — Playwright 测试配置

## 快速开始

```bash
npx playwright test