#!/bin/bash
# Test Scenario 3: Draw/Tie - No Turnover
# Expected: validTurnoverFinance = 0 (DRAW status = 0% factor)

curl -X POST "http://localhost:1024/igaming/activity/turnover/calculate" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "betId": "BET-2026-03-12-003",
    "playerId": 1003,
    "tenantId": 1,
    "betAmount": 100.00,
    "gameCategory": 2,
    "settlementStatus": 3,
    "oddsValue": 1.95,
    "oddsType": 1,
    "riskScore": 0
  }'

echo ""
echo "Expected Result:"
echo "  - effectiveTurnoverBase: 100.00 (passes risk filter)"
echo "  - validTurnoverFinance: 0.00 (DRAW status × 0% factor)"
echo "  - activityValidTurnover: 0.00"
echo "  - statusFactor: 0.00 (DRAW settlement status)"
echo "  - gameWeight: 0.15 (Baccarat/Live Casino)"
echo "  - rejected: false (not rejected, just 0% turnover)"
echo "  - Reason: settlementStatus=3 (DRAW) has 0% status factor"
