# PowerShell 版本 - 掃描所有 Markdown 文件中的斷裂鏈接

$docsDir = "docs/iGaming"
$brokenLinks = 0

Write-Host "🔍 掃描 iGaming 文檔鏈接..." -ForegroundColor Cyan

Get-ChildItem -Path $docsDir -Filter "*.md" -Recurse | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content $file -Raw

    # 提取所有 Markdown 鏈接
    $links = [regex]::Matches($content, '\[.*?\]\(([^)]+)\)')

    foreach ($match in $links) {
        $link = $match.Groups[1].Value

        # 忽略外部鏈接和錨點
        if ($link -match '^http' -or $link -match '^#') {
            continue
        }

        # 移除錨點
        $link = $link -replace '#.*$', ''

        # 解析相對路徑
        $dir = Split-Path $file -Parent
        $target = Join-Path $dir $link
        $target = [System.IO.Path]::GetFullPath($target)

        # 檢查文件是否存在
        if (-not (Test-Path $target)) {
            Write-Host "❌ 斷裂鏈接: $file -> $link" -ForegroundColor Red
            $brokenLinks++
        }
    }
}

# 輸出結果
if ($brokenLinks -eq 0) {
    Write-Host "✅ 所有鏈接有效" -ForegroundColor Green
    exit 0
} else {
    Write-Host "❌ 發現 $brokenLinks 個斷裂鏈接" -ForegroundColor Red
    exit 1
}
