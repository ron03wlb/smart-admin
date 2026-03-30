# PowerShell Script: Automated Turnover Calculation API Test
# Usage: .\test-turnover-auto.ps1
# Uses default admin credentials (admin/123456)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "SmartAdmin Turnover API Automated Test" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Test if application is running
Write-Host "Step 1: Checking if application is running on port 1024..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:1024/doc.html" -Method GET -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Application is running (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ Application is NOT running or not accessible" -ForegroundColor Red
    Write-Host "  Please ensure SmartAdmin is started: ./gradlew :smartadmin-app:bootRun" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: Login with default credentials
Write-Host "Step 2: Logging in with default admin credentials..." -ForegroundColor Yellow

$loginBody = @{
    loginName = "admin"
    password = "123456"
    loginDevice = 1  # PC端登入
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:1024/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody

    if ($loginResponse.ok -eq $true) {
        $token = $loginResponse.data.token
        Write-Host "✓ Login successful! Token obtained." -ForegroundColor Green
        Write-Host "  Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
    } else {
        Write-Host "✗ Login failed: $($loginResponse.msg)" -ForegroundColor Red
        Write-Host "  Possible reasons:" -ForegroundColor Yellow
        Write-Host "    1. Default password has been changed" -ForegroundColor Yellow
        Write-Host "    2. Admin account does not exist" -ForegroundColor Yellow
        Write-Host "  Please check your database or use manual login" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "✗ Login request failed: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 3: Test Turnover Calculation - Scenario 1
Write-Host "Step 3: Testing Turnover Calculation API (Scenario 1: Baccarat Win)..." -ForegroundColor Yellow
Write-Host ""

$testBody1 = @{
    betId = "BET-2026-03-13-001"
    playerId = 1001
    tenantId = 1
    betAmount = 100.00
    gameCategory = 2
    settlementStatus = 1
    oddsValue = 1.95
    oddsType = 1
    riskScore = 0
} | ConvertTo-Json

try {
    $headers = @{
        "Content-Type" = "application/json"
        "x-sa-token" = $token
    }

    $result1 = Invoke-RestMethod -Uri "http://localhost:1024/igaming/activity/turnover/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $testBody1

    Write-Host "Response:" -ForegroundColor Gray
    Write-Host "  Code: $($result1.code)" -ForegroundColor Gray
    Write-Host "  OK: $($result1.ok)" -ForegroundColor Gray
    Write-Host "  Message: $($result1.msg)" -ForegroundColor Gray

    if ($result1.ok -eq $true) {
        Write-Host ""
        Write-Host "✓ Test passed!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Calculation Results:" -ForegroundColor Cyan
        Write-Host "  Bet ID: $($result1.data.betId)" -ForegroundColor Gray
        Write-Host "  Player ID: $($result1.data.playerId)" -ForegroundColor Gray
        Write-Host "  Bet Amount: $($result1.data.betAmount)" -ForegroundColor Gray
        Write-Host "  Effective Turnover Base: $($result1.data.effectiveTurnoverBase)" -ForegroundColor Gray
        Write-Host "  Valid Turnover Finance: $($result1.data.validTurnoverFinance)" -ForegroundColor Gray
        Write-Host "  Activity Valid Turnover: $($result1.data.activityValidTurnover)" -ForegroundColor Yellow
        Write-Host "  Game Weight: $($result1.data.gameWeight)" -ForegroundColor Gray
        Write-Host "  Status Factor: $($result1.data.statusFactor)" -ForegroundColor Gray
        Write-Host "  Risk Action Type: $($result1.data.riskActionType)" -ForegroundColor Gray
        Write-Host "  Rejected: $($result1.data.rejected)" -ForegroundColor Gray
        Write-Host ""

        # Validate expected results
        $expectedTurnover = 15.00
        $actualTurnover = $result1.data.activityValidTurnover

        if ([Math]::Abs($actualTurnover - $expectedTurnover) -lt 0.01) {
            Write-Host "✓ PASS: Activity Valid Turnover = $actualTurnover (Expected: $expectedTurnover)" -ForegroundColor Green
        } else {
            Write-Host "✗ FAIL: Activity Valid Turnover = $actualTurnover (Expected: $expectedTurnover)" -ForegroundColor Red
        }

        # Validate other key fields
        if ($result1.data.gameWeight -eq 0.15) {
            Write-Host "✓ PASS: Game Weight = 0.15 (Baccarat/Live Casino)" -ForegroundColor Green
        } else {
            Write-Host "✗ FAIL: Game Weight = $($result1.data.gameWeight) (Expected: 0.15)" -ForegroundColor Red
        }

        if ($result1.data.statusFactor -eq 1.00) {
            Write-Host "✓ PASS: Status Factor = 1.00 (WIN settlement)" -ForegroundColor Green
        } else {
            Write-Host "✗ FAIL: Status Factor = $($result1.data.statusFactor) (Expected: 1.00)" -ForegroundColor Red
        }

        if ($result1.data.rejected -eq $false) {
            Write-Host "✓ PASS: Not rejected by risk filter" -ForegroundColor Green
        } else {
            Write-Host "✗ FAIL: Unexpectedly rejected" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ Test failed: $($result1.msg)" -ForegroundColor Red
        if ($null -ne $result1.data) {
            Write-Host "  Error Data: $($result1.data | ConvertTo-Json -Depth 5)" -ForegroundColor Red
        }
    }
} catch {
    Write-Host "✗ API request failed: $_" -ForegroundColor Red
    Write-Host "  Error Details: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        Write-Host "  HTTP Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Test completed!" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Run remaining test scenarios (2-4)" -ForegroundColor Gray
Write-Host "2. Test hot-reload functionality" -ForegroundColor Gray
Write-Host "3. Run performance benchmarks with k6" -ForegroundColor Gray
Write-Host "4. Review application logs for any errors" -ForegroundColor Gray
