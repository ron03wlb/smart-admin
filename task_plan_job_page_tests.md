# Task Plan: 修復 Job 模塊 Page Tests

**Created**: 2026-03-25 17:00
**Goal**: 修復 Job 模塊的 7 個失敗的頁面測試，提升測試通過率從 80.6% → 100%
**Priority**: P2 (Non-blocking, but improves CI/CD coverage)
**Estimated Time**: 2-3 hours

---

## Background Context

**Current Status**:
- Job Module Component Tests: ✅ 100% (29/29 passed)
- Job Module Page Tests: ❌ 7/10 failing (70% pass rate)
- Overall Job Module: 80.6% (29/36 tests passing)

**Root Causes Identified**:
1. Select/DictSelect placeholder not rendered as expected
2. Modal/Drawer opening tests timing out (15s timeout not enough)
3. Page-level integration tests more sensitive to component interactions

---

## Failed Tests Analysis

### Category A: Select/DictSelect Issues (3 tests)
| Test | Error | Root Cause |
|------|-------|------------|
| should filter by trigger type | Unable to find placeholder "請選擇觸發類型" | DictSelect placeholder attribute |
| should filter by enabled status | Unable to find placeholder "請選擇狀態" | Select placeholder attribute |
| should switch to deleted tab | Similar placeholder issues | Tab switching + filter interaction |

### Category B: Modal/Drawer Timeout (3 tests)
| Test | Error | Root Cause |
|------|-------|------------|
| should open JobFormModal when add button is clicked | Timeout waiting for "新增任務" | Modal rendering delay |
| should open JobFormModal with job data when edit button is clicked | Timeout waiting for "編輯任務" | Modal + data loading delay |
| should open JobLogDrawer when view log button is clicked | Timeout waiting for Drawer | Drawer rendering delay |

### Category C: API Parameter Mismatch (1 test)
| Test | Error | Root Cause |
|------|-------|------------|
| should trigger search when keyword is entered | API call parameter mismatch | Search debounce or parameter structure |

---

## Implementation Phases

### Phase 1: Investigation & Diagnosis [✅ COMPLETE]
**Goal**: Understand the exact rendering behavior of Select/DictSelect and Modal/Drawer components

**Actions Completed**:
1. ✅ Read JobManagement page component (`src/views/support/job/index.tsx`)
2. ✅ Read DictSelect implementation (`src/components/common/DictSelect/index.tsx`)
3. ✅ Run single failing test locally with verbose output
4. ✅ Identified root cause: Async rendering delay in Tabs → TabPane → Form → Select chain

**Success Criteria Met**:
- ✅ Understand why `getByPlaceholderText` fails: Test queries before Select mounts
- ✅ Know the actual DOM structure: Native Ant Design Select (not DictSelect)
- ✅ Identified correct query strategy: Add `waitFor()` before querying Select elements

**Root Cause**: Multi-level async rendering. Test waits for table data but not for Select components to mount.

### Phase 2: Fix Select/DictSelect Tests [🔄 IN PROGRESS]
**Goal**: Fix 3 tests related to Select/DictSelect placeholder issues

**Strategy**: Add `waitFor()` before querying Select components

**Actions**:
1. Fix "should filter by trigger type" test (~Line 261)
2. Fix "should filter by enabled status" test (~Line 300)
3. Fix "should switch to deleted tab" test (~Line 180)
4. Test each fix individually

**Code Pattern to Apply**:
```typescript
// After waiting for table data
await waitFor(() => {
  expect(screen.getAllByPlaceholderText('<placeholder>').length).toBeGreaterThan(0);
}, { timeout: TEST_TIMEOUT });

const selectElements = screen.getAllByPlaceholderText('<placeholder>');
```

**Files to Modify**:
- `src/views/support/job/index.test.tsx` (lines 180-350 approximately)

