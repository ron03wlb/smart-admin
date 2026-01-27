# 12-02 測試驗收標準 (QA Testing Standard)

## 1. 系統概述
金融類系統對錯誤的容忍度為零。本標準定義了各階段測試的 **"通過條件 (Exit Criteria)"**。
核心理念：**"Shift Left Testing"** (測試左移，儘早發現 Bug)。

## 2. 測試金字塔 (Testing Pyramid)

### 2.1 單元測試 (Unit Test)
*   **範圍**: 
    - 佣金計算公式 (Commission Formula)。
    - 錢包扣款邏輯 (Wallet Deduction Priority)。
    - 流水檢查邏輯 (Wagering Check)。
*   **工具**: JUnit (Java), Jest (Node.js), Go Test。
*   **標準**: 
    - 核心模組 (Finance/Risk) 代碼覆蓋率 > **90%**。
    - 必須包含邊界值測試 (如：餘額剛好為 0，流水差 $0.01)。

### 2.2 整合測試 (Integration Test)
*   **範圍**: 
    - API 接口 (Controller -> Service -> DB)。
    - 第三方回調 (Game Callback -> Wallet Update)。
*   **工具**: Postman / Newman, RestAssured。
*   **標準**: 
    - 所有 API 必須有對應的自動化測試腳本。
    - 模擬並發場景 (Race Condition Test) 必須通過。

### 2.3 端對端測試 (E2E Test)
*   **範圍**: 
    - 用戶從 "註冊" -> "存款" -> "進入遊戲" -> "提款" 的完整流程。
*   **工具**: Cypress / Playwright。
*   **標準**: 
    - 每日凌晨自動運行一次 Regression Test，確保舊功能未損壞。

---

## 3. 性能測試標準 (Performance Testing)

### 3.1 效能測試矩陣 (Performance Test Matrix)

#### 3.1.1 測試場景與目標

| Scenario | Description | Concurrent Users | Duration | Target P99 Latency | Target Error Rate | Business Context |
|---|---|---|---|---|---|---|
| **Login Storm** | 大量用戶同時登入 | 50K users/min | 5 min | < 500ms | 0% | 重大賽事開踢前 |
| **Bet Spike** | 投注高峰 | 100K bets/min | 10 min | < 200ms | 0% (no lost bets) | 足球賽事開始瞬間 |
| **Withdrawal Burst** | 提款高峰 | 10K withdrawals/min | 5 min | Queue delay < 30s | 0% | 薪資發放日後 |
| **Game Launch Peak** | 遊戲啟動高峰 | 200K launches/min | 10 min | < 1000ms | < 0.1% | 新遊戲上線首日 |
| **24h Soak Test** | 持續負載測試 | 200K concurrent | 24 hours | No degradation | 0% | 驗證記憶體洩漏 |
| **Spike Test** | 瞬間流量暴增 | 0 → 100K in 10s | 2 min | < 800ms | < 1% | 測試自動擴展 |
| **Stress Test** | 極限壓力測試 | Ramp until failure | Until crash | Find breaking point | - | 容量規劃 |

#### 3.1.2 Scenario 1: Login Storm (登入熱點測試)

**測試目標**: 驗證認證系統在大量並發登入時的穩定性

**K6 測試腳本**:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 10000 },   // Ramp up to 10K users
    { duration: '5m', target: 50000 },   // Ramp up to 50K users
    { duration: '5m', target: 50000 },   // Stay at 50K for 5 minutes
    { duration: '2m', target: 0 },       // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(99)<500'],  // 99% of requests < 500ms
    'http_req_failed': ['rate<0.01'],    // Error rate < 1%
    'errors': ['rate<0.01'],
  },
};

const BASE_URL = 'https://api.casino.com';

