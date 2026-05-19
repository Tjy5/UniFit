import { expect, test } from '@playwright/test';
import { installApiMockGuard } from './api-mock-guard';

function mockCommonUserApis(page: import('@playwright/test').Page) {
  return Promise.all([
    page.route('**/api/auth/validate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ ok: true }),
      });
    }),
    page.route('**/api/s-schools/selectList', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ schoolId: 1, schoolName: '阳光中学' }]),
      });
    }),
    page.route('**/api/s-grades/listBySchool**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ gradeId: 1, gradeName: '初一' }]),
      });
    }),
    page.route('**/api/s-sizes/all', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 1, sizeName: '160' },
          { id: 2, sizeName: '165' },
          { id: 3, sizeName: '170' },
        ]),
      });
    }),
    page.route('**/api/user-message/**', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({ status: 200, contentType: 'text/plain', body: '更新成功' });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          messageUserName: '测试用户',
          messageUserAge: 13,
          messageUserSex: '男',
          height: 165,
          weight: 52,
          chest: 84,
          waist: 72,
          hip: 89,
          shoulder: 39,
          schoolId: 1,
          gradeId: 1,
        }),
      });
    }),
    page.route('**/api/size-recommendations/recommend', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          available: true,
          message: '已基于身高、体重、胸围为您推荐 165 码',
          confidence: 88,
          confidenceLevel: 'MEDIUM',
          confidenceMessage: '基础维度可支撑推荐，建议结合尺码表进一步确认',
          lowConfidence: false,
          reasons: ['已结合胸围优化推荐结果', '身高位于建议区间内'],
          recommended: {
            sizeName: '165',
            score: 88,
            reasons: ['主推荐尺码在关键维度内更稳定'],
            dimensionMatches: [
              { dimensionKey: 'height', label: '身高', userValue: 165, minValue: 160, maxValue: 170, matchPercent: 94, withinRange: true, message: '身高位于建议区间内' },
              { dimensionKey: 'chest', label: '胸围', userValue: 84, minValue: 82, maxValue: 88, matchPercent: 90, withinRange: true, message: '胸围位于建议区间内' },
            ],
          },
          alternatives: [
            {
              sizeName: '160',
              score: 76,
              reasons: ['胸围略偏紧，适合作为偏修身备选'],
              dimensionMatches: [
                { dimensionKey: 'height', label: '身高', userValue: 165, minValue: 155, maxValue: 165, matchPercent: 72, withinRange: true, message: '身高位于建议区间边缘，建议关注松紧度' },
                { dimensionKey: 'chest', label: '胸围', userValue: 84, minValue: 78, maxValue: 84, matchPercent: 68, withinRange: true, message: '胸围位于建议区间边缘，建议关注松紧度' },
              ],
            },
            {
              sizeName: '170',
              score: 70,
              reasons: ['整体更宽松，可作为舒适度备选'],
              dimensionMatches: [
                { dimensionKey: 'height', label: '身高', userValue: 165, minValue: 165, maxValue: 175, matchPercent: 74, withinRange: true, message: '身高位于建议区间边缘，建议关注松紧度' },
                { dimensionKey: 'chest', label: '胸围', userValue: 84, minValue: 88, maxValue: 94, matchPercent: 60, withinRange: false, message: '胸围低于建议区间，穿着会更宽松' },
              ],
            },
          ],
        }),
      });
    }),
    page.route('**/api/size-preferences**', async (route) => {
      if (route.request().method() === 'PUT') {
        await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          explicitPreference: 'STANDARD',
          learnedDirection: 'STANDARD',
          confidenceLevel: 'LOW',
          sampleCount: 0,
        }),
      });
    }),
  ]);
}

