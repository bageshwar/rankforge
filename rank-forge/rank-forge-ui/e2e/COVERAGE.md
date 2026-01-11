# E2E Test Coverage Guide

## Understanding E2E Test Coverage

E2E (End-to-End) test coverage is **different** from unit test code coverage. Instead of measuring lines of code executed, E2E coverage focuses on:

1. **Feature Coverage** - Which features/user stories are tested
2. **Page/Route Coverage** - Which pages/routes are tested
3. **User Flow Coverage** - Which user journeys/workflows are tested
4. **Integration Coverage** - How components work together

## Current Test Coverage

### Pages Covered ✅

- ✅ **Home Page** (`/`)
  - Rankings link visibility and navigation
  - Games link visibility and navigation

- ✅ **Games Page** (`/games`)
  - Display of 2+ games
  - Game table structure
  - Filter controls (All Games, Recent 10, Recent 25)
  - Navigation to game details

- ✅ **Game Details Page** (`/games/:gameId`)
  - Game header (map, score, date, duration, rounds)
  - Round timeline with clickable badges
  - Player statistics table
  - Accolades section
  - Navigation to round details

- ✅ **Round Details Page** (`/games/:gameId/rounds/:roundNumber`)
  - Round header and winner team
  - Round statistics
  - Bomb status indicators
  - Event timeline
  - Kill feed
  - Navigation back to game

- ✅ **Player Profile Page** (`/players/:playerId`)
  - Profile header (name, Steam ID, rank, K/D)
  - Stats grid
  - Rating progression chart
  - Accolades overview
  - Recent accolades list
  - Navigation back to rankings

- ✅ **Rankings Page** (`/rankings`)
  - Rankings table structure
  - Filter controls (All Players, Top 10, Top 25)
  - Rank icons for top 3
  - Player links navigation

### User Flows Covered ✅

- ✅ Home → Rankings navigation
- ✅ Home → Games navigation
- ✅ Games → Game Details navigation
- ✅ Game Details → Round Details navigation
- ✅ Rankings → Player Profile navigation
- ✅ Game Details → Player Profile (from stats table)
- ✅ Game Details → Player Profile (from accolades)
- ✅ Round Details → Player Profile (from event timeline)
- ✅ Round Details → Player Profile (from kill feed)

### Features Covered ✅

- ✅ Navigation between all major pages
- ✅ Data display validation
- ✅ Interactive elements (links, buttons, filters)
- ✅ API integration (waiting for API calls)
- ✅ Dynamic content rendering

## Coverage Metrics

### Manual Coverage Tracking

You can track coverage manually by:

1. **Listing all tests:**
   ```bash
   npx playwright test --list
   ```

2. **Running with detailed output:**
   ```bash
   npx playwright test --reporter=list
   ```

3. **Generating HTML report:**
   ```bash
   npx playwright test
   # Then open playwright-report/index.html
   ```

### Coverage Checklist

Use this checklist to ensure comprehensive coverage:

#### Pages
- [x] Home page
- [x] Games page
- [x] Game details page
- [x] Round details page
- [x] Player profile page
- [x] Rankings page

#### Critical User Flows
- [x] Browse games → View game details
- [x] View game → View round details
- [x] View rankings → View player profile
- [x] View game → View player profile (multiple entry points)
- [x] View round → View player profile

#### Interactive Elements
- [x] Navigation links
- [x] Filter buttons
- [x] Player profile links (all locations)
- [x] Round badge clicks
- [x] Back navigation buttons

## Code Coverage for E2E Tests

While E2E tests don't typically measure code coverage the same way unit tests do, you can use tools to track which parts of your application are exercised:

### Option 1: Istanbul/NYC with Playwright

You can instrument your code and collect coverage:

```bash
# Install coverage tools
npm install --save-dev @istanbuljs/nyc-config-typescript source-map-support

# Run tests with coverage
npx nyc --reporter=html --reporter=text playwright test
```

However, this requires:
- Code instrumentation (modifying build process)
- Source maps enabled
- Can be complex for React/Vite apps

### Option 2: Manual Coverage Matrix

Create a coverage matrix document tracking:
- Which components are tested
- Which API endpoints are called
- Which user flows are covered

### Option 3: Feature Coverage Reports

Use Playwright's built-in reporting to track:
- Test execution results
- Which tests pass/fail
- Test duration and flakiness

## Improving Coverage

### Add More Tests For:

1. **Error States**
   - 404 pages
   - API errors
   - Empty states (no games, no players)

2. **Edge Cases**
   - Games with no rounds
   - Players with no accolades
   - Empty search results

3. **Accessibility**
   - Keyboard navigation
   - Screen reader compatibility
   - ARIA attributes

4. **Performance**
   - Page load times
   - API response times
   - Large data sets

5. **Cross-Browser**
   - Firefox
   - Safari/WebKit
   - Mobile viewports

## Coverage Reports

### HTML Report

After running tests, view the HTML report:

```bash
npx playwright show-report
```

This shows:
- Test results
- Execution time
- Screenshots/videos on failure
- Test grouping by file

### JSON Report

Generate JSON report for CI/CD:

```bash
npx playwright test --reporter=json
```

## Best Practices

1. **Focus on User Flows** - Test what users actually do
2. **Test Critical Paths** - Prioritize important features
3. **Maintain Test Quality** - Keep tests fast and reliable
4. **Document Coverage** - Keep this document updated
5. **Review Regularly** - Ensure coverage stays comprehensive

## Coverage Goals

Aim for:
- ✅ 100% of critical user flows
- ✅ 100% of major pages/routes
- ✅ 80%+ of interactive elements
- ✅ All navigation paths tested
- ✅ Error states covered

Remember: **Quality over quantity**. It's better to have fewer, well-written tests that cover critical paths than many tests that are flaky or test trivial things.
