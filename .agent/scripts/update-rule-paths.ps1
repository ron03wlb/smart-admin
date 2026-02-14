# Update rule paths in all .agent/rules/ Markdown files

$rootPath = "C:\Workspace\open_source\smart-admin\.agent\rules"

# Define path mappings (old pattern -> new pattern)
$pathMappings = @{
    # Foundation
    "rules/01-naming-conventions.md" = "foundation/01-naming-conventions.md"
    "rules/02-oop-principles.md" = "foundation/02-oop-principles.md"
    "rules/09-manager-layer.md" = "foundation/09-manager-layer.md"
    "rules/10-architecture-rules.md" = "foundation/10-architecture-rules.md"

    # Technology/Database
    "rules/05-postgresql-basics.md" = "technology/database/05-postgresql-basics.md"
    "rules/05-postgresql-advanced.md" = "technology/database/05-postgresql-advanced.md"
    "rules/05-postgresql-mybatis-integration.md" = "technology/database/05-postgresql-mybatis-integration.md"
    "rules/09-mybatis-plus-core.md" = "technology/database/09-mybatis-plus-core.md"
    "rules/09-mybatis-plus-postgresql.md" = "technology/database/09-mybatis-plus-postgresql.md"

    # Technology/Functional
    "rules/08-vavr-fundamentals.md" = "technology/functional/08-vavr-fundamentals.md"
    "rules/08-vavr-advanced.md" = "technology/functional/08-vavr-advanced.md"
    "rules/08-vavr-mybatis-integration.md" = "technology/functional/08-vavr-mybatis-integration.md"

    # Technology/Patterns
    "rules/03-concurrency-rules.md" = "technology/patterns/03-concurrency-rules.md"
    "rules/04-exception-logging.md" = "technology/patterns/04-exception-logging.md"

    # Security
    "rules/07-owasp-top10-part1.md" = "security/07-owasp-top10-part1.md"
    "rules/07-owasp-top10-part2.md" = "security/07-owasp-top10-part2.md"

    # Quality Tools
    "rules/11-checkstyle-rules.md" = "quality-tools/11-checkstyle-rules.md"
    "rules/12-pmd-rules.md" = "quality-tools/12-pmd-rules.md"
    "rules/13-spotbugs-rules.md" = "quality-tools/13-spotbugs-rules.md"
    "rules/14-spotless-rules.md" = "quality-tools/14-spotless-rules.md"
    "rules/15-error-prone-rules.md" = "quality-tools/15-error-prone-rules.md"
    "rules/16-jacoco-coverage-rules.md" = "quality-tools/16-jacoco-coverage-rules.md"

    # Workflows
    "rules/06-sonarqube-rules.md" = "workflows/06-sonarqube-rules.md"
    "rules/17-commit-message-conventions.md" = "workflows/17-commit-message-conventions.md"

    # Special case: old decision matrix
    "rules/00-ai-decision-matrix.md" = "00-INDEX.md"
}

# Get all Markdown files recursively
$mdFiles = Get-ChildItem -Path $rootPath -Filter "*.md" -Recurse

Write-Host "Found $($mdFiles.Count) Markdown files to process"

$totalReplacements = 0

foreach ($file in $mdFiles) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $fileReplacements = 0

    foreach ($oldPath in $pathMappings.Keys) {
        $newPath = $pathMappings[$oldPath]
        $pattern = [regex]::Escape($oldPath)

        if ($content -match $pattern) {
            $content = $content -replace $pattern, $newPath
            $count = ([regex]::Matches($originalContent, $pattern)).Count
            $fileReplacements += $count
            Write-Host "  $($file.Name): Replaced '$oldPath' -> '$newPath' ($count occurrences)"
        }
    }

    if ($fileReplacements -gt 0) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $totalReplacements += $fileReplacements
        Write-Host "✓ Updated: $($file.FullName) ($fileReplacements replacements)"
    }
}

Write-Host "`nTotal replacements: $totalReplacements"
Write-Host "Script completed successfully!"
