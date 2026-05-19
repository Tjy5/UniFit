import { expect, test, type Locator } from '@playwright/test';
import { installApiMockGuard } from './api-mock-guard';

async function expectMinBorderRadius(locator: Locator, minimum: number) {
  const radius = await locator.evaluate((element) => {
    const style = window.getComputedStyle(element);
    return Number.parseFloat(style.borderTopLeftRadius || style.borderRadius || '0');
  });

  expect(radius).toBeGreaterThanOrEqual(minimum);
}

test('user-vue login page renders the MCM auth card', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.goto('http://127.0.0.1:3000/login');

  await expect(page.getByRole('heading', { name: '欢迎回来' })).toBeVisible();
  const loginCard = page.locator('.login-card');
  await expect(loginCard).toBeVisible();
  await expectMinBorderRadius(loginCard, 12);
  await expect(page.locator('.auth-page__eyebrow')).toHaveText('校服订购账号');
  apiGuard.assertNoUnexpectedApiRequests();
});

test('user-vue reviews page keeps the account panel rounded and populated', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'smoke-user-token');
    window.localStorage.setItem('userId', '42');
  });

  await page.route('**/api/auth/validate', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ ok: true }),
    });
  });

  await page.route('**/api/s-reviews/user**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        list: [
          {
            reviewId: 101,
            displayName: '测试用户',
            uniformName: '秋季制服',
            rating: 5,
            isAnonymous: false,
            content: '版型很稳，面料也舒服。',
            createTime: '2026-03-31T09:00:00',
            uniformId: 8,
            orderItemId: 12,
          },
        ],
        total: 1,
      }),
    });
  });

  await page.goto('http://127.0.0.1:3000/user-reviews');

  await expect(page.getByRole('heading', { name: '我的评价' })).toBeVisible();
  await expect(page.getByText('集中查看已提交评价，并快速修改评分与评价内容。')).toBeVisible();
  await expect(page.getByRole('heading', { name: '秋季制服' })).toBeVisible();
  await expect(page.getByText('1 条记录')).toBeVisible();

  const panel = page.getByTestId('user-reviews-panel');
  const table = page.locator('.review-card');
  await expect(panel).toBeVisible();
  await expect(table).toBeVisible();
  await expectMinBorderRadius(panel, 12);
  await expectMinBorderRadius(table, 12);
  apiGuard.assertNoUnexpectedApiRequests();
});

test('admin-web login page renders the MCM admin panel', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.goto('http://127.0.0.1:5173/login');

  await expect(page.getByRole('heading', { name: '校服智能订购管理后台' })).toBeVisible();
  const loginPanel = page.locator('.login-page__panel');
  await expect(loginPanel).toBeVisible();
  await expectMinBorderRadius(loginPanel, 12);
  apiGuard.assertNoUnexpectedApiRequests();
});

test('admin-web dashboard renders authenticated MCM stats with mocked data', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.addInitScript(() => {
    window.localStorage.setItem('suios-admin-token', 'smoke-admin-token');
  });

  await page.route('**/admin-api/auth/info', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: 'ok',
        data: {
          userId: 1,
          username: 'admin',
          nickname: 'MCM Admin',
          avatar: '',
        },
      }),
    });
  });

  await page.route('**/admin-api/dashboard/stats', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: 'ok',
        data: {
          uniformTotal: 48,
          todayOrders: 12,
          pendingReviews: 3,
          todayVisits: 97,
          orderTrend: [
            { day: '2026-03-25', orderCount: 6 },
            { day: '2026-03-26', orderCount: 8 },
            { day: '2026-03-27', orderCount: 11 },
            { day: '2026-03-28', orderCount: 9 },
            { day: '2026-03-29', orderCount: 13 },
            { day: '2026-03-30', orderCount: 10 },
            { day: '2026-03-31', orderCount: 12 },
          ],
          recentOrders: [
            {
              id: 3001,
              userAccount: 'alice',
              totalPrice: 299,
              status: 2,
              orderDate: '2026-03-31T10:00:00',
            },
          ],
        },
      }),
    });
  });

  await page.goto('http://127.0.0.1:5173/');

  await expect(page.getByRole('heading', { name: '运营工作台' })).toBeVisible();
  await expect(page.getByText('校服总数')).toBeVisible();
  await expect(page.getByText('最近订单')).toBeVisible();
  await expect(page.getByText('alice')).toBeVisible();

  const statCards = page.locator('.dashboard__stat');
  await expect(statCards).toHaveCount(4);
  await expectMinBorderRadius(statCards.first(), 12);
  apiGuard.assertNoUnexpectedApiRequests();
});
