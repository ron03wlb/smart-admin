#!/bin/bash
# Test Scenario 2: Risk Filter Block (Odds Too Low)
# Expected: effectiveTurnoverBase = 0 (BLOCK)

curl -X POST "http://localhost:1024/igaming/activity/turnover/calculate" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "betId": "BET-2026-03-12-002",
    "playerId": 1002,
    "tenantId": 1,
    "betAmount": 100.00,
    "gameCategory": 3,
    "settlementStatus": 1,
    "oddsValue": 1.20,
    "oddsType": 1,
    "riskScore": 0
  }'

echo ""
echo "Expected Result:"
echo "  - effectiveTurnoverBase: 0.00 (rejected by odds threshold)"
echo "  - validTurnoverFinance: 0.00"
echo "  - activityValidTurnover: 0.00"
echo "  - rejected: true"
echo "  - rejectedBy: 'ODDS_TOO_LOW'"
echo "  - Reason: oddsValue (1.20) < threshold (1.50 for Sports Betting)"
