#!/bin/bash
# Test Scenario 4: High Weight Game (Slots)
# Expected: gameWeight = 100% (full turnover contribution)

curl -X POST "http://localhost:1024/igaming/activity/turnover/calculate" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "betId": "BET-2026-03-12-004",
    "playerId": 1004,
    "tenantId": 1,
    "betAmount": 100.00,
    "gameCategory": 1,
    "settlementStatus": 1,
    "oddsValue": 50.00,
    "oddsType": 1,
    "riskScore": 0
  }'

echo ""
echo "Expected Result:"
echo "  - effectiveTurnoverBase: 100.00 (betAmount × 100% risk factor)"
echo "  - validTurnoverFinance: 100.00 (effectiveTurnoverBase × 100% status factor)"
echo "  - activityValidTurnover: 100.00 (validTurnoverFinance × 100% game weight)"
echo "  - gameWeight: 1.00 (Slots = 100% weight)"
echo "  - statusFactor: 1.00 (WIN settlement)"
echo "  - riskActionType: 1 (RA_PASS)"
echo "  - rejected: false"
echo "  - Note: Slots have the highest weight (100%) for activity turnover"
