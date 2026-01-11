# Quick Start: Running All E2E Tests

## Run All Tests

### Basic Command
```bash
cd rank-forge/rank-forge-ui
npm run test:e2e
```

This will:
- Start the Vite dev server automatically (if not running)
- Run all test files in the `e2e/` directory
- Generate an HTML report

### Run All Tests with Options

**With UI mode (interactive):**
```bash
npm run test:e2e:ui
```

**In headed mode (see browser):**
```bash
npx playwright test --headed
```

**With verbose output:**
```bash
npx playwright test --reporter=list
```

**List all tests without running:**
```bash
npx playwright test --list
```

## Test Files

You have **7 test files** with multiple tests each:

1. `e2e/home.spec.ts` - 3 tests
2. `e2e/games.spec.ts` - 5 tests
3. `e2e/game-details.spec.ts` - 10 tests
4. `e2e/round-details.spec.ts` - 8 tests
5. `e2e/player-profile.spec.ts` - 8 tests
6. `e2e/navigation.spec.ts` - 5 tests
7. `e2e/rankings.spec.ts` - 6 tests

**Total: ~45 tests** covering all pages and user flows.

## View Test Results

After running tests, view the HTML report:

```bash
npx playwright show-report
```

This opens a browser with:
- Test results summary
- Pass/fail status
- Execution time
- Screenshots/videos on failure
- Test grouping

## Check Coverage

### 1. List All Tests
```bash
npx playwright test --list
```

### 2. View HTML Report
```bash
npx playwright show-report
```

### 3. Check Coverage Document
See `e2e/COVERAGE.md` for detailed coverage information.

### 4. Run Specific Test Suites
```bash
# Run only home page tests
npx playwright test e2e/home.spec.ts

# Run only games tests
npx playwright test e2e/games.spec.ts

# Run multiple files
npx playwright test e2e/home.spec.ts e2e/games.spec.ts
```

## Prerequisites

Before running tests, ensure:

1. **Backend is running** on `http://localhost:8080`
2. **Dependencies installed:**
   ```bash
   npm install
   npx playwright install chromium
   ```

## Troubleshooting

**If tests fail:**
- Check backend is running: `curl http://localhost:8080/api/rankings/health`
- Check frontend is accessible: Open `http://localhost:5173` in browser
- View test report: `npx playwright show-report`

**If only some tests run:**
- Check for `test.only()` in test files (shouldn't be in production)
- Check test filters: `npx playwright test --list` to see all tests
