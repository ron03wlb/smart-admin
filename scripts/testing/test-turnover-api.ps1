# PowerShell Script: Test Turnover Calculation API
# Usage: .\test-turnover-api.ps1

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "SmartAdmin Turnover API Quick Test" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Test if application is running
Write-Host "Step 1: Checking if application is running on port 1024..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:1024/doc.html" -Method GET -TimeoutSec 5
    Write-Host "✓ Application is running (Status: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ Application is NOT running or not accessible" -ForegroundColor Red
    Write-Host "  Please ensure SmartAdmin is started: ./gradlew :smartadmin-app:bootRun" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 2: Login to get authentication token
Write-Host "Step 2: Obtaining authentication token..." -ForegroundColor Yellow
Write-Host "Please enter admin credentials:" -ForegroundColor Gray

$loginName = Read-Host "Login Name (default: admin)"
if ([string]::IsNullOrWhiteSpace($loginName)) {
    $loginName = "admin"
}

$password = Read-Host "Password" -AsSecureString
$passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
)

$loginBody = @{
    loginName = $loginName
    password = $passwordPlain
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
        exit 1
    }
} catch {
    Write-Host "✗ Login request failed: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Step 3: Test Turnover Calculation Endpoint
Write-Host "Step 3: Testing Turnover Calculation API..." -ForegroundColor Yellow
Write-Host ""

# Scenario 1: Normal Calculation (Baccarat Win)
Write-Host "Scenario 1: Normal Calculation (Baccarat Win)" -ForegroundColor Cyan
$testBody1 = @{
    betId = "BET-2026-03-12-001"
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
        "satoken" = $token
    }

    $result1 = Invoke-RestMethod -Uri "http://localhost:1024/igaming/activity/turnover/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $testBody1

    Write-Host "Response:" -ForegroundColor Gray
    $result1.data | Format-List

    if ($result1.ok -eq $true) {
        Write-Host "✓ Test passed!" -ForegroundColor Green

        # Validate expected results
        $expectedTurnover = 15.00
        $actualTurnover = $result1.data.activityValidTurnover

        if ([Math]::Abs($actualTurnover - $expectedTurnover) -lt 0.01) {
            Write-Host "✓ Activity Valid Turnover: $actualTurnover (Expected: $expectedTurnover)" -ForegroundColor Green
        } else {
            Write-Host "✗ Activity Valid Turnover: $actualTurnover (Expected: $expectedTurnover)" -ForegroundColor Red
        }

        Write-Host "  - Effective Turnover Base: $($result1.data.effectiveTurnoverBase)" -ForegroundColor Gray
        Write-Host "  - Valid Turnover Finance: $($result1.data.validTurnoverFinance)" -ForegroundColor Gray
        Write-Host "  - Game Weight: $($result1.data.gameWeight)" -ForegroundColor Gray
        Write-Host "  - Status Factor: $($result1.data.statusFactor)" -ForegroundColor Gray
        Write-Host "  - Risk Action Type: $($result1.data.riskActionType)" -ForegroundColor Gray
        Write-Host "  - Rejected: $($result1.data.rejected)" -ForegroundColor Gray
    } else {
        Write-Host "✗ Test failed: $($result1.msg)" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ API request failed: $_" -ForegroundColor Red
    Write-Host "  Error Details: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test completed!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Run additional test scenarios using bash scripts in project root" -ForegroundColor Gray
Write-Host "2. Access Swagger UI: http://localhost:1024/doc.html" -ForegroundColor Gray
Write-Host "3. Review test guide: TURNOVER_API_TESTING_GUIDE.md" -ForegroundColor Gray
