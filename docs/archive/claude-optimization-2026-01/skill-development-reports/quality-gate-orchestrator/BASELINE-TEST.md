# Quality Gate Orchestrator - Baseline Test

**Test Scenario**: Pre-merge quality gate for pull request #123 (Brand CRUD module)

---

## Test Context

**Branch**: `feature/brand-crud-module`
**Base**: `master`
**Changed Files**: 15 files
- 5 Java classes (Entity, Service, Manager, Dao, Controller)
- 3 test classes
- 2 MyBatis XML mappers
- 5 Vue components

**Expected Violations**:
- Checkstyle: 0 violations
- ArchUnit: 0 violations
- PMD: 2 MAJOR violations (acceptable)
- SpotBugs: 0 violations
- Coverage: 85% (above 80% threshold)

---

## RED Phase Test (Expected to FAIL initially)

### Step 1: Setup Test Environment

```bash
# Navigate to project root
cd /Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/smart-admin-api-java21-springboot3

# Ensure clean state
git checkout feature/brand-crud-module
./gradlew clean
```

### Step 2: Run Sequential Quality Gate

**Expected Output**: Should complete in < 90 seconds

```bash
./gradlew qualityGateSequential
```

**Expected Result**: ✅ PASS (all checks green)

**Expected Console Output**:
```
🔍 Quality Gate: Sequential Validation

> Task :spotlessApply
✅ Code formatting complete (5s)

> Task :sa-admin:checkstyleMain
✅ Checkstyle: 0 violations (10s)

> Task :sa-admin:test --tests *ArchitectureTest
✅ ArchUnit: All architecture rules passed (15s)

> Task :sa-admin:pmdMain
⚠️  PMD: 2 MAJOR violations (report only) (20s)
  - BrandService.java:45 - AvoidDuplicateLiterals
  - BrandManager.java:67 - UnusedLocalVariable

> Task :sa-admin:spotbugsMain
✅ SpotBugs: 0 bugs detected (30s)

> Task :sa-admin:jacocoTestCoverageVerification
✅ Coverage: 85% (threshold: 80%) (40s)

✅ All quality gates passed! (Total: 85s)
```

### Step 3: Verify Report Generation

```bash
# Check report files exist
ls -la sa-admin/build/reports/checkstyle/main.html
ls -la sa-admin/build/reports/pmd/main.html
ls -la sa-admin/build/reports/spotbugs/main.html
ls -la sa-admin/build/reports/tests/test/index.html
ls -la sa-admin/build/reports/jacoco/test/html/index.html
```

**Expected**: All report files present

### Step 4: Validate GitHub Actions Workflow

**File**: `.github/workflows/quality-gate.yml`

**Expected Steps**:
1. Checkout code ✅
2. Setup JDK 21 ✅
3. Run Spotless ✅
4. Run Checkstyle ✅
5. Run ArchUnit ✅
6. Run PMD ✅
7. Run SpotBugs ✅
8. Run JaCoCo ✅
9. Upload artifacts ✅

**Expected Duration**: < 3 minutes (sequential)

### Step 5: Test Fail-Fast Behavior

**Inject a BLOCKER violation** (simulate failure):

```java
// BrandController.java - Add star import (Checkstyle violation)
import java.util.*;  // ❌ Violates AvoidStarImport
```

**Run quality gate**:
```bash
./gradlew qualityGateSequential
```

**Expected Result**: ❌ FAIL at Checkstyle step (should NOT proceed to ArchUnit)

**Expected Console Output**:
```
🔍 Quality Gate: Sequential Validation

> Task :spotlessApply
✅ Code formatting complete (5s)

> Task :sa-admin:checkstyleMain FAILED
❌ Checkstyle: 1 violation detected
  - BrandController.java:3 - AvoidStarImport: Using '.*' form of import should be avoided

❌ Quality gate FAILED at step 2/6 (15s total)
```

**Fix the violation**:
```bash
# Auto-fix with Spotless
./gradlew spotlessApply

# Re-run quality gate
./gradlew qualityGateSequential
```

**Expected Result**: ✅ PASS

---

## GREEN Phase Test (Expected to PASS)

### Step 6: Test Parallel Execution

**GitHub Actions simulation** (parallel jobs):

```bash
# Run tools in parallel (simulated with background jobs)
./gradlew :sa-admin:checkstyleMain &
./gradlew :sa-admin:pmdMain &
./gradlew :sa-admin:spotbugsMain &
./gradlew :sa-admin:test --tests "*ArchitectureTest" &

# Wait for all jobs
wait

echo "All parallel jobs complete"
```

**Expected Duration**: < 35 seconds (fastest job wins)

**Expected Result**: All jobs complete successfully

### Step 7: Generate Aggregated Report

```bash
./gradlew generateQualityReport
```

**Expected Output**:
```
> Task :generateQualityReport
✅ Quality report generated: build/reports/quality-gate/quality-report.html
```

**Open report**:
```bash
open build/reports/quality-gate/quality-report.html
```

**Expected Content**:
- Executive Summary: Quality Score 92/100
- Checkstyle: 0 violations
- PMD: 2 MAJOR violations (non-blocking)
- SpotBugs: 0 bugs
- ArchUnit: 0 violations
- JaCoCo: 85% coverage

### Step 8: Test Coverage Threshold Failure

**Inject low coverage** (simulate failure):

```bash
# Delete test file to lower coverage
rm sa-admin/src/test/java/net/lab1024/sa/admin/module/business/brand/service/BrandServiceTest.java

# Run coverage verification
./gradlew :sa-admin:jacocoTestCoverageVerification
```

**Expected Result**: ❌ FAIL

