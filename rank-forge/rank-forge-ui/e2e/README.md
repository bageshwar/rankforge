# E2E Testing Guide

This directory contains end-to-end tests for the RankForge UI using Playwright.

## Prerequisites

1. **Node.js** and **npm** installed
2. **Staging backend** running on `http://localhost:8080` with seeded data
3. **Playwright browsers** installed (run `npx playwright install` after installing dependencies)

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Install Playwright browsers:
   ```bash
   npx playwright install
   ```

3. Ensure your staging backend is running with seeded data:
   ```bash
   # Start your backend server on localhost:8080
   ```

4. (Optional) Capture API responses to generate test fixtures:
   ```bash
   npm run test:e2e:capture-api
   ```
   This will create `e2e/fixtures/captured-test-data.ts` with expected values from your staging instance.

## Running Tests

### Run all E2E tests
```bash
npm run test:e2e
```

### Run tests in UI mode (interactive)
```bash
npm run test:e2e:ui
```

### Run tests in debug mode
```bash
npm run test:e2e:debug
```

### Run a specific test file
```bash
npx playwright test e2e/home.spec.ts
```

### Run tests in headed mode (see browser)
```bash
npx playwright test --headed
```

## Test Structure

### Page Object Model

Tests use the Page Object Model pattern for maintainability:

- `e2e/pages/BasePage.ts` - Base page with common utilities
- `e2e/pages/HomePage.ts` - Home page interactions
- `e2e/pages/GamesPage.ts` - Games listing page
- `e2e/pages/GameDetailsPage.ts` - Game details page
- `e2e/pages/RoundDetailsPage.ts` - Round details page
- `e2e/pages/RankingsPage.ts` - Rankings page
- `e2e/pages/PlayerProfilePage.ts` - Player profile page

### Test Files

- `e2e/home.spec.ts` - Home page tests
- `e2e/games.spec.ts` - Games page tests
- `e2e/game-details.spec.ts` - Game details page tests
- `e2e/round-details.spec.ts` - Round details page tests
- `e2e/player-profile.spec.ts` - Player profile page tests
- `e2e/navigation.spec.ts` - Navigation and profile link tests
- `e2e/rankings.spec.ts` - Rankings page tests

### Test Data Fixtures

- `e2e/fixtures/test-data.ts` - TypeScript interfaces for test data
- `e2e/fixtures/captured-test-data.ts` - Auto-generated from API capture script

### Utilities

- `e2e/utils/helpers.ts` - General test utilities
- `e2e/utils/api-capture.ts` - Script to capture API responses from staging

## Test Coverage

The E2E test suite covers:

1. **Home Page**
   - Rankings and games links visibility
   - Navigation to rankings and games pages

2. **Games Page**
   - Display of 2+ games
   - Game table structure
   - Filter controls (All Games, Recent 10, Recent 25)
   - Navigation to game details

3. **Game Details Page**
   - Game header (map, score, date, duration, rounds)
   - Round timeline with clickable round badges
   - Player statistics table
   - Accolades section
   - Navigation to round details

4. **Round Details Page**
   - Round header and winner team
   - Round statistics
   - Bomb status indicators
   - Event timeline
   - Kill feed
   - Navigation back to game details

5. **Player Profile Page**
   - Profile header (name, Steam ID, rank, K/D)
   - Stats grid
   - Rating progression chart
   - Accolades overview and recent accolades
   - Navigation back to rankings

6. **Navigation**
   - Profile links from rankings page
   - Profile links from game details player stats
   - Profile links from game details accolades
   - Profile links from round details timeline
   - Profile links from round details kill feed

7. **Rankings Page**
   - Rankings table structure
   - Filter controls (All Players, Top 10, Top 25)
   - Rank icons for top 3 players
   - Player links navigation

## Updating Test Data Fixtures

When your staging data changes, update the test fixtures:

1. Ensure staging backend is running with the latest seeded data
2. Run the API capture script:
   ```bash
   npm run test:e2e:capture-api
   ```
3. Review the generated `e2e/fixtures/captured-test-data.ts` file
4. Update test assertions if needed based on the captured data

## Adding New Tests

1. Create a new test file in `e2e/` directory (e.g., `e2e/new-feature.spec.ts`)
2. Import necessary page objects and test utilities
3. Write tests using Playwright's test API
4. Use page objects for interactions and assertions
5. Follow existing test patterns for consistency

Example:
```typescript
import { test, expect } from '@playwright/test';
import { SomePage } from './pages/SomePage';

test.describe('New Feature', () => {
  test('should do something', async ({ page }) => {
    const somePage = new SomePage(page);
    await somePage.navigate();
    // ... test code
  });
});
```

## CI/CD Integration

For CI/CD pipelines:

1. Install dependencies: `npm install`
2. Install Playwright browsers: `npx playwright install --with-deps`
3. Start the backend server (or use a test database)
4. Run tests: `npm run test:e2e`

Example GitHub Actions workflow:
```yaml
- name: Install dependencies
  run: npm install

- name: Install Playwright browsers
  run: npx playwright install --with-deps

- name: Run E2E tests
  run: npm run test:e2e
  env:
    API_BASE_URL: ${{ secrets.STAGING_API_URL }}
```

## Troubleshooting

### Tests fail with "Navigation timeout"
- Ensure the frontend dev server is running (`npm run dev`)
- Check that the backend is accessible at `http://localhost:8080`

### Tests fail with "Element not found"
- Verify the staging backend has the expected data
- Check that test IDs are correctly added to components
- Ensure API responses match expected structure

### Tests are flaky
- Increase timeout values in `playwright.config.ts`
- Add explicit waits for API calls using `waitForApiResponse()`
- Use `waitForLoadState('networkidle')` for pages with many API calls

## Configuration

Test configuration is in `playwright.config.ts`:

- **Base URL**: `http://localhost:5173` (Vite dev server)
- **API Base URL**: `http://localhost:8080/api` (staging backend)
- **Browser**: Chromium (can be extended to Firefox/WebKit)
- **Timeouts**: 30s test timeout, 5s expect timeout

To override defaults, set environment variables:
```bash
PLAYWRIGHT_BASE_URL=http://localhost:3000 npm run test:e2e
API_BASE_URL=http://localhost:9000/api npm run test:e2e
```
