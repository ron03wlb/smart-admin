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
        isIgnoreFailures = false

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

// Ensure git hooks are installed during project sync or build
tasks.named("build") {
    dependsOn("installGitHooks")
}
