# PR #17 Review: Monthly Leaderboard Implementation

## Summary
This PR implements monthly leaderboards with accurate rounds calculation using ROUND_END events. Overall, the implementation is solid, but there are several areas for improvement.

---

## 🔴 Critical Issues (Should Fix)

### 1. **Performance: N+1 Query Problem in Monthly Leaderboard**
**Location:** `PlayerRankingService.getMonthlyPlayerRankingsWithStats()` lines 400-406

**Issue:** For each player in the paginated results, a separate database query is made to count games:
```java
for (PlayerStats stats : paginatedStats) {
    long gamesPlayedInMonth = playerStatsRepository.countDistinctGamesByPlayerIdInMonth(
            playerId, startInstant, endInstant);
    // ...
}
```

**Impact:** If returning 100 players, this makes 100+ additional database queries.

**Suggestion:** 
- Batch query all games counts in one query using `GROUP BY`
- Add repository method: `Map<String, Long> countDistinctGamesByPlayerIdsInMonth(List<String> playerIds, Instant start, Instant end)`
- Use the map to lookup counts instead of querying per player

**Priority:** High - Will significantly improve performance for large leaderboards

---

### 2. **Memory Efficiency: Loading All ROUND_END Events**
**Location:** `PlayerRankingService.getMonthlyPlayerRankingsWithStats()` line 262

**Issue:** All ROUND_END events for all games in the month are loaded into memory at once. For months with many games, this could be memory-intensive.

**Suggestion:**
- Consider processing in batches if the list is very large (>10,000 events)
- Or add a database-level aggregation query to count rounds per player directly in SQL
- Could add: `@Query("SELECT e.game.id, COUNT(DISTINCT e.id) FROM RoundEndEventEntity e WHERE e.game.id IN :gameIds GROUP BY e.game.id")`

**Priority:** Medium - May not be an issue now but could become one with scale

---

### 3. **Error Handling: Silent Failures in JSON Parsing**
**Location:** `PlayerRankingService.countRoundsPerPlayerFromRoundEndEvents()` lines 491-503

**Issue:** If JSON parsing fails, it silently falls back to CSV parsing. If CSV also fails, it only logs a warning but continues. This could lead to missing rounds data.

**Suggestion:**
- Add metrics/alerting for parsing failures
- Consider storing parsing failures in a separate table for investigation
- At minimum, increment a counter and log summary at end: "Failed to parse X out of Y ROUND_END events"

**Priority:** Medium - Data accuracy is important

---

## 🟡 Important Improvements (Should Consider)

### 4. **Code Duplication: Ranking Algorithm Instantiation**
**Location:** `PlayerRankingService.getMonthlyPlayerRankingsWithStats()` line 282

**Issue:** `EloBasedRankingAlgorithm` is instantiated on every request. This is fine, but could be a singleton bean.

**Suggestion:**
- Make `EloBasedRankingAlgorithm` a Spring `@Component` and inject it
- Or at least make it a class-level constant if it's stateless

**Priority:** Low - Minor optimization

---

### 5. **API Design: Inconsistent Response Types**
**Location:** `PlayerRankingApiController` lines 52-56 vs 62-66

**Issue:** `/api/rankings` returns `List<PlayerRankingDTO>` but `/api/rankings/stats` returns `LeaderboardResponseDTO`. This inconsistency could confuse API consumers.

**Suggestion:**
- Consider making `/api/rankings` also return `LeaderboardResponseDTO` for consistency
- Or document clearly that one endpoint includes stats and the other doesn't

**Priority:** Low - API design preference

---

### 6. **Validation: Missing Input Validation**
**Location:** `PlayerRankingApiController.getMonthlyLeaderboard()` lines 138-156

**Issue:** Validation is basic. No validation for:
- Negative month/year values
- Future dates beyond reasonable range
- Very large offset values that could cause performance issues

**Suggestion:**
- Add `@Valid` annotations and validation constraints
- Consider adding a maximum offset limit (e.g., 10,000)
- Validate that requested month/year is not in the future

**Priority:** Low - Edge case handling

---

### 7. **Frontend: Missing Error Boundaries**
**Location:** `RankingsPage.tsx` line 127-132

**Issue:** Error handling is basic - just shows a generic error message. No retry mechanism or detailed error info.

**Suggestion:**
- Add retry button for failed requests
- Show more specific error messages (network error vs server error)
- Consider adding error boundaries for React error handling

**Priority:** Low - UX improvement

---

### 8. **Database Query: Potential Index Missing**
**Location:** `GameEventRepository.findRoundEndEventsByGameIds()` line 94

**Issue:** Query uses `IN :gameIds` which could be slow for large lists without proper indexing.

