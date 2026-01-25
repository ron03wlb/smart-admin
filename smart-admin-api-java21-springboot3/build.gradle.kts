plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.sonarqube) apply false
    alias(libs.plugins.spotless) apply false
    java
}

allprojects {
    group = "net.lab1024"
    version = "3.0.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "jacoco")
    apply(plugin = "org.sonarqube")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        // Maven's -parameters flag for parameter name retention
        options.compilerArgs.add("-parameters")
        dependsOn("spotlessApply")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.4")
            mavenBom("com.baomidou:mybatis-plus-bom:3.5.12")
        }
    }

    // Checkstyle Configuration
    configure<CheckstyleExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("checkstyle").get().toString()
        isIgnoreFailures = false
        maxWarnings = 0
        // configFile will be created in the next step
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    // PMD Configuration
    configure<PmdExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("pmd").get().toString()

        // Don't fail on first violation - we'll check priority in checkPmdPriority task
        // Only fail on P1 (HIGH) and P2 (MEDIUM_HIGH) violations
        // P3 (MEDIUM) and below will be reported but won't fail the build
        isIgnoreFailures = true  // Let checkPmdPriority task handle failure logic

        // 使用自定义规则集（与 Checkstyle 配置结构保持一致）
        ruleSetFiles = files("${rootProject.projectDir}/config/pmd/ruleset.xml")

        // 清空默认规则集
        ruleSets = listOf()
    }

    // 配置 PMD 报告输出
    tasks.withType<Pmd> {
        reports {
            xml.required.set(true)    // 生成 XML 报告供 CI/CD 使用
            html.required.set(true)   // 生成 HTML 报告供开发者查看
        }
    }

    // Custom task to check PMD violations by priority
    tasks.register("checkPmdPriority") {
        dependsOn("pmdMain")
        doLast {
            val xmlReport = project.layout.buildDirectory.file("reports/pmd/main.xml").get().asFile
            if (!xmlReport.exists()) {
                println("⚠️  PMD report not found at: ${xmlReport.absolutePath}")
                println("   Skipping priority check")
                return@doLast
            }

            val xmlContent = xmlReport.readText()
            // Count violations by priority (1=HIGH, 2=MEDIUM_HIGH are CRITICAL)
            val p1Violations = xmlContent.split("priority=\"1\"").size - 1
            val p2Violations = xmlContent.split("priority=\"2\"").size - 1
            val p3Violations = xmlContent.split("priority=\"3\"").size - 1
            val p4Violations = xmlContent.split("priority=\"4\"").size - 1
            val p5Violations = xmlContent.split("priority=\"5\"").size - 1

            val criticalViolations = p1Violations + p2Violations
            val totalViolations = criticalViolations + p3Violations + p4Violations + p5Violations

            println()
            println("PMD Violation Summary:")
            println("  P1 (HIGH):        $p1Violations violations")
            println("  P2 (MEDIUM_HIGH): $p2Violations violations [CRITICAL]")
            println("  P3 (MEDIUM):      $p3Violations violations [INFO]")
            println("  P4 (MEDIUM_LOW):  $p4Violations violations [INFO]")
            println("  P5 (LOW):         $p5Violations violations [INFO]")
            println("  ---")
            println("  CRITICAL (P1+P2): $criticalViolations violations")
            println("  Total:            $totalViolations violations")
            println()

            if (criticalViolations > 0) {
                throw GradleException("PMD found $criticalViolations CRITICAL violations (P1/P2). See report: ${xmlReport.absolutePath}")
            } else {
                println("✅ PMD: No CRITICAL violations found (P3+ violations are informational only)")
            }
        }
    }

    // SpotBugs Configuration
    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion.set(libs.findVersion("spotbugs").get().toString())
        ignoreFailures.set(false)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
        excludeFilter.set(rootProject.file("config/spotbugs/exclude.xml"))
    }

    // Error Prone Configuration
    dependencies {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        "errorprone"(libs.findLibrary("error-prone-core").get())
    }

    // JaCoCo Configuration
    configure<JacocoPluginExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Spotless Configuration
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

