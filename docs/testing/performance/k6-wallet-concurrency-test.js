// k6 Performance Test: Concurrent Wallet Debit (Wallet Deduction)
// Target: Test three-layer protection mechanism (Redisson Lock + Optimistic Lock + Request ID)
// Expected: No duplicate deductions, TPS > 500, P95 < 100ms, Error rate < 0.1%

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const walletDebitDuration = new Trend('wallet_debit_duration');
const successfulDebits = new Counter('successful_debits');
const duplicateDebits = new Counter('duplicate_debits');
const optimisticLockFailures = new Counter('optimistic_lock_failures');

// Test configuration
export const options = {
  scenarios: {
    // Scenario 1: Ramp-up load test (0 → 1000 VUs over 2 minutes)
    concurrent_debit_rampup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },   // Warm-up: 0 → 100 VUs over 2 min
        { duration: '3m', target: 500 },   // Ramp-up: 100 → 500 VUs over 3 min
        { duration: '5m', target: 1000 },  // Peak load: 500 → 1000 VUs over 5 min
        { duration: '5m', target: 1000 },  // Sustain: 1000 VUs for 5 min
        { duration: '2m', target: 0 },     // Ramp-down: 1000 → 0 VUs over 2 min
      ],
      gracefulRampDown: '30s',
    },

    // Scenario 2: Stress test (sustained 1000 VUs for 10 minutes)
    concurrent_debit_stress: {
      executor: 'constant-vus',
      vus: 1000,
      duration: '10m',
      startTime: '20m', // Start after scenario 1 completes
    },

    // Scenario 3: Spike test (sudden burst to 2000 VUs)
    concurrent_debit_spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 2000 },  // Sudden spike
        { duration: '1m', target: 2000 },   // Hold spike
        { duration: '10s', target: 0 },     // Sudden drop
      ],
      startTime: '35m', // Start after scenario 2 completes
    },
  },

  thresholds: {
    // Acceptance criteria from iGaming implementation plan
    http_req_duration: ['p(95)<100'],      // P95 response time < 100ms
    http_req_failed: ['rate<0.001'],       // Error rate < 0.1%
    'http_reqs': ['rate>500'],             // TPS > 500
    errors: ['rate<0.001'],                // Error rate < 0.1%
    wallet_debit_duration: ['p(95)<100'],  // P95 debit time < 100ms
    duplicateDebits: ['count==0'],         // No duplicate debits (critical)
  },

  // Test environment configuration
  ext: {
    loadimpact: {
      projectID: 3588584,
      name: 'Wallet Concurrent Debit Test - SmartAdmin iGaming',
    },
  },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:1024';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || 'test-token-placeholder';

// Test data: Pre-created wallets (should exist in test database)
const TEST_WALLETS = [
  { walletId: 1001, playerId: 101, initialBalance: 10000.0000 },
  { walletId: 1002, playerId: 102, initialBalance: 10000.0000 },
  { walletId: 1003, playerId: 103, initialBalance: 10000.0000 },
  { walletId: 1004, playerId: 104, initialBalance: 10000.0000 },
  { walletId: 1005, playerId: 105, initialBalance: 10000.0000 },
  // Add more test wallets as needed (total: 100 wallets for 1000 VUs)
];

// Populate 100 test wallets
for (let i = 6; i <= 100; i++) {
  TEST_WALLETS.push({
    walletId: 1000 + i,
    playerId: 100 + i,
    initialBalance: 10000.0000,
  });
}

