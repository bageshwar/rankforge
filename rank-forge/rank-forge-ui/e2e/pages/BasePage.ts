import { Page, Locator } from '@playwright/test';

export class BasePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  async goto(path: string = '/') {
    console.log(`[BasePage] Navigating to: ${path}`);
    await this.page.goto(path);
    console.log(`[BasePage] Navigation complete to: ${path}`);
  }

  async waitForLoadState(state: 'load' | 'domcontentloaded' | 'networkidle' = 'networkidle', timeout: number = 70000) {
    console.log(`[BasePage] Waiting for load state: ${state} (timeout: ${timeout}ms)`);
    await this.page.waitForLoadState(state, { timeout });
    console.log(`[BasePage] Load state ${state} reached`);
  }

  async waitForApiResponse(urlPattern: string | RegExp, timeout: number = 70000) {
    const patternStr = typeof urlPattern === 'string' ? urlPattern : urlPattern.toString();
    console.log(`[BasePage] Waiting for API response matching: ${patternStr} (timeout: ${timeout}ms)`);
    const startTime = Date.now();
    
    try {
      await this.page.waitForResponse(
        (response) => {
          const url = response.url();
          const matches = typeof urlPattern === 'string' ? url.includes(urlPattern) : urlPattern.test(url);
          if (matches) {
            const elapsed = Date.now() - startTime;
            console.log(`[BasePage] ✓ API response received: ${url} (after ${elapsed}ms)`);
          }
          return matches;
        },
        { timeout }
      );
      const elapsed = Date.now() - startTime;
      console.log(`[BasePage] API response wait complete (total: ${elapsed}ms)`);
    } catch (error) {
      const elapsed = Date.now() - startTime;
      console.log(`[BasePage] ⚠ API response wait timed out after ${elapsed}ms - response may have already completed`);
      // Don't throw - the data might already be loaded
    }
  }

  async waitForApiResponseOrDataLoaded(
    urlPattern: string | RegExp, 
    dataSelector: string, 
    timeout: number = 70000
  ) {
    const patternStr = typeof urlPattern === 'string' ? urlPattern : urlPattern.toString();
    console.log(`[BasePage] Waiting for API response (${patternStr}) OR data loaded (${dataSelector})`);
    
    // First check if data is already loaded
    try {
      const element = this.page.locator(dataSelector).first();
      const isVisible = await element.isVisible({ timeout: 1000 }).catch(() => false);
      if (isVisible) {
        console.log(`[BasePage] ✓ Data already loaded (${dataSelector} is visible), skipping API wait`);
        return;
      }
    } catch {
      // Data not loaded yet, continue with waiting
    }
    
    // Set up response listener before checking again
    const responsePromise = this.page.waitForResponse(
      (response) => {
        const url = response.url();
        return typeof urlPattern === 'string' ? url.includes(urlPattern) : urlPattern.test(url);
      },
      { timeout }
    );
    
    // Also wait for data selector
    const dataPromise = this.page.waitForSelector(dataSelector, { timeout, state: 'visible' }).catch(() => null);
    
    // Wait for either response or data to be loaded
    try {
      await Promise.race([responsePromise, dataPromise]);
      console.log(`[BasePage] ✓ Either API response received or data loaded`);
    } catch (error) {
      console.log(`[BasePage] ⚠ Wait timed out, but data might be loaded`);
    }
  }

  async getTextContent(selector: string): Promise<string | null> {
    const element = await this.page.locator(selector).first();
    return await element.textContent();
  }

  async isVisible(selector: string): Promise<boolean> {
    const element = await this.page.locator(selector).first();
    return await element.isVisible();
  }

  async click(selector: string) {
    await this.page.locator(selector).first().click();
  }

  async waitForSelector(selector: string, timeout: number = 30000) {
    console.log(`[BasePage] Waiting for selector: ${selector} (timeout: ${timeout}ms)`);
    const startTime = Date.now();
    await this.page.waitForSelector(selector, { timeout });
    const elapsed = Date.now() - startTime;
    console.log(`[BasePage] ✓ Selector found: ${selector} (after ${elapsed}ms)`);
  }
}