**Success Criteria**:
- Tests pass locally with `npm run test -- --run src/views/support/job/index.test.tsx`
- No new timeouts introduced

### Phase 3: Fix Modal/Drawer Timeout Tests [pending]
**Goal**: Fix 3 tests related to Modal/Drawer opening timeouts

**Options**:
- **Option A**: Increase timeout to 30s (quick fix)
- **Option B**: Add explicit wait for Modal visibility state
- **Option C**: Mock Modal.open to avoid actual rendering delay

**Recommended**: Option B (more robust)

**Actions**:
1. Add `waitFor` with visibility check for Modal container
2. Use `screen.getByRole('dialog')` to detect Modal presence
3. Increase timeout if needed (20s instead of 15s)
4. Test with individual runs

**Files to Modify**:
- `src/views/support/job/index.test.tsx` (lines 369-450 approximately)

**Success Criteria**:
- Tests pass locally without excessive timeout
- Modal/Drawer interactions properly detected

### Phase 4: Fix API Parameter Mismatch [pending]
**Goal**: Fix "should trigger search when keyword is entered" test

**Actions**:
1. Inspect search input implementation
2. Check if debounce is applied
3. Verify actual API call parameters
4. Update test expectations or mock accordingly

**Files to Modify**:
- `src/views/support/job/index.test.tsx` (lines ~250)

**Success Criteria**:
- Test properly waits for debounced search
- API call parameters match expected structure

### Phase 5: Verification & CI/CD [pending]
**Goal**: Ensure all fixes work in CI/CD environment

**Actions**:
1. Run all Job module tests locally: `npm run test -- --run src/views/support/job`
2. Verify 100% pass rate (36/36 or close, accounting for skipped)
3. Commit changes with clear message
4. Trigger CI/CD workflow manually
5. Verify CI/CD passes

**Success Criteria**:
- Local: Job module 100% pass rate
- CI/CD: test-job-module job passes
- No new failures in other modules

---

## Risks & Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Fixes break component tests | Low | High | Run full Job module tests after each change |
| Timeout fixes too fragile | Medium | Medium | Use visibility checks instead of time-based waits |
| CI/CD still times out | Medium | Medium | Increase TEST_TIMEOUT to 20000ms |
| Changes affect other modules | Low | High | Run full Support module tests before commit |

---

## Errors Encountered

| Error | Attempt | Resolution | Timestamp |
|-------|---------|------------|-----------|
| - | - | - | - |

*(To be filled during implementation)*

---

## Files to Modify

| File | Purpose | Lines |
|------|---------|-------|
| `src/views/support/job/index.test.tsx` | Fix all 7 failing tests | 200-450 |
| `src/views/support/job/index.tsx` | (Reference only, understand component) | - |
| `src/components/common/DictSelect/index.tsx` | (Reference only, understand rendering) | - |

---

## Testing Strategy

**Local Testing**:
```bash
# Test single failing test
npm run test -- --run src/views/support/job/index.test.tsx -t "should filter by trigger type"

# Test all Job page tests
npm run test -- --run src/views/support/job/index.test.tsx

# Test all Job module
npm run test -- --run src/views/support/job
```

**CI/CD Testing**:
- Manual trigger on commit: `git push` → GitHub Actions

---

## Success Metrics

| Metric | Before | Target | Actual |
|--------|--------|--------|--------|
| Job Page Tests Pass Rate | 30% (3/10) | 100% (10/10 or 9/10) | - |
| Job Module Pass Rate | 80.6% (29/36) | 100% (36/36) | - |
| Support Module Pass Rate | 94.1% (143/152) | 96-97% | - |
| CI/CD test-job-module | ❌ Failing | ✅ Passing | - |

---

## Next Actions

1. **Start Phase 1**: Investigation & Diagnosis
2. Read JobManagement component
3. Read DictSelect component
4. Run failing test locally with verbose output

---

**Status**: Ready to start
**Last Updated**: 2026-03-25 17:00
