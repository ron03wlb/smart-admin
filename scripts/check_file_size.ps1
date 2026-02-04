# PowerShell 版本 - 檢查文件是否超過大小限制

$maxLines = 2500
$largeFiles = 0

Write-Host "🔍 檢查文件大小..." -ForegroundColor Cyan

Get-ChildItem -Path "docs/iGaming" -Filter "*.md" -Recurse | ForEach-Object {
    $file = $_.FullName
    $lines = (Get-Content $file | Measure-Object -Line).Lines

    if ($lines -gt $maxLines) {
        Write-Host "⚠️  超大文件: $file ($lines lines, 限制 $maxLines)" -ForegroundColor Yellow
        $largeFiles++
    }
}

if ($largeFiles -le 5) {
    Write-Host "✅ 超大文件數量符合規範 (<= 5 個)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "❌ 超大文件數量過多 ($largeFiles > 5)" -ForegroundColor Red
    exit 1
}