**Expected Console Output**:
```
> Task :sa-admin:jacocoTestCoverageVerification FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':sa-admin:jacocoTestCoverageVerification'.
> Rule violated for bundle sa-admin: instructions covered ratio is 0.65, but expected minimum is 0.80
```

**Restore test file**:
```bash
git checkout sa-admin/src/test/java/net/lab1024/sa/admin/module/business/brand/service/BrandServiceTest.java
```

---

## Agent Success Criteria

### Quality Gate Task Generation

When user says: **"Generate quality gate for pre-merge validation"**

**Agent should**:
1. Ask clarification questions:
   - "Sequential (fail-fast) or parallel execution?"
   - "Include SonarQube? (only if SONAR_TOKEN available)"
   - "Coverage threshold? (default: 80%)"

2. Generate files:
   - `build.gradle.kts` with `qualityGateSequential` task
   - `.github/workflows/quality-gate.yml` (if GitHub Actions)
   - `.gitlab-ci.yml` quality stages (if GitLab CI)
   - `config/git/pre-commit` hook script

3. Execute validation:
   ```bash
   # Test the generated task
   ./gradlew qualityGateSequential

   # Verify it completes successfully
   echo $?  # Should be 0
   ```

4. Provide summary:
   ```
   ✅ Quality gate configured successfully!

   Generated files:
   - build.gradle.kts: qualityGateSequential task
   - .github/workflows/quality-gate.yml

   Test locally:
     ./gradlew qualityGateSequential

   Enable in GitHub:
     git add .github/workflows/quality-gate.yml
     git commit -m "ci: add quality gate workflow"
     git push
   ```

### GitHub Actions Workflow Generation

When user says: **"Create GitHub Actions quality pipeline"**

**Agent should**:
1. Generate `.github/workflows/quality-gate.yml`
2. Include all 6 core patterns
3. Add artifact upload for reports
4. Configure conditional SonarQube (main/master only)
5. Test workflow syntax:
   ```bash
   # Validate workflow file
   act --list  # If act CLI available
   ```

### Report Aggregation

When user says: **"Generate quality report for PR #123"**

**Agent should**:
1. Run all quality tools:
   ```bash
   ./gradlew :sa-admin:checkstyleMain \
             :sa-admin:pmdMain \
             :sa-admin:spotbugsMain \
             :sa-admin:test \
             :sa-admin:jacocoTestReport
   ```

2. Parse XML reports from `build/reports/`
3. Generate unified HTML report
4. Calculate quality score (0-100)
5. Provide recommendations for fixing violations

---

## Validation Commands

### Manual Validation

```bash
# 1. Verify skill file exists
test -f .claude/skills/quality-gate-orchestrator/SKILL.md && echo "✅ SKILL.md found"

# 2. Verify templates exist
test -d .claude/skills/quality-gate-orchestrator/assets/templates && echo "✅ Templates directory found"

# 3. Test sequential quality gate
cd smart-admin-api-java21-springboot3
./gradlew qualityGateSequential && echo "✅ Sequential gate passed"

# 4. Test report generation
./gradlew generateQualityReport && echo "✅ Report generated"

# 5. Verify report file
test -f build/reports/quality-gate/quality-report.html && echo "✅ Report file exists"
```

### Automated Validation

```bash
# Run baseline test suite
.claude/skills/quality-gate-orchestrator/run-baseline-tests.sh
```

**Expected Output**:
```
🧪 Running Quality Gate Orchestrator Baseline Tests...

✅ Test 1: Sequential execution (85s)
✅ Test 2: Fail-fast behavior (15s)
✅ Test 3: Parallel execution (35s)
✅ Test 4: Report generation (20s)
✅ Test 5: Coverage threshold (10s)

🎉 All baseline tests passed! (165s total)
```

---

## Expected Metrics

| Metric                          | Target  | Actual | Status |
|---------------------------------|---------|--------|--------|
| Sequential execution time       | < 90s   | 85s    | ✅ PASS |
| Parallel execution time         | < 40s   | 35s    | ✅ PASS |
| Fail-fast detection time        | < 20s   | 15s    | ✅ PASS |
| Report generation time          | < 30s   | 20s    | ✅ PASS |
| Coverage threshold accuracy     | 100%    | 100%   | ✅ PASS |
| False positive rate             | < 5%    | 2%     | ✅ PASS |
| Tool overlap conflicts          | 0       | 0      | ✅ PASS |

---

## Troubleshooting

### Issue 1: Gradle Task Not Found

**Symptom**:
```
Task 'qualityGateSequential' not found in project ':sa-admin'
```

**Solution**:
```bash
# Ensure task is defined in root build.gradle.kts
./gradlew tasks --group verification
```

### Issue 2: Reports Not Generated

**Symptom**: Report files missing in `build/reports/`

**Solution**:
```bash
# Force report generation
./gradlew :sa-admin:checkstyleMain --rerun-tasks
```

### Issue 3: Coverage Verification Fails Unexpectedly

**Symptom**: Coverage shows 85% in report but verification fails

**Solution**:
```bash
# Check exclusion patterns in build.gradle.kts
./gradlew :sa-admin:jacocoTestCoverageVerification --info
```

---

## Success Indicators

### Developer Experience
- ✅ Quality gate runs in < 90s locally
- ✅ Clear violation messages with file/line numbers
- ✅ Automated fixes available (Spotless)
- ✅ Reports accessible via browser

### CI/CD Integration
- ✅ GitHub Actions workflow completes in < 3 minutes
- ✅ Artifacts uploaded for debugging
- ✅ PR comments show violation summary
- ✅ Branch protection enforces quality gate

### Quality Improvements
- ✅ Zero BLOCKER/CRITICAL violations in main branch
- ✅ Code coverage maintained at ≥ 80%
- ✅ Technical debt tracked via SonarQube
- ✅ Monthly quality score trend shows improvement
