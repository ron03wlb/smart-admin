// k6 Quick Test: Wallet Debit Validation (2-minute smoke test)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomString, randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const walletDebitDuration = new Trend('wallet_debit_duration');
const successfulDebits = new Counter('successful_debits');

// Test configuration (simplified for quick validation)
export const options = {
  scenarios: {
    smoke_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },   // Warm-up: 0 → 10 VUs
        { duration: '1m', target: 50 },    // Ramp-up: 10 → 50 VUs
        { duration: '30s', target: 0 },    // Ramp-down: 50 → 0 VUs
      ],
    },
  },

  thresholds: {
    http_req_duration: ['p(95)<100'],      // P95 response time < 100ms
    http_req_failed: ['rate<0.01'],        // Error rate < 1%
    errors: ['rate<0.01'],                 // Error rate < 1%
  },
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:1024';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || 'test-token-placeholder';

// Test data: Pre-created wallets (wallet_id: 101-200)
const TEST_WALLETS = [];
for (let i = 1; i <= 100; i++) {
  TEST_WALLETS.push({
    walletId: 100 + i,
    playerId: 100 + i,
    initialBalance: 10000.0000,
  });
}

// Setup
export function setup() {
  console.log('========================================');
  console.log('k6 Quick Test: Wallet Debit Validation');
  console.log('========================================');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Test Wallets: ${TEST_WALLETS.length}`);
  console.log(`Access Token: ${ACCESS_TOKEN.substring(0, 20)}...`);
  console.log('========================================');

  return {
    baseUrl: BASE_URL,
    accessToken: ACCESS_TOKEN,
    wallets: TEST_WALLETS,
  };
}

// Main test function
export default function (data) {
  const { baseUrl, accessToken, wallets } = data;

  // Randomly select a wallet
  const wallet = wallets[randomIntBetween(0, wallets.length - 1)];

  // Generate unique requestId for idempotency
  const requestId = `k6-quick-${__VU}-${__ITER}-${Date.now()}-${randomString(8)}`;

  // Prepare debit request payload
  const payload = JSON.stringify({
    walletId: wallet.walletId,
    amount: randomIntBetween(1, 10) + 0.0001, // Random amount between 1.0001 and 10.0001
    transactionType: 3, // BET = 3
    requestId: requestId,
    referenceType: 'BET',
    referenceId: `bet-${randomString(16)}`,
    description: `k6 quick test - VU ${__VU} - Iteration ${__ITER}`,
  });

  // Set request headers
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'x-access-token': accessToken,
      'x-refresh-token': accessToken,
    },
    timeout: '10s',
  };

  // Execute wallet debit request
  const startTime = Date.now();
  const res = http.post(`${baseUrl}/igaming/wallet/debit`, payload, params);
  const duration = Date.now() - startTime;

  // Record custom metrics
  walletDebitDuration.add(duration);

  // Check response status
  const checkResult = check(res, {
    'status is 200': (r) => r.status === 200,
    'response has success=true': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true || body.ok === true;
      } catch (e) {
        return false;
      }
    },
    'response time < 100ms': (r) => r.timings.duration < 100,
  });

  // Handle errors
  if (!checkResult) {
    errorRate.add(1);
    console.log(`❌ Request failed: VU ${__VU} - Status ${res.status} - Body: ${res.body.substring(0, 200)}`);
  } else {
    successfulDebits.add(1);
  }

  // Sleep between requests (simulate real-world pacing)
  sleep(0.1);
}

// Teardown
export function teardown(data) {
  console.log('========================================');
  console.log('k6 Quick Test Completed');
  console.log('========================================');
}
