#!/bin/bash
# Test Scenario 1: Normal Calculation (Baccarat Win)
# Expected: activityValidTurnover = 100 × 1.0 × 0.15 = 15.00

curl -X POST "http://localhost:1024/igaming/activity/turnover/calculate" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "betId": "BET-2026-03-12-001",
    "playerId": 1001,
    "tenantId": 1,
    "betAmount": 100.00,
    "gameCategory": 2,
    "settlementStatus": 1,
    "oddsValue": 1.95,
    "oddsType": 1,
    "riskScore": 0
  }'

echo ""
echo "Expected Result:"
echo "  - effectiveTurnoverBase: 100.00 (betAmount × 100% risk factor)"
echo "  - validTurnoverFinance: 100.00 (effectiveTurnoverBase × 100% status factor)"
echo "  - activityValidTurnover: 15.00 (validTurnoverFinance × 15% game weight)"
echo "  - gameWeight: 0.15 (Baccarat/Live Casino)"
echo "  - statusFactor: 1.00 (WIN settlement)"
echo "  - riskActionType: 1 (RA_PASS)"
echo "  - rejected: false"