export default function () {
  const username = `testuser_${__VU}_${__ITER}`;
  const password = 'TestPassword123!';

  // Step 1: Login
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    username: username,
    password: password,
    device_id: `device_${__VU}`,
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  const loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'has access token': (r) => r.json('access_token') !== undefined,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!loginSuccess);

  if (!loginSuccess) {
    console.error(`Login failed for ${username}: ${loginRes.status}`);
    return;
  }

  const token = loginRes.json('access_token');

  // Step 2: Fetch user profile (typical post-login action)
  const profileRes = http.get(`${BASE_URL}/api/v1/players/me`, {
    headers: { 'Authorization': `Bearer ${token}` },
  });

  check(profileRes, {
    'profile status is 200': (r) => r.status === 200,
  });

  sleep(1);  // Think time: 1 second between actions
}
```

**Expected Results**:
```
✓ http_req_duration.............: avg=245ms  min=98ms  med=210ms  max=1.2s  p(90)=350ms p(95)=420ms p(99)=480ms
✓ http_req_failed...............: 0.03% (150 failed out of 500,000 requests)
✓ http_reqs.....................: 500,000 (1,666/s)
✓ vus...........................: 50,000 max
```

#### 3.1.3 Scenario 2: Bet Spike (投注高峰測試)

**測試目標**: 驗證錢包系統與遊戲下注接口在高併發下的一致性

**JMeter Test Plan**:

```xml
<jmeterTestPlan version="1.2">
  <TestPlan>
    <ThreadGroup name="Bet Spike Test" enabled="true">
      <elementProp name="ThreadGroup.main_controller">
        <loopController>
          <intProp name="LoopController.loops">-1</intProp>
        </loopController>
      </elementProp>
      <stringProp name="ThreadGroup.num_threads">10000</stringProp>
      <stringProp name="ThreadGroup.ramp_time">60</stringProp>
      <stringProp name="ThreadGroup.duration">600</stringProp>
    </ThreadGroup>

    <HTTPSamplerProxy name="Place Bet">
      <stringProp name="HTTPSampler.domain">api.casino.com</stringProp>
      <stringProp name="HTTPSampler.port">443</stringProp>
      <stringProp name="HTTPSampler.protocol">https</stringProp>
      <stringProp name="HTTPSampler.path">/api/v1/bets</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
      <elementProp name="HTTPsampler.Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="">
            <stringProp name="Argument.value">{
              "game_id": "${game_id}",
              "amount": ${bet_amount},
              "selection": "${selection}"
            }</stringProp>
            <stringProp name="Argument.metadata">=</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </HTTPSamplerProxy>

    <ResultCollector name="Summary Report">
      <stringProp name="filename">./results/bet_spike_test.jtl</stringProp>
    </ResultCollector>
  </TestPlan>
</jmeterTestPlan>
```

**Pass Criteria**:
- P99 Latency: < 200ms
- Error Rate: 0% (no lost bets due to race conditions)
- Wallet Balance Consistency: 100% (all bets deducted correctly)

#### 3.1.4 Scenario 4: 24-Hour Soak Test (穩定性測試)

**測試目標**: 檢測記憶體洩漏、連接池耗盡、緩存過期等長期運行問題

**Test Configuration**:

```javascript
export const options = {
  scenarios: {
    soak_test: {
      executor: 'constant-vus',
      vus: 20000,              // 20K concurrent users
      duration: '24h',         // Run for 24 hours
    },
  },
  thresholds: {
    'http_req_duration': ['p(99)<500'],
    'http_req_failed': ['rate<0.01'],
    'iteration_duration': ['p(99)<2000'],  // Full scenario < 2s
  },
};

export default function () {
  // Simulate typical user session
  const actions = [
    () => http.get(`${BASE_URL}/api/v1/games/lobby`),
    () => http.post(`${BASE_URL}/api/v1/bets`, betPayload),
    () => http.get(`${BASE_URL}/api/v1/wallet/balance`),
    () => http.get(`${BASE_URL}/api/v1/players/me/bonuses`),
  ];

  const randomAction = actions[Math.floor(Math.random() * actions.length)];
  randomAction();

  sleep(randomBetween(5, 15));  // User think time: 5-15 seconds
}
```

**Monitoring During Test**:
- Memory Usage: Should remain stable (no upward trend)
- Database Connection Pool: No exhaustion (available connections > 10%)
- CPU Usage: < 70% sustained
- Garbage Collection: Frequency and pause times within normal range

**Expected Metrics (Start vs End of 24h)**:

| Metric | Hour 1 | Hour 24 | Delta | Status |
|---|---|---|---|---|
| P99 Latency | 210ms | 215ms | +5ms | ✅ Acceptable |
| Memory Usage (RSS) | 4.2 GB | 4.3 GB | +100 MB | ✅ No leak |
| DB Connections | 180/200 | 185/200 | +5 | ✅ Stable |
| Error Rate | 0.02% | 0.03% | +0.01% | ✅ Acceptable |

### 3.2 測試數據生成策略 (Test Data Generation)

#### 3.2.1 匿名化生產數據 (Sanitized Production Data)

**推薦方案**: 使用生產數據備份，但進行敏感資料脫敏

```sql
-- Anonymize player data for testing
UPDATE players_test SET
  email = CONCAT('testuser_', player_id, '@test.com'),
  phone = CONCAT('+8869', LPAD(player_id::TEXT, 8, '0')),
  real_name = CONCAT('Test User ', player_id),
  id_number = NULL,
  bank_account = NULL
WHERE tenant_id = 'test_tenant';

-- Preserve statistical distribution (e.g., VIP tiers, deposit amounts)
-- but remove PII
```

#### 3.2.2 合成數據生成 (Synthetic Data)

```python
# Generate realistic test data using Faker
from faker import Faker
import random

