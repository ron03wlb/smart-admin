#!/bin/bash
# Master Test Script: Run all 4 turnover calculation scenarios
#
# Prerequisites:
# 1. Application must be running on http://localhost:1024
# 2. You must obtain an authentication token first
#
# To get authentication token:
# 1. Open http://localhost:1024/doc.html in browser
# 2. Use /login endpoint with admin credentials
# 3. Copy the Sa-Token from response
# 4. Set it as environment variable: export SA_TOKEN="your-token-here"
#
# Or run this script with token as argument:
#   ./run-all-turnover-tests.sh "your-token-here"

# Set token from argument or environment variable
TOKEN="${1:-$SA_TOKEN}"

if [ -z "$TOKEN" ]; then
  echo "ERROR: Authentication token not provided"
  echo "Usage: $0 <sa-token>"
  echo "   or: export SA_TOKEN='your-token' && $0"
  exit 1
fi

echo "=========================================="
echo "SmartAdmin Turnover Calculation Test Suite"
echo "=========================================="
echo "Token: ${TOKEN:0:20}..."
echo ""

# Function to run a test scenario
run_test() {
  local scenario_name=$1
  local scenario_file=$2

  echo "=========================================="
  echo "Running: $scenario_name"
  echo "=========================================="

  # Execute the scenario with authentication token
  bash "$scenario_file" | sed "s|curl -X POST|curl -X POST -H \"satoken: $TOKEN\"|"

  echo ""
  echo "Press Enter to continue to next test..."
  read -r
}

# Run all 4 test scenarios
run_test "Scenario 1: Normal Calculation (Baccarat Win)" "test-scenario-1-baccarat-win.sh"
run_test "Scenario 2: Risk Filter Block (Odds Too Low)" "test-scenario-2-odds-blocked.sh"
run_test "Scenario 3: Draw/Tie - No Turnover" "test-scenario-3-draw-no-turnover.sh"
run_test "Scenario 4: High Weight Game (Slots)" "test-scenario-4-slots-high-weight.sh"

echo "=========================================="
echo "All tests completed!"
echo "=========================================="