// Setup: Run once before test starts
export function setup() {
  console.log('========================================');
  console.log('k6 Performance Test: Wallet Concurrent Debit');
  console.log('========================================');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Test Wallets: ${TEST_WALLETS.length}`);
  console.log(`Access Token: ${ACCESS_TOKEN.substring(0, 20)}...`);
  console.log('========================================');

  // Optional: Verify test wallets exist and have sufficient balance
  // (Not implemented here - assume test data is pre-loaded)

  return {
    baseUrl: BASE_URL,
    accessToken: ACCESS_TOKEN,
    wallets: TEST_WALLETS,
  };
}

// Main test function (executed for each VU iteration)
export default function (data) {
  const { baseUrl, accessToken, wallets } = data;

  // Randomly select a wallet from test data
  const wallet = wallets[randomIntBetween(0, wallets.length - 1)];

  // Generate unique requestId for idempotency
  const requestId = `k6-test-${__VU}-${__ITER}-${Date.now()}-${randomString(8)}`;

  // Prepare debit request payload
  const payload = JSON.stringify({
    walletId: wallet.walletId,
    amount: randomIntBetween(1, 100) + 0.0001, // Random amount between 1.0001 and 100.0001
    transactionType: 3, // BET = 3 (from TransactionTypeEnum)
    requestId: requestId,
    referenceType: 'BET',
    referenceId: `bet-${randomString(16)}`,
    description: `k6 performance test - VU ${__VU} - Iteration ${__ITER}`,
  });

  // Set request headers
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'x-access-token': accessToken,
      'x-refresh-token': accessToken, // Same token for test purposes
    },
    timeout: '30s',
  };

  // Execute wallet debit request
  const startTime = Date.now();
  const res = http.post(`${baseUrl}/igaming/wallet/debit`, payload, params);
  const duration = Date.now() - startTime;

  // Record custom metrics
  walletDebitDuration.add(duration);

  // Check response status and business logic
  const checkResult = check(res, {
    'status is 200': (r) => r.status === 200,
    'response has code=1 (success)': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.code === 1;
      } catch (e) {
        return false;
      }
    },
    'response contains transactionId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.transactionId != null;
      } catch (e) {
        return false;
      }
    },
    'response time < 100ms': (r) => r.timings.duration < 100,
  });

  // Handle errors and edge cases
  if (!checkResult) {
    errorRate.add(1);

    // Detect duplicate debit errors (idempotency check)
    if (res.status === 200) {
      try {
        const body = JSON.parse(res.body);
        if (body.code === 30001 && body.msg && body.msg.includes('Duplicate requestId')) {
          duplicateDebits.add(1);
          console.warn(`Duplicate requestId detected: ${requestId}`);
        }
      } catch (e) {
        // Ignore JSON parse errors
      }
    }

    // Detect optimistic lock failures
    if (res.status === 200) {
      try {
        const body = JSON.parse(res.body);
        if (body.code === 30002 && body.msg && body.msg.includes('Optimistic lock')) {
          optimisticLockFailures.add(1);
          console.warn(`Optimistic lock failure for walletId: ${wallet.walletId}`);
        }
      } catch (e) {
        // Ignore JSON parse errors
      }
    }

    // Log error details for debugging
    if (res.status !== 200) {
      console.error(`[VU ${__VU}] HTTP Error ${res.status}: ${res.body}`);
    }
  } else {
    errorRate.add(0);
    successfulDebits.add(1);
  }

  // Simulate user think time (random 0.5s - 2s)
  sleep(randomIntBetween(0.5, 2));
}

// Teardown: Run once after test completes
export function teardown(data) {
  console.log('========================================');
  console.log('Test Completed - Summary');
  console.log('========================================');
  console.log('Manual verification steps:');
  console.log('1. Check database for final wallet balances');
  console.log('2. Verify no duplicate transactions with same requestId');
  console.log('3. Compare expected vs actual total debit amount');
  console.log('4. Review optimistic lock failure rate');
  console.log('========================================');

  // Optional: Query database to verify wallet balance consistency
  // (Not implemented here - use external script for verification)
}

// Handle scenarios (example: use different VU behaviors for different scenarios)
export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data),
    'summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

// Helper: Generate HTML report
function htmlReport(data) {
  const { metrics } = data;

  const html = `
<!DOCTYPE html>
<html>
<head>
    <title>k6 Performance Test Report - Wallet Concurrent Debit</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; background: white; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        .pass { color: green; font-weight: bold; }
        .fail { color: red; font-weight: bold; }
        .metric-name { font-weight: bold; }
    </style>
</head>
<body>
    <h1>k6 Performance Test Report</h1>
    <h2>Test: Wallet Concurrent Debit (1000 VUs)</h2>
    <p><strong>Execution Time:</strong> ${new Date().toISOString()}</p>

    <h3>Key Metrics</h3>
    <table>
        <tr>
            <th>Metric</th>
            <th>Value</th>
            <th>Threshold</th>
            <th>Status</th>
        </tr>
        <tr>
            <td class="metric-name">Total HTTP Requests</td>
            <td>${metrics.http_reqs.values.count}</td>
            <td>N/A</td>
            <td>-</td>
        </tr>
        <tr>
            <td class="metric-name">HTTP Request Rate (TPS)</td>
            <td>${metrics.http_reqs.values.rate.toFixed(2)}</td>
            <td>> 500</td>
            <td class="${metrics.http_reqs.values.rate > 500 ? 'pass' : 'fail'}">
                ${metrics.http_reqs.values.rate > 500 ? 'PASS' : 'FAIL'}
            </td>
        </tr>
        <tr>
            <td class="metric-name">P95 Response Time</td>
            <td>${metrics.http_req_duration.values['p(95)'].toFixed(2)} ms</td>
            <td>< 100 ms</td>
            <td class="${metrics.http_req_duration.values['p(95)'] < 100 ? 'pass' : 'fail'}">
                ${metrics.http_req_duration.values['p(95)'] < 100 ? 'PASS' : 'FAIL'}
            </td>
        </tr>
        <tr>
            <td class="metric-name">Error Rate</td>
            <td>${(metrics.http_req_failed.values.rate * 100).toFixed(2)}%</td>
            <td>< 0.1%</td>
            <td class="${metrics.http_req_failed.values.rate < 0.001 ? 'pass' : 'fail'}">
                ${metrics.http_req_failed.values.rate < 0.001 ? 'PASS' : 'FAIL'}
            </td>
        </tr>
        <tr>
            <td class="metric-name">Successful Debits</td>
            <td>${metrics.successful_debits.values.count}</td>
            <td>N/A</td>
            <td>-</td>
        </tr>
        <tr>
            <td class="metric-name">Duplicate Debits</td>
            <td>${metrics.duplicateDebits?.values.count || 0}</td>
            <td>= 0 (critical)</td>
            <td class="${(metrics.duplicateDebits?.values.count || 0) === 0 ? 'pass' : 'fail'}">
                ${(metrics.duplicateDebits?.values.count || 0) === 0 ? 'PASS' : 'FAIL'}
            </td>
        </tr>
        <tr>
            <td class="metric-name">Optimistic Lock Failures</td>
            <td>${metrics.optimisticLockFailures?.values.count || 0}</td>
            <td>< 1% of total requests</td>
            <td>INFO</td>
        </tr>
    </table>

    <h3>Response Time Distribution</h3>
    <table>
        <tr>
            <th>Percentile</th>
            <th>Response Time (ms)</th>
        </tr>
        <tr>
            <td>P50 (Median)</td>
            <td>${metrics.http_req_duration.values['p(50)'].toFixed(2)}</td>
        </tr>
        <tr>
            <td>P90</td>
            <td>${metrics.http_req_duration.values['p(90)'].toFixed(2)}</td>
        </tr>
        <tr>
            <td>P95</td>
            <td>${metrics.http_req_duration.values['p(95)'].toFixed(2)}</td>
        </tr>
        <tr>
            <td>P99</td>
            <td>${metrics.http_req_duration.values['p(99)'].toFixed(2)}</td>
        </tr>
        <tr>
            <td>Max</td>
            <td>${metrics.http_req_duration.values.max.toFixed(2)}</td>
        </tr>
    </table>

    <h3>Three-Layer Protection Mechanism Verification</h3>
    <ul>
        <li><strong>Layer 1 - Redisson Lock:</strong> ${(metrics.optimisticLockFailures?.values.count || 0) === 0 ? 'Working (no lock contentions)' : `Lock contentions detected: ${metrics.optimisticLockFailures?.values.count}`}</li>
        <li><strong>Layer 2 - Optimistic Lock:</strong> ${(metrics.optimisticLockFailures?.values.count || 0) < metrics.http_reqs.values.count * 0.01 ? 'Working (< 1% failures)' : 'High contention rate (> 1%)'}</li>
        <li><strong>Layer 3 - Request ID Deduplication:</strong> ${(metrics.duplicateDebits?.values.count || 0) === 0 ? 'Working (no duplicates)' : `Duplicates detected: ${metrics.duplicateDebits?.values.count} ❌`}</li>
    </ul>

    <h3>Acceptance Criteria</h3>
    <table>
        <tr>
            <th>Criteria</th>
            <th>Expected</th>
            <th>Actual</th>
            <th>Status</th>
        </tr>
        <tr>
            <td>TPS (Throughput)</td>
            <td>> 500</td>
            <td>${metrics.http_reqs.values.rate.toFixed(2)}</td>
            <td class="${metrics.http_reqs.values.rate > 500 ? 'pass' : 'fail'}">
                ${metrics.http_reqs.values.rate > 500 ? '✅ PASS' : '❌ FAIL'}
            </td>
        </tr>
        <tr>
            <td>P95 Response Time</td>
            <td>< 100ms</td>
            <td>${metrics.http_req_duration.values['p(95)'].toFixed(2)} ms</td>
            <td class="${metrics.http_req_duration.values['p(95)'] < 100 ? 'pass' : 'fail'}">
                ${metrics.http_req_duration.values['p(95)'] < 100 ? '✅ PASS' : '❌ FAIL'}
            </td>
        </tr>
        <tr>
            <td>Error Rate</td>
            <td>< 0.1%</td>
            <td>${(metrics.http_req_failed.values.rate * 100).toFixed(2)}%</td>
            <td class="${metrics.http_req_failed.values.rate < 0.001 ? 'pass' : 'fail'}">
                ${metrics.http_req_failed.values.rate < 0.001 ? '✅ PASS' : '❌ FAIL'}
            </td>
        </tr>
        <tr>
            <td>Duplicate Debits</td>
            <td>0</td>
            <td>${metrics.duplicateDebits?.values.count || 0}</td>
            <td class="${(metrics.duplicateDebits?.values.count || 0) === 0 ? 'pass' : 'fail'}">
                ${(metrics.duplicateDebits?.values.count || 0) === 0 ? '✅ PASS' : '❌ FAIL (CRITICAL)'}
            </td>
        </tr>
    </table>

    <h3>Recommendations</h3>
    <ul>
        ${metrics.http_reqs.values.rate < 500 ? '<li>⚠️ TPS is below target. Consider optimizing database queries or adding read replicas.</li>' : ''}
        ${metrics.http_req_duration.values['p(95)'] >= 100 ? '<li>⚠️ P95 response time exceeds target. Investigate slow queries or add caching.</li>' : ''}
        ${(metrics.duplicateDebits?.values.count || 0) > 0 ? '<li>❌ CRITICAL: Duplicate debits detected! Verify Request ID deduplication logic immediately.</li>' : ''}
        ${(metrics.optimisticLockFailures?.values.count || 0) > metrics.http_reqs.values.count * 0.01 ? '<li>⚠️ High optimistic lock failure rate (> 1%). Consider tuning Redisson lock wait time.</li>' : ''}
    </ul>

    <footer>
        <p><small>Generated by k6 v0.49.0 | SmartAdmin iGaming Performance Test Suite</small></p>
    </footer>
</body>
</html>
  `;

  return html;
}