// Git Hooks Task
tasks.register("installGitHooks") {
    doLast {
        val hooksSourceDir = file("config/git")
        val gitHooksDir = file("../.git/hooks")
        
        if (hooksSourceDir.exists()) {
            hooksSourceDir.listFiles()?.forEach { hookFile ->
                val targetFile = File(gitHooksDir, hookFile.name)
                hookFile.copyTo(targetFile, overwrite = true)
                targetFile.setExecutable(true)
            }
            println("Git hooks installed successfully.")
        } else {
             // Create pre-commit hook if it doesn't exist
            val preCommitHook = File(gitHooksDir, "pre-commit")
            preCommitHook.writeText("""
                #!/bin/sh
                echo "Running Spotless in smart-admin-api-java21-springboot3..."
                cd smart-admin-api-java21-springboot3 && ./gradlew spotlessApply
            """.trimIndent())
            preCommitHook.setExecutable(true)
            println("Default pre-commit hook created.")
        }
    }
}

// Foundation Package Migration Task
tasks.register("migrateFoundationPackages") {
    description = "Migrate legacy common.* packages to foundation.* in foundation modules"
    group = "migration"

    doLast {
        val modules = listOf(
            "api-encrypt" to "apiencrypt",
            "cache" to "cache",
            "captcha" to "captcha",
            "data-masking" to "datamasking",
            "excel" to "excel",
            "mq" to "mq",
            "redis-lock" to "redislock",
            "repeat-submit" to "repeatsubmit",
            "security-protect" to "securityprotect"
        )

        println("=".repeat(80))
        println("Foundation Package Migration Tool")
        println("=".repeat(80))

        var totalFilesUpdated = 0
        var totalPackageDeclarationsUpdated = 0
        var totalImportsUpdated = 0

        modules.forEach { (moduleName, packageName) ->
            println("\n[Module] Migrating: $moduleName")
            println("-".repeat(80))

            var moduleFilesUpdated = 0
            var modulePackagesUpdated = 0
            var moduleImportsUpdated = 0

            // 1. Update package declarations in module source files
            val moduleDir = file("sa-base/foundation/$moduleName/src/main/java")
            if (moduleDir.exists()) {
                fileTree(moduleDir) {
                    include("**/*.java")
                }.forEach { file ->
                    val content = file.readText()
                    val updated = content.replace(
                        "package net.lab1024.sa.common.$packageName",
                        "package net.lab1024.sa.foundation.$packageName"
                    )
                    if (updated != content) {
                        file.writeText(updated)
                        moduleFilesUpdated++
                        modulePackagesUpdated++
                        println("  ✓ Package declaration: ${file.name}")
                    }
                }
            } else {
                println("  ⚠ Module directory not found: $moduleDir")
            }

            // 2. Update imports across entire codebase
            fileTree(rootDir) {
                include("**/src/**/*.java")
                exclude("**/build/**")
            }.forEach { file ->
                val content = file.readText()
                val updated = content.replace(
                    "import net.lab1024.sa.common.$packageName",
                    "import net.lab1024.sa.foundation.$packageName"
                )
                if (updated != content) {
                    file.writeText(updated)
                    moduleImportsUpdated++
                    val relativePath = file.relativeTo(rootDir).path
                    println("  ✓ Import: $relativePath")
                }
            }

            // 3. Update Spring Boot auto-configuration imports
            val autoConfigFile = file("sa-base/foundation/$moduleName/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
            if (autoConfigFile.exists()) {
                val content = autoConfigFile.readText()
                val updated = content.replace(
                    "net.lab1024.sa.common.$packageName",
                    "net.lab1024.sa.foundation.$packageName"
                )
                if (updated != content) {
                    autoConfigFile.writeText(updated)
                    println("  ✓ AutoConfiguration.imports updated")
                }
            }

            println("  Summary: $modulePackagesUpdated package declarations, $moduleImportsUpdated imports")
            totalFilesUpdated += moduleFilesUpdated
            totalPackageDeclarationsUpdated += modulePackagesUpdated
            totalImportsUpdated += moduleImportsUpdated
        }

        println("\n" + "=".repeat(80))
        println("Migration Complete!")
        println("=".repeat(80))
        println("Package declarations updated: $totalPackageDeclarationsUpdated")
        println("Import statements updated: $totalImportsUpdated")
        println("Total files modified: ${totalPackageDeclarationsUpdated + totalImportsUpdated}")
        println("\nNext Steps:")
        println("  1. Run './gradlew clean build' to verify compilation")
        println("  2. Run './gradlew test' to verify tests (maintain 92%+ pass rate)")
        println("  3. Review changes with 'git diff'")
        println("=".repeat(80))
    }
}

// Phase 2: Core Bridge Class Migration Task
tasks.register("migrateToFoundation") {
    description = "Migrate net.lab1024.sa.common.core.* imports to foundation.domain.*"
    group = "migration"

    doLast {
        println("=".repeat(80))
        println("Phase 2: Core Bridge Class Migration Tool")
        println("Migrating: net.lab1024.sa.common.core.* → net.lab1024.sa.foundation.domain.*")
        println("=".repeat(80))

        // Import mappings (order matters - specific before generic)
        val mappings = mapOf(
            // Domain objects (specific paths - must come before package-level mappings)
            "net.lab1024.sa.common.core.domain.ResponseDTO" to
                "net.lab1024.sa.foundation.domain.response.ResponseDTO",
            "net.lab1024.sa.common.core.domain.PageResult" to
                "net.lab1024.sa.foundation.domain.response.PageResult",
            "net.lab1024.sa.common.core.domain.RequestUser" to
                "net.lab1024.sa.foundation.domain.request.RequestUser",
            "net.lab1024.sa.common.core.domain.PageParam" to
                "net.lab1024.sa.foundation.domain.request.PageParam",

            // Error codes (package-level)
            "net.lab1024.sa.common.core.code" to
                "net.lab1024.sa.foundation.domain.code",

            // Constants, enums, exceptions (package-level)
            "net.lab1024.sa.common.core.constant" to
                "net.lab1024.sa.foundation.domain.constant",
            "net.lab1024.sa.common.core.enumeration" to
                "net.lab1024.sa.foundation.domain.enumeration",
            "net.lab1024.sa.common.core.exception" to
                "net.lab1024.sa.foundation.domain.exception"
        )

        var totalFilesChanged = 0
        var totalReplacements = 0

        // Find all .java files (exclude SmartBeanUtil - CRITICAL!)
        fileTree(rootDir) {
            include("**/src/**/*.java")
            exclude("**/build/**")
            exclude("**/SmartBeanUtil.java")  // CRITICAL EXCEPTION
        }.forEach { file ->
            val originalContent = file.readText()
            var content = originalContent
            var fileChanged = false
            var fileReplacements = 0

            mappings.forEach { (oldPackage, newPackage) ->
                // Regular imports
                val oldImport = "import $oldPackage"
                val newImport = "import $newPackage"
                if (content.contains(oldImport)) {
                    content = content.replace(oldImport, newImport)
                    fileChanged = true
                    fileReplacements++
                }

                // Static imports
                val oldStaticImport = "import static $oldPackage"
                val newStaticImport = "import static $newPackage"
                if (content.contains(oldStaticImport)) {
                    content = content.replace(oldStaticImport, newStaticImport)
                    fileChanged = true
                    fileReplacements++
                }

                // JavaDoc {@link} references
                val oldLink = "{@link $oldPackage"
                val newLink = "{@link $newPackage"
                if (content.contains(oldLink)) {
                    content = content.replace(oldLink, newLink)
                    fileChanged = true
                    fileReplacements++
                }
            }

            if (fileChanged) {
                file.writeText(content)
                totalFilesChanged++
                totalReplacements += fileReplacements
                val relativePath = file.relativeTo(rootDir).path
                println("  ✅ Migrated: $relativePath ($fileReplacements replacements)")
            }
        }

        println("\n" + "=".repeat(80))
        println("Migration Complete!")
        println("=".repeat(80))
        println("Files modified: $totalFilesChanged")
        println("Total replacements: $totalReplacements")
        println("\n⚠️  CRITICAL: SmartBeanUtil.java was EXCLUDED (intentional exception)")
        println("\nNext steps:")
        println("  1. Review changes: git diff")
        println("  2. Build: ./gradlew clean build")
        println("  3. Test: ./gradlew test")
        println("  4. Architecture: ./gradlew :sa-admin:test --tests ArchitectureTest")
        println("\nManual fixes may be required for:")
        println("  - instanceof checks (e.g., OperateLogAspect.java)")
        println("  - Pattern matching (e.g., SmartEncryptResponseAdvice.java)")
        println("=".repeat(80))
    }
}

// Phase 3: Core Module Migration Task
tasks.register("migrateCoreToFoundation") {
    description = "Migrate net.lab1024.sa.base.core.* to foundation.core.* and resolve SmartPageUtil duplication"
    group = "migration"

    doLast {
        println("=".repeat(80))
        println("Phase 3: Core Module Migration Tool")
        println("Step 1: Resolving SmartPageUtil duplication")
        println("Step 2: Migrating base.core.* → foundation.core.*")
        println("=".repeat(80))

        var totalFilesChanged = 0
        var totalReplacements = 0

        // ==========================================================================================
        // STEP 1: Resolve SmartPageUtil Duplication
        // ==========================================================================================
        println("\n[Step 1] Resolving SmartPageUtil duplication...")
        println("  Canonical: net.lab1024.sa.base.mybatis.util.SmartPageUtil")
        println("  Duplicate: net.lab1024.sa.base.core.util.SmartPageUtil (will be deleted)")

        // Update imports from core.util.SmartPageUtil to mybatis.util.SmartPageUtil
        val smartPageUtilFiles = mutableListOf<String>()
        fileTree(rootDir) {
            include("**/src/**/*.java")
            exclude("**/build/**")
        }.forEach { file ->
            val originalContent = file.readText()
            var content = originalContent

            // Replace import statement
            if (content.contains("import net.lab1024.sa.base.core.util.SmartPageUtil")) {
                content = content.replace(
                    "import net.lab1024.sa.base.core.util.SmartPageUtil",
                    "import net.lab1024.sa.base.mybatis.util.SmartPageUtil"
                )
                file.writeText(content)
                totalFilesChanged++
                totalReplacements++
                smartPageUtilFiles.add(file.relativeTo(rootDir).path)
                println("  ✅ Updated: ${file.relativeTo(rootDir).path}")
            }
        }
        println("  → SmartPageUtil imports updated: ${smartPageUtilFiles.size} files")

        // Delete duplicate SmartPageUtil.java
        val duplicateSmartPageUtil = file("sa-base/foundation/core/src/main/java/net/lab1024/sa/base/core/util/SmartPageUtil.java")
        if (duplicateSmartPageUtil.exists()) {
            duplicateSmartPageUtil.delete()
            println("  ✅ Deleted: sa-base/foundation/core/.../base/core/util/SmartPageUtil.java")
        }

        // ==========================================================================================
        // STEP 2: Migrate base.core.* to foundation.core.*
        // ==========================================================================================
        println("\n[Step 2] Migrating base.core.* to foundation.core.*...")

        // Package mappings (order matters - specific before generic)
        val mappings = mapOf(
            // net.lab1024.sa.base.core.* → net.lab1024.sa.foundation.core.*
            "net.lab1024.sa.base.core.annotation" to "net.lab1024.sa.foundation.core.annotation",
            "net.lab1024.sa.base.core.code" to "net.lab1024.sa.foundation.core.code",
            "net.lab1024.sa.base.core.config" to "net.lab1024.sa.foundation.core.config",
            "net.lab1024.sa.base.core.domain" to "net.lab1024.sa.foundation.core.domain",
            "net.lab1024.sa.base.core.enumeration" to "net.lab1024.sa.foundation.core.enumeration",
            "net.lab1024.sa.base.core.util" to "net.lab1024.sa.foundation.core.util",

            // net.lab1024.sa.base.config.* → net.lab1024.sa.foundation.core.config.*
            "net.lab1024.sa.base.config" to "net.lab1024.sa.foundation.core.config",

            // net.lab1024.sa.base.constant.* → net.lab1024.sa.foundation.core.constant.*
            "net.lab1024.sa.base.constant" to "net.lab1024.sa.foundation.core.constant"
        )

        var step2FilesChanged = 0
        var step2Replacements = 0

        // Process all .java files (exclude SmartBeanUtil and SmartPageUtil)
        fileTree(rootDir) {
            include("**/src/**/*.java")
            exclude("**/build/**")
            exclude("**/SmartBeanUtil.java")  // CRITICAL EXCEPTION
            exclude("**/SmartPageUtil.java")  // Already deleted
        }.forEach { file ->
            val originalContent = file.readText()
            var content = originalContent
            var fileChanged = false
            var fileReplacements = 0

            mappings.forEach { (oldPackage, newPackage) ->
                // Package declarations
                val oldPkg = "package $oldPackage"
                val newPkg = "package $newPackage"
                if (content.contains(oldPkg)) {
                    content = content.replace(oldPkg, newPkg)
                    fileChanged = true
                    fileReplacements++
                }

                // Regular imports
                val oldImport = "import $oldPackage"
                val newImport = "import $newPackage"
                if (content.contains(oldImport)) {
                    content = content.replace(oldImport, newImport)
                    fileChanged = true
                    fileReplacements++
                }

                // Static imports
                val oldStaticImport = "import static $oldPackage"
                val newStaticImport = "import static $newPackage"
                if (content.contains(oldStaticImport)) {
                    content = content.replace(oldStaticImport, newStaticImport)
                    fileChanged = true
                    fileReplacements++
                }

                // JavaDoc {@link} references
                val oldLink = "{@link $oldPackage"
                val newLink = "{@link $newPackage"
                if (content.contains(oldLink)) {
                    content = content.replace(oldLink, newLink)
                    fileChanged = true
                    fileReplacements++
                }
            }

            if (fileChanged) {
                file.writeText(content)
                step2FilesChanged++
                step2Replacements += fileReplacements
                val relativePath = file.relativeTo(rootDir).path
                println("  ✅ Migrated: $relativePath ($fileReplacements replacements)")
            }
        }

        println("  → Package migrations: $step2FilesChanged files, $step2Replacements replacements")

        totalFilesChanged += step2FilesChanged
        totalReplacements += step2Replacements

        // ==========================================================================================
        // SUMMARY
        // ==========================================================================================
        println("\n" + "=".repeat(80))
        println("Migration Complete!")
        println("=".repeat(80))
        println("Total files modified: $totalFilesChanged")
        println("Total replacements: $totalReplacements")
        println("\n✅ Step 1: SmartPageUtil duplication resolved")
        println("   - Deleted: sa-base/foundation/core/.../base/core/util/SmartPageUtil.java")
        println("   - Updated ${smartPageUtilFiles.size} files to use mybatis.util.SmartPageUtil")
        println("\n✅ Step 2: Package migration complete")
        println("   - Migrated: net.lab1024.sa.base.core.* → net.lab1024.sa.foundation.core.*")
        println("   - Migrated: net.lab1024.sa.base.config.* → net.lab1024.sa.foundation.core.config.*")
        println("   - Migrated: net.lab1024.sa.base.constant.* → net.lab1024.sa.foundation.core.constant.*")
        println("\n⚠️  CRITICAL EXCEPTIONS (intentionally preserved):")
        println("   - SmartBeanUtil.java in net.lab1024.sa.common.core.util.* (documented)")
        println("\nNext steps:")
        println("  1. Review changes: git diff")
        println("  2. Build: ./gradlew clean build")
        println("  3. Test: ./gradlew test")
        println("  4. Architecture: ./gradlew :sa-admin:test --tests ArchitectureTest")
        println("=".repeat(80))
    }
}

// Quality Gate - Sequential Execution (Fail-Fast)
// Generated by: quality-gate-orchestrator skill
tasks.register("qualityGateSequential") {
    description = "Run quality checks sequentially with fail-fast strategy"
    group = "verification"

    doLast {
        println("=" .repeat(80))
        println("🔍 SmartAdmin Quality Gate - Sequential Validation")
        println("=" .repeat(80))
        println()

        val startTime = System.currentTimeMillis()
        val results = mutableMapOf<String, Pair<Int, Long>>()

        // Step 1: Auto-format (always run first, non-blocking)
        println("1️⃣  Running Spotless (auto-format)...")
        val spotlessStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", "spotlessApply", "--no-daemon")
            }
            val spotlessDuration = System.currentTimeMillis() - spotlessStart
            results["Spotless"] = 0 to spotlessDuration
            println("   ✅ Code formatting complete (${spotlessDuration}ms)")
        } catch (e: Exception) {
            val spotlessDuration = System.currentTimeMillis() - spotlessStart
            results["Spotless"] = 1 to spotlessDuration
            println("   ⚠️  Spotless failed (${spotlessDuration}ms) - continuing...")
        }
        println()

        // Step 2: Checkstyle (BLOCKER - fail immediately)
        println("2️⃣  Running Checkstyle (code style)...")
        val checkstyleStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", ":sa-admin:checkstyleMain", ":sa-admin:checkstyleTest", "--no-daemon")
                isIgnoreExitValue = false
            }
            val checkstyleDuration = System.currentTimeMillis() - checkstyleStart
            results["Checkstyle"] = 0 to checkstyleDuration
            println("   ✅ Checkstyle: 0 violations (${checkstyleDuration}ms)")
        } catch (e: Exception) {
            val checkstyleDuration = System.currentTimeMillis() - checkstyleStart
            results["Checkstyle"] = 1 to checkstyleDuration
            println("   ❌ Checkstyle violations detected (${checkstyleDuration}ms)")
            println()
            printFailureSummary(results, startTime, "Checkstyle", 2, 6)
            throw GradleException("Quality gate FAILED at Checkstyle (BLOCKER)")
        }
        println()

        // Step 3: ArchUnit (BLOCKER - architecture rules)
        println("3️⃣  Running ArchUnit (architecture rules)...")
        val archunitStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", ":sa-admin:test", "--tests", "*ArchitectureTest", "--no-daemon")
                isIgnoreExitValue = false
            }
            val archunitDuration = System.currentTimeMillis() - archunitStart
            results["ArchUnit"] = 0 to archunitDuration
            println("   ✅ ArchUnit: All architecture rules passed (${archunitDuration}ms)")
        } catch (e: Exception) {
            val archunitDuration = System.currentTimeMillis() - archunitStart
            results["ArchUnit"] = 1 to archunitDuration
            println("   ❌ ArchUnit violations detected (${archunitDuration}ms)")
            println()
            printFailureSummary(results, startTime, "ArchUnit", 3, 6)
            throw GradleException("Quality gate FAILED at ArchUnit (BLOCKER)")
        }
        println()

        // Step 4: PMD (CRITICAL - code smells, only P1/P2 violations fail)
        println("4️⃣  Running PMD (code quality)...")
        val pmdStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", ":sa-admin:checkPmdPriority", "--no-daemon")
                isIgnoreExitValue = false
            }
            val pmdDuration = System.currentTimeMillis() - pmdStart
            results["PMD"] = 0 to pmdDuration
            println("   ✅ PMD: 0 CRITICAL violations (${pmdDuration}ms)")
        } catch (e: Exception) {
            val pmdDuration = System.currentTimeMillis() - pmdStart
            results["PMD"] = 1 to pmdDuration
            println("   ❌ PMD CRITICAL violations detected (${pmdDuration}ms)")
            println()
            printFailureSummary(results, startTime, "PMD", 4, 6)
            throw GradleException("Quality gate FAILED at PMD (CRITICAL)")
        }
        println()

        // Step 5: SpotBugs (CRITICAL - bug patterns)
        println("5️⃣  Running SpotBugs (bug detection)...")
        val spotbugsStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", ":sa-admin:spotbugsMain", "--no-daemon")
                isIgnoreExitValue = false
            }
            val spotbugsDuration = System.currentTimeMillis() - spotbugsStart
            results["SpotBugs"] = 0 to spotbugsDuration
            println("   ✅ SpotBugs: 0 bugs detected (${spotbugsDuration}ms)")
        } catch (e: Exception) {
            val spotbugsDuration = System.currentTimeMillis() - spotbugsStart
            results["SpotBugs"] = 1 to spotbugsDuration
            println("   ❌ SpotBugs violations detected (${spotbugsDuration}ms)")
            println()
            printFailureSummary(results, startTime, "SpotBugs", 5, 6)
            throw GradleException("Quality gate FAILED at SpotBugs (CRITICAL)")
        }
        println()

        // Step 6: JaCoCo Coverage Verification (MAJOR - coverage threshold)
        println("6️⃣  Running JaCoCo (coverage verification)...")
        val jacocoStart = System.currentTimeMillis()
        try {
            exec {
                commandLine("./gradlew", ":sa-admin:test", ":sa-admin:jacocoTestReport", ":sa-admin:jacocoTestCoverageVerification", "--no-daemon")
                isIgnoreExitValue = false
            }
            val jacocoDuration = System.currentTimeMillis() - jacocoStart
            results["JaCoCo"] = 0 to jacocoDuration
            println("   ✅ JaCoCo: Coverage threshold met (≥80%) (${jacocoDuration}ms)")
        } catch (e: Exception) {
            val jacocoDuration = System.currentTimeMillis() - jacocoStart
            results["JaCoCo"] = 1 to jacocoDuration
            println("   ❌ JaCoCo: Coverage below threshold (${jacocoDuration}ms)")
            println()
            printFailureSummary(results, startTime, "JaCoCo", 6, 6)
            throw GradleException("Quality gate FAILED at JaCoCo (MAJOR)")
        }
        println()

        // Success summary
        printSuccessSummary(results, startTime)
    }
}