**Suggestion:**
- Verify indexes exist on `GameEvent.game.id` and `GameEvent.gameEventType`
- Consider adding composite index if queries are slow
- Document expected query performance

**Priority:** Medium - Performance optimization

---

## 🟢 Nice-to-Have Improvements

### 9. **Code Organization: Long Method**
**Location:** `PlayerRankingService.getMonthlyPlayerRankingsWithStats()` - 180+ lines

**Issue:** The method is quite long and does multiple things. Could be broken down.

**Suggestion:**
- Extract `computeMonthlyStatsForPlayer()` method
- Extract `buildMonthlyLeaderboardDTOs()` method
- Makes code more testable and readable

**Priority:** Low - Code quality

---

### 10. **Logging: Too Verbose in Production**
**Location:** Multiple `LOGGER.info()` calls throughout

**Issue:** Many info-level logs that might be too verbose for production.

**Suggestion:**
- Change some `LOGGER.info()` to `LOGGER.debug()`
- Keep only critical info logs (e.g., "Querying monthly leaderboard for X-Y")
- Consider using structured logging with context

**Priority:** Low - Logging hygiene

---

### 11. **Testing: Missing Integration Tests**
**Location:** Only unit tests exist

**Issue:** No integration tests that verify the full flow from API to database.

**Suggestion:**
- Add integration test that:
  - Creates test games and ROUND_END events
  - Calls the API endpoint
  - Verifies correct rounds counting
- Tests the ID normalization logic end-to-end

**Priority:** Medium - Test coverage

---

### 12. **Documentation: Missing API Documentation**
**Location:** Controller methods

**Issue:** No OpenAPI/Swagger annotations for API documentation.

**Suggestion:**
- Add `@Operation` and `@ApiResponse` annotations
- Document request/response schemas
- Add example values

**Priority:** Low - Developer experience

---

### 13. **Frontend: Missing Loading States**
**Location:** `RankingsPage.tsx`

**Issue:** While loading spinner exists, individual operations (like changing month) don't show loading state clearly.

**Suggestion:**
- Add skeleton loaders for table rows
- Show loading indicator when changing filters
- Disable controls during loading

**Priority:** Low - UX polish

---

### 14. **Data Consistency: Race Condition Potential**
**Location:** `PlayerRankingService.getMonthlyPlayerRankingsWithStats()`

**Issue:** If games are being processed while leaderboard is queried, there could be inconsistencies.

**Suggestion:**
- Document expected behavior
- Consider using read-only transactions
- Add version/timestamp checks if needed

**Priority:** Low - Edge case

---

### 15. **Caching: No Caching Strategy**
**Location:** All service methods

**Issue:** Monthly leaderboards are computed on every request. For past months, this could be cached.

**Suggestion:**
- Add `@Cacheable` for past months (not current month)
- Cache key: `"monthly-leaderboard-{year}-{month}"`
- TTL: Indefinite for past months, short TTL for current month

**Priority:** Medium - Performance optimization

---

## 📋 Summary by Priority

### Must Fix (Before Merge):
1. **N+1 Query Problem** - Performance critical
2. **Error Handling for JSON Parsing** - Data accuracy

### Should Fix (Soon):
3. **Memory Efficiency for ROUND_END Events** - Scalability
4. **Database Index Verification** - Performance
5. **Integration Tests** - Test coverage

### Nice to Have:
6. Code organization improvements
7. Logging optimization
8. API documentation
9. Frontend UX improvements
10. Caching strategy

---

## ✅ What's Good

1. **Accurate Rounds Calculation** - Using ROUND_END events is the right approach
2. **ID Normalization** - Good handling of format mismatches
3. **Comprehensive Unit Tests** - Good test coverage for edge cases
4. **URL-based Navigation** - Good UX with shareable URLs
5. **Error Handling** - Basic error handling is present
6. **Code Comments** - Good documentation of complex logic

---

## 🎯 Recommended Action Plan

1. **Immediate (Before Merge):**
   - Fix N+1 query problem (#1)
   - Improve error handling for JSON parsing (#3)

2. **Short Term (Next Sprint):**
   - Add integration tests (#11)
   - Verify/optimize database indexes (#8)
   - Consider caching for past months (#15)

3. **Long Term (Backlog):**
   - Code refactoring for maintainability (#9)
   - API documentation (#12)
   - Frontend UX improvements (#13)

---

## Questions for Discussion

1. **Performance Requirements:** What's the expected number of players/games per month? This affects priority of performance optimizations.

2. **Caching Strategy:** Should past months be cached? How often do they change?

3. **Error Handling:** How should we handle partial failures (e.g., some ROUND_END events fail to parse)?

4. **API Versioning:** Should we version the API endpoints for future changes?
