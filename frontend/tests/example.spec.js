// @ts-check
import { test, expect } from '@playwright/test';

test('首页标题包含 DeepSeek', async ({ page }) => {
  await page.goto('https://www.deepseek.com/');

  // 只校验品牌关键词，避免标题文案微调导致测试失败。
  await expect(page).toHaveTitle(/DeepSeek/);
});

test('首页存在指向 API 开放平台的链接', async ({ page }) => {
  // 强制走英文版，避免站点按语言/地域自动跳转导致断言不稳定。
  await page.goto('https://www.deepseek.com/en/');

  // 同一 href 在页面中出现多次（顶部导航、主体、页脚），取第一个即可。
  const apiPlatformLink = page
    .locator('a[href="https://platform.deepseek.com/"]')
    .first();

  await expect(apiPlatformLink).toBeVisible();
});