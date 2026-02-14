# Update rule paths in all .claude/ directory Markdown files

$rootPath = "C:\Workspace\open_source\smart-admin\.claude"

# Define path mappings (old pattern -> new pattern)
$pathMappings = @{
    # Decision matrix (special case - may appear with or without .agent prefix)
    "\.agent/rules/00-ai-decision-matrix\.md" = ".agent/rules/00-INDEX.md"
    "rules/00-ai-decision-matrix\.md" = "00-INDEX.md"

    # Foundation
    "\.agent/rules/01-naming-conventions\.md" = ".agent/rules/foundation/01-naming-conventions.md"
    "rules/01-naming-conventions\.md" = "foundation/01-naming-conventions.md"

    "\.agent/rules/02-oop-principles\.md" = ".agent/rules/foundation/02-oop-principles.md"
    "rules/02-oop-principles\.md" = "foundation/02-oop-principles.md"

    "\.agent/rules/09-manager-layer\.md" = ".agent/rules/foundation/09-manager-layer.md"
    "rules/09-manager-layer\.md" = "foundation/09-manager-layer.md"

    "\.agent/rules/10-architecture-rules\.md" = ".agent/rules/foundation/10-architecture-rules.md"
    "rules/10-architecture-rules\.md" = "foundation/10-architecture-rules.md"

    # Technology/Database
    "\.agent/rules/05-postgresql-basics\.md" = ".agent/rules/technology/database/05-postgresql-basics.md"
    "rules/05-postgresql-basics\.md" = "technology/database/05-postgresql-basics.md"

    "\.agent/rules/05-postgresql-advanced\.md" = ".agent/rules/technology/database/05-postgresql-advanced.md"
    "rules/05-postgresql-advanced\.md" = "technology/database/05-postgresql-advanced.md"

    "\.agent/rules/05-postgresql-mybatis-integration\.md" = ".agent/rules/technology/database/05-postgresql-mybatis-integration.md"
    "rules/05-postgresql-mybatis-integration\.md" = "technology/database/05-postgresql-mybatis-integration.md"

    "\.agent/rules/09-mybatis-plus-core\.md" = ".agent/rules/technology/database/09-mybatis-plus-core.md"
    "rules/09-mybatis-plus-core\.md" = "technology/database/09-mybatis-plus-core.md"

    "\.agent/rules/09-mybatis-plus-postgresql\.md" = ".agent/rules/technology/database/09-mybatis-plus-postgresql.md"
    "rules/09-mybatis-plus-postgresql\.md" = "technology/database/09-mybatis-plus-postgresql.md"

    # Technology/Functional
    "\.agent/rules/08-vavr-fundamentals\.md" = ".agent/rules/technology/functional/08-vavr-fundamentals.md"
    "rules/08-vavr-fundamentals\.md" = "technology/functional/08-vavr-fundamentals.md"

    "\.agent/rules/08-vavr-advanced\.md" = ".agent/rules/technology/functional/08-vavr-advanced.md"
    "rules/08-vavr-advanced\.md" = "technology/functional/08-vavr-advanced.md"

    "\.agent/rules/08-vavr-mybatis-integration\.md" = ".agent/rules/technology/functional/08-vavr-mybatis-integration.md"
    "rules/08-vavr-mybatis-integration\.md" = "technology/functional/08-vavr-mybatis-integration.md"

    # Technology/Patterns
    "\.agent/rules/03-concurrency-rules\.md" = ".agent/rules/technology/patterns/03-concurrency-rules.md"
    "rules/03-concurrency-rules\.md" = "technology/patterns/03-concurrency-rules.md"

    "\.agent/rules/04-exception-logging\.md" = ".agent/rules/technology/patterns/04-exception-logging.md"
    "rules/04-exception-logging\.md" = "technology/patterns/04-exception-logging.md"

    # Security
    "\.agent/rules/07-owasp-top10-part1\.md" = ".agent/rules/security/07-owasp-top10-part1.md"
    "rules/07-owasp-top10-part1\.md" = "security/07-owasp-top10-part1.md"

    "\.agent/rules/07-owasp-top10-part2\.md" = ".agent/rules/security/07-owasp-top10-part2.md"
    "rules/07-owasp-top10-part2\.md" = "security/07-owasp-top10-part2.md"

    # Quality Tools
    "\.agent/rules/11-checkstyle-rules\.md" = ".agent/rules/quality-tools/11-checkstyle-rules.md"
    "rules/11-checkstyle-rules\.md" = "quality-tools/11-checkstyle-rules.md"

    "\.agent/rules/12-pmd-rules\.md" = ".agent/rules/quality-tools/12-pmd-rules.md"
    "rules/12-pmd-rules\.md" = "quality-tools/12-pmd-rules.md"

    "\.agent/rules/13-spotbugs-rules\.md" = ".agent/rules/quality-tools/13-spotbugs-rules.md"
    "rules/13-spotbugs-rules\.md" = "quality-tools/13-spotbugs-rules.md"

    "\.agent/rules/14-spotless-rules\.md" = ".agent/rules/quality-tools/14-spotless-rules.md"
    "rules/14-spotless-rules\.md" = "quality-tools/14-spotless-rules.md"

    "\.agent/rules/15-error-prone-rules\.md" = ".agent/rules/quality-tools/15-error-prone-rules.md"
    "rules/15-error-prone-rules\.md" = "quality-tools/15-error-prone-rules.md"

    "\.agent/rules/16-jacoco-coverage-rules\.md" = ".agent/rules/quality-tools/16-jacoco-coverage-rules.md"
    "rules/16-jacoco-coverage-rules\.md" = "quality-tools/16-jacoco-coverage-rules.md"

    # Workflows
    "\.agent/rules/06-sonarqube-rules\.md" = ".agent/rules/workflows/06-sonarqube-rules.md"
    "rules/06-sonarqube-rules\.md" = "workflows/06-sonarqube-rules.md"

    "\.agent/rules/17-commit-message-conventions\.md" = ".agent/rules/workflows/17-commit-message-conventions.md"
    "rules/17-commit-message-conventions\.md" = "workflows/17-commit-message-conventions.md"
}

# Get all Markdown files recursively
$mdFiles = Get-ChildItem -Path $rootPath -Filter "*.md" -Recurse

Write-Host "Found $($mdFiles.Count) Markdown files to process in .claude/"

$totalReplacements = 0
$updatedFiles = 0

foreach ($file in $mdFiles) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $fileReplacements = 0

    foreach ($oldPattern in $pathMappings.Keys) {
        $newPath = $pathMappings[$oldPattern]

        if ($content -match $oldPattern) {
            $content = $content -replace $oldPattern, $newPath
            $count = ([regex]::Matches($originalContent, $oldPattern)).Count
            $fileReplacements += $count
            $relativePath = $file.FullName.Replace("$rootPath\", "")
            Write-Host "  $relativePath`: Replaced pattern -> '$newPath' ($count occurrences)"
        }
    }

    if ($fileReplacements -gt 0) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $totalReplacements += $fileReplacements
        $updatedFiles++
        $relativePath = $file.FullName.Replace("$rootPath\", "")
        Write-Host "✓ Updated: $relativePath ($fileReplacements replacements)"
        Write-Host ""
    }
}

Write-Host "`n========================================="
Write-Host "Total files updated: $updatedFiles"
Write-Host "Total replacements: $totalReplacements"
Write-Host "========================================="
Write-Host "Script completed successfully!"