test('mall page renders unified recommendation panel', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'smoke-user-token');
    window.localStorage.setItem('userId', '42');
  });

  await mockCommonUserApis(page);
  await page.route('**/api/s-uniform/active', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 8,
          name: '春季制服',
          price: 199,
          intro: '轻便透气',
          image: '',
          schoolId: 1,
          schoolName: '阳光中学',
          gradeId: 1,
          gradeName: '初一',
        },
      ]),
    });
  });
  await page.route('**/api/s-wishlist/list', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
  });

  await page.goto('http://127.0.0.1:3000/mall');

  await expect(page.getByRole('heading', { name: '查找并预订校服' })).toBeVisible();
  await page.getByRole('button', { name: '快速选购' }).click();
  await expect(page.getByText('智能尺码推荐')).toBeVisible();
  await expect(page.locator('.size-panel__title').getByText('165 码')).toBeVisible();
  await page.getByRole('button', { name: '查看其他尺码' }).click();
  await expect(page.getByText('尺码匹配度对比')).toBeVisible();
  await expect(page.getByText('主推荐', { exact: true })).toBeVisible();
  await expect(page.getByText('备选 1', { exact: true })).toBeVisible();
  await expect(page.locator('.size-panel__candidate-size').getByText('170 码')).toBeVisible();
  apiGuard.assertNoUnexpectedApiRequests();
});

test('user profile renders extended measurement fields', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'smoke-user-token');
    window.localStorage.setItem('userId', '42');
  });

  await mockCommonUserApis(page);
  await page.route('**/api/s-addresses', async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
  });

  await page.goto('http://127.0.0.1:3000/profile');

  await expect(page.getByRole('button', { name: '如何测量' })).toBeVisible();
  await expect(page.getByText('胸围 (cm)')).toBeVisible();
  await expect(page.getByText('肩宽 (cm)')).toBeVisible();
  apiGuard.assertNoUnexpectedApiRequests();
});

test('my orders page opens non-blocking size feedback dialog', async ({ page }) => {
  const apiGuard = await installApiMockGuard(page);
  await page.addInitScript(() => {
    window.localStorage.setItem('token', 'smoke-user-token');
    window.localStorage.setItem('userId', '42');
  });

  await mockCommonUserApis(page);
  await page.route('**/api/s-orders/user', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        {
          id: 88,
          orderDate: '2026-04-01T10:00:00',
          totalPrice: 199,
          status: 3,
          paymentStatusCode: 'PAID',
          shippingStatusCode: 'DELIVERED',
          shippingAddress: {
            recipientName: '测试用户',
            phoneNumber: '13800138000',
            province: '广东省',
            city: '深圳市',
            district: '南山区',
            streetAddress: '科技园 1 号',
          },
          orderItems: [
            {
              orderItemId: 12,
              uniformId: 8,
              uniformNameSnapshot: '春季制服',
              sizeNameSnapshot: '160',
              quantity: 1,
              unitPriceSnapshot: 199,
              itemTotalPrice: 199,
              imageSnapshot: '',
              reviewId: null,
            },
          ],
        },
      ]),
    });
  });
  await page.route('**/api/size-feedback/order-items/12', async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          orderItemId: 12,
          satisfaction: 'FIT',
          purchasedSize: '160',
          recommendedSize: '165',
        }),
      });
      return;
    }
    await route.fulfill({ status: 404, body: '' });
  });
  await page.route('**/api/size-feedback/order-items/12/recommendation', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        available: true,
        recommended: {
          sizeName: '165',
          score: 88,
          reasons: ['历史推荐结果可作为反馈参考'],
          dimensionMatches: [],
        },
        alternatives: [],
      }),
    });
  });

  await page.goto('http://127.0.0.1:3000/my-orders');

  await expect(page.getByText('我的订单')).toBeVisible();
  await page.getByRole('button', { name: '尺码反馈' }).click();
  await expect(page.getByRole('dialog', { name: '尺码反馈' })).toBeVisible();
  await expect(page.getByText('已购尺码：160')).toBeVisible();
  apiGuard.assertNoUnexpectedApiRequests();
});