fake = Faker(['en_US', 'th_TH', 'vi_VN'])

def generate_test_players(count=100000):
    players = []
    for i in range(count):
        player = {
            'player_id': i + 1,
            'username': fake.user_name(),
            'email': fake.email(),
            'phone': fake.phone_number(),
            'vip_tier': random.choices(
                ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'],
                weights=[50, 30, 15, 4, 1]
            )[0],
            'balance': round(random.lognormvariate(5, 2), 2),  # Realistic distribution
            'created_at': fake.date_time_between(start_date='-2y', end_date='now'),
        }
        players.append(player)
    return players

# Generate test bets with realistic patterns
def generate_test_bets(player_id, count=100):
    games = ['slots', 'baccarat', 'blackjack', 'roulette', 'poker']
    bets = []
    for _ in range(count):
        bet = {
            'player_id': player_id,
            'game_type': random.choice(games),
            'amount': round(random.uniform(1, 500), 2),
            'result': random.choices(['WIN', 'LOSE'], weights=[45, 55])[0],
            'payout': 0,  # Calculated based on result
        }
        if bet['result'] == 'WIN':
            bet['payout'] = round(bet['amount'] * random.uniform(0.9, 3.0), 2)
        bets.append(bet)
    return bets
```

### 3.3 測試腳本架構 (Test Script Structure)

#### 3.3.1 模組化測試腳本

```
tests/
├── performance/
│   ├── scenarios/
│   │   ├── login_storm.js
│   │   ├── bet_spike.js
│   │   ├── withdrawal_burst.js
│   │   └── soak_test.js
│   ├── lib/
│   │   ├── auth.js            # Reusable auth functions
│   │   ├── wallet.js          # Wallet API helpers
│   │   ├── game.js            # Game API helpers
│   │   └── metrics.js         # Custom metrics
│   ├── data/
│   │   ├── test_users.csv     # Pre-generated test users
│   │   ├── test_games.json    # Game catalog
│   │   └── test_bets.json     # Bet scenarios
│   └── config/
│       ├── dev.json           # Dev environment config
│       ├── staging.json       # Staging environment config
│       └── prod-like.json     # Production-like load config
├── security/
│   ├── owasp_zap_scan.yaml   # Automated security scanning
│   └── snyk_scan.yaml         # Dependency vulnerability scan
└── regression/
    ├── api_regression.postman_collection.json
    └── e2e_regression.cy.js   # Cypress E2E tests
```

### 3.4 效能基準與回歸檢測 (Performance Baselines & Regression Detection)

#### 3.4.1 建立效能基準

**首次測試 (Baseline)**:
```bash
# Run baseline test and save results
k6 run --out json=baseline.json scenarios/login_storm.js

# Extract key metrics
k6-reporter --input baseline.json --output baseline_report.html

# Store baseline in database for comparison
python store_baseline.py --file baseline.json --version v1.0.0
```

**後續回歸測試**:
```bash
# Run test
k6 run --out json=current.json scenarios/login_storm.js

# Compare against baseline
python compare_performance.py --baseline baseline.json --current current.json

# Output: Performance Regression Report
# - P99 Latency: 210ms → 350ms (+66% REGRESSION ❌)
# - Error Rate: 0.02% → 0.05% (+150% REGRESSION ❌)
# - Throughput: 1000 RPS → 950 RPS (-5% ACCEPTABLE ✅)
```

#### 3.4.2 自動化效能門檻檢查 (Automated Performance Gates)

```yaml
# .github/workflows/performance-gate.yml
name: Performance Gate

on:
  pull_request:
    branches: [main, develop]

jobs:
  performance_test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run K6 Test
        uses: grafana/k6-action@v0.3.0
        with:
          filename: tests/performance/scenarios/api_smoke_test.js
          cloud: false

      - name: Check Performance Thresholds
        run: |
          if [ $K6_EXIT_CODE -ne 0 ]; then
            echo "❌ Performance test failed"
            exit 1
          fi

      - name: Compare Against Baseline
        run: |
          python scripts/compare_performance.py \
            --baseline performance_baseline.json \
            --current test_results.json \
            --threshold 10  # Allow 10% degradation

      - name: Comment PR
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '✅ Performance tests passed! No regressions detected.'
            })
```

---

**文件版本**: V2.0 (Enhanced)
**最後更新**: 2026-01-27
**狀態**: Architecture-Level Complete

## 4. 驗收發布 (UAT & Release)

### 4.1 上線檢查表 (Go-Live Checklist)
1.  [ ] 所有 P1/P2 Bug 已修復。
2.  [ ] 壓力測試報告 (Load Test Report) 已簽核。
3.  [ ] 資料庫變更腳本 (Migration Script) 已驗證可回滾。
4.  [ ] 監控儀表板 (Grafana) 已配置完畢。
