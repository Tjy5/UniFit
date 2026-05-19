import type { Page, Route } from '@playwright/test';

const guardedApiPattern = /^\/(?:api|admin-api)(?:\/|$)/;

export async function installApiMockGuard(page: Page) {
  const unhandledApiRequests: string[] = [];

  await page.route('**/*', async (route: Route) => {
    const url = route.request().url();
    if (guardedApiPattern.test(new URL(url).pathname)) {
      unhandledApiRequests.push(url);
      await route.abort('failed');
      return;
    }

    await route.fallback();
  });

  return {
    assertNoUnexpectedApiRequests() {
      if (unhandledApiRequests.length > 0) {
        throw new Error(
          `Unexpected unmocked API request(s):\n${unhandledApiRequests.map((url) => `- ${url}`).join('\n')}`,
        );
      }
    },
  };
}
