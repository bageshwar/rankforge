import { Page } from '@playwright/test';

/**
 * General test utilities
 */

export async function waitForApiCalls(page: Page, timeout: number = 10000) {
  // Wait for network to be idle
  await page.waitForLoadState('networkidle', { timeout });
}

export async function extractTextContent(page: Page, selector: string): Promise<string> {
  const element = await page.locator(selector).first();
  return (await element.textContent()) || '';
}

export function extractSteamId(fullId: string): string {
  if (!fullId) return '';
  const match = fullId.match(/\[U:\d+:(\d+)\]/);
  return match ? match[1] : fullId;
}

export async function waitForElementVisible(page: Page, selector: string, timeout: number = 5000) {
  await page.waitForSelector(selector, { state: 'visible', timeout });
}

export async function waitForElementHidden(page: Page, selector: string, timeout: number = 5000) {
  await page.waitForSelector(selector, { state: 'hidden', timeout });
}