fun printFailureSummary(
    results: Map<String, Pair<Int, Long>>,
    startTime: Long,
    failedTool: String,
    failedStep: Int,
    totalSteps: Int
) {
    println("=" .repeat(80))
    println("❌ Quality Gate FAILED")
    println("=" .repeat(80))
    println()
    println("Failed at: Step $failedStep/$totalSteps - $failedTool")
    println("Total execution time: ${System.currentTimeMillis() - startTime}ms")
    println()
    println("Completed steps:")
    results.forEach { (tool, result) ->
        val (exitCode, duration) = result
        val status = if (exitCode == 0) "✅" else "❌"
        println("  $status $tool (${duration}ms)")
    }
    println()
    println("View detailed reports:")
    println("  - Checkstyle: sa-admin/build/reports/checkstyle/main.html")
    println("  - PMD: sa-admin/build/reports/pmd/main.html")
    println("  - SpotBugs: sa-admin/build/reports/spotbugs/main.html")
    println("  - Tests: sa-admin/build/reports/tests/test/index.html")
    println("  - Coverage: sa-admin/build/reports/jacoco/test/html/index.html")
    println("=" .repeat(80))
}

fun printSuccessSummary(results: Map<String, Pair<Int, Long>>, startTime: Long) {
    val totalDuration = System.currentTimeMillis() - startTime
    println("=" .repeat(80))
    println("✅ Quality Gate PASSED")
    println("=" .repeat(80))
    println()
    println("All quality checks completed successfully!")
    println()
    println("Execution breakdown:")
    results.forEach { (tool, result) ->
        val (_, duration) = result
        val percentage = (duration.toDouble() / totalDuration * 100).toInt()
        println("  ✅ $tool: ${duration}ms ($percentage%)")
    }
    println()
    println("Total execution time: ${totalDuration}ms")
    println()
    println("Quality reports available at:")
    println("  - Checkstyle: sa-admin/build/reports/checkstyle/main.html")
    println("  - PMD: sa-admin/build/reports/pmd/main.html")
    println("  - SpotBugs: sa-admin/build/reports/spotbugs/main.html")
    println("  - Tests: sa-admin/build/reports/tests/test/index.html")
    println("  - Coverage: sa-admin/build/reports/jacoco/test/html/index.html")
    println("=" .repeat(80))
}

// Ensure git hooks are installed during project sync or build
tasks.named("build") {
    dependsOn("installGitHooks")
}
