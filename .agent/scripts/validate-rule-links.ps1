# Validate all rule links in SmartAdmin documentation

$rootPath = "C:\Workspace\open_source\smart-admin"
$errors = @()
$warnings = @()
$totalLinks = 0
$validLinks = 0

Write-Host "========================================"
Write-Host "SmartAdmin Rule Link Validator"
Write-Host "========================================"
Write-Host ""

# Get all Markdown files
$mdFiles = @()
$mdFiles += Get-ChildItem -Path "$rootPath\.agent\rules" -Filter "*.md" -Recurse
$mdFiles += Get-ChildItem -Path "$rootPath\.claude" -Filter "*.md" -Recurse
$mdFiles += Get-ChildItem -Path "$rootPath\CLAUDE.md"
$mdFiles += Get-ChildItem -Path "$rootPath\README.md"

Write-Host "Scanning $($mdFiles.Count) Markdown files...`n"

foreach ($file in $mdFiles) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $relativePath = $file.FullName.Replace("$rootPath\", "")

    # Find Markdown links: [text](path)
    $linkPattern = '\[([^\]]+)\]\(([^)]+)\)'
    $matches = [regex]::Matches($content, $linkPattern)

    foreach ($match in $matches) {
        $linkText = $match.Groups[1].Value
        $linkPath = $match.Groups[2].Value

        $totalLinks++

        # Skip external URLs
        if ($linkPath -match '^https?://') {
            continue
        }

        # Skip anchors
        if ($linkPath -match '^#') {
            continue
        }

        # Extract file path (remove anchor if present)
        $filePath = $linkPath -replace '#.*$', ''

        # Resolve relative path
        $baseDir = Split-Path -Parent $file.FullName
        $targetPath = Join-Path $baseDir $filePath
        $targetPath = [System.IO.Path]::GetFullPath($targetPath)

        # Check if file exists
        if (-not (Test-Path $targetPath)) {
            $errors += [PSCustomObject]@{
                File = $relativePath
                Link = $linkPath
                LinkText = $linkText
                Expected = $targetPath.Replace("$rootPath\", "")
            }
        } else {
            $validLinks++
        }
    }

    # Check for old rule path patterns (should have been updated)
    $oldPatterns = @(
        'rules/01-naming-conventions\.md',
        'rules/10-architecture-rules\.md',
        'rules/08-vavr',
        'rules/09-mybatis',
        'rules/05-postgresql',
        'rules/11-checkstyle',
        'rules/12-pmd',
        'rules/13-spotbugs',
        'rules/00-ai-decision-matrix\.md'
    )

    foreach ($pattern in $oldPatterns) {
        if ($content -match $pattern) {
            $warnings += [PSCustomObject]@{
                File = $relativePath
                Pattern = $pattern
                Message = "Old rule path pattern detected (may need manual review)"
            }
        }
    }
}

Write-Host "========================================"
Write-Host "Validation Results"
Write-Host "========================================"
Write-Host "Total links checked: $totalLinks"
Write-Host "Valid links: $validLinks"
Write-Host "Broken links: $($errors.Count)"
Write-Host "Warnings: $($warnings.Count)"
Write-Host ""

if ($errors.Count -gt 0) {
    Write-Host "❌ BROKEN LINKS FOUND:`n" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "File: $($error.File)" -ForegroundColor Yellow
        Write-Host "  Link: [$($error.LinkText)]($($error.Link))"
        Write-Host "  Expected: $($error.Expected)"
        Write-Host ""
    }
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️  WARNINGS:`n" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "File: $($warning.File)" -ForegroundColor Yellow
        Write-Host "  Pattern: $($warning.Pattern)"
        Write-Host "  Message: $($warning.Message)"
        Write-Host ""
    }
}

if ($errors.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "✅ All links validated successfully!" -ForegroundColor Green
    Write-Host "✅ No old rule path patterns detected!" -ForegroundColor Green
    exit 0
} elseif ($errors.Count -eq 0) {
    Write-Host "✅ No broken links found" -ForegroundColor Green
    Write-Host "⚠️  Some warnings detected (manual review recommended)" -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "❌ Validation failed: $($errors.Count) broken link(s) found" -ForegroundColor Red
    exit 1
}
