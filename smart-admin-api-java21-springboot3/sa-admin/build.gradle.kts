plugins {
    java
    alias(libs.plugins.spring.boot)
    id("io.spring.dependency-management")
}

// Get environment from project property (defaults to 'dev')
val activeEnv: String by lazy {
    (project.findProperty("env") as? String) ?: "dev"
}

configurations {
    // Exclude Logback and log4j-to-slf4j globally - using Log4j2 with log4j-slf4j2-impl
    all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "ch.qos.logback", module = "logback-core")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
}

dependencies {
    // Dependency on sa-base module
    implementation(project(":sa-base"))

    // Dependency on sa-base-devtools module (code generator)
    implementation(project(":sa-base:infrastructure:devtools"))

    // Dependency on sa-common:mq module
    implementation(project(":sa-base:foundation:mq"))

    // Dependency on sa-common:captcha module
    implementation(project(":sa-base:foundation:captcha"))

    // Dependency on sa-base-support:table module
    implementation(project(":sa-base:support:table"))

    // Dependency on sa-base-support:feedback module
    implementation(project(":sa-base:support:feedback"))

    // Dependency on sa-base-support:changelog module
    implementation(project(":sa-base:support:changelog"))

    // Dependency on sa-base-support:message module
    implementation(project(":sa-base:support:message"))

    // Phase 2B support modules
    // Dependency on sa-base-support:heartbeat module
    implementation(project(":sa-base:support:heartbeat"))

    // Dependency on sa-base-support:mail module
    implementation(project(":sa-base:support:mail"))

    // Dependency on sa-base-support:serialnumber module
    implementation(project(":sa-base:support:serialnumber"))

    // Dependency on sa-base-support:helpdoc module
    implementation(project(":sa-base:support:helpdoc"))

    // Lombok annotation processor
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)

    // H2 in-memory database for tests
    testRuntimeOnly("com.h2database:h2")

    // Testcontainers for integration tests (Redis, PostgreSQL)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks {
    // Configure unit tests (CI default) - excludes integration tests
    test {
        useJUnitPlatform {
            excludeTags("integration")
        }
    }

    // Configure integration tests - requires Redis and PostgreSQL
    register<Test>("integrationTest") {
        description = "Runs integration tests (requires Redis, PostgreSQL via Testcontainers)"
        group = "verification"

        useJUnitPlatform {
            includeTags("integration")
        }

        // Use test profile
        systemProperty("spring.profiles.active", "test")

        // Run after unit tests
        shouldRunAfter(test)
    }

    // Configure Spring Boot plugin
    springBoot {
        mainClass.set("net.lab1024.sa.admin.AdminApplication")
    }

    // Configure resource processing for environment-specific resources
    processResources {
        // Set duplicate handling strategy - environment resources override base resources
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Exclude all environment directories first
        exclude("dev/**", "test/**", "pre/**", "prod/**")

        // Include base resources (mapper/, etc.)
        from("src/main/resources") {
            exclude("dev/**", "test/**", "pre/**", "prod/**")
        }

        // Include environment-specific resources with filtering
        from("src/main/resources/$activeEnv") {
            // Enable filtering for YAML files with @...@ placeholders
            filesMatching("*.yaml") {
                filter { line ->
                    line.replace("@profiles.active@", activeEnv)
                }
            }
        }
    }

    // Configure JAR naming to match Maven pattern: sa-admin-dev-3.0.0.jar
    bootJar {
        // Use custom file name to match Maven's finalName format
        val fileName = "sa-admin-" + activeEnv + "-" + project.version + ".jar"
        archiveFileName.set(fileName)
    }

    // Disable plain jar task (we only want the boot jar)
    named<Jar>("jar") {
        enabled = false
    }

    // JaCoCo Coverage Report Configuration
    withType<JacocoReport> {
        dependsOn(test)

        reports {
            xml.required.set(true) // For SonarQube integration
            html.required.set(true) // For local review
        }

        // Exclude generated/config classes from coverage calculation
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude(
                            "**/config/**",
                            "**/constant/**",
                            "**/domain/entity/**",
                            "**/domain/form/**",
                            "**/domain/vo/**",
                            "**/*Application.class",
                            "**/*Configuration.class"
                        )
                    }
                }
            )
        )
    }

    // JaCoCo Coverage Verification - Enforces 80%+ Coverage
    named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(test, jacocoTestReport)

        violationRules {
            // Rule 1: Overall coverage thresholds
            rule {
                enabled = true
                element = "BUNDLE"

                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal() // 80% line coverage
                }

                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal() // 70% branch coverage
                }
            }

            // Rule 2: Service layer - higher standard
            rule {
                enabled = true
                element = "CLASS"
                includes = listOf("*.service.*")

                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal() // 85% for services
                }
            }

            // Rule 3: Manager layer
            rule {
                enabled = true
                element = "CLASS"
                includes = listOf("*.manager.*")

                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal() // 80% for managers
                }
            }
        }

        // Exclude same classes as JacocoReport
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude(
                            "**/config/**",
                            "**/constant/**",
                            "**/domain/entity/**",
                            "**/domain/form/**",
                            "**/domain/vo/**",
                            "**/*Application.class",
                            "**/*Configuration.class"
                        )
                    }
                }
            )
        )
    }

    // Integrate coverage verification into build pipeline
    named("check") {
        dependsOn("jacocoTestCoverageVerification")
    }

    // Foundation package migration task
    register("migrateToFoundation") {
        description = "Migrates code to foundation packages"
        group = "migration"

        doLast {
            val replacements = linkedMapOf(
                // Process specific classes first (more specific patterns)
                "import net.lab1024.sa.common.core.domain.ResponseDTO" to
                    "import net.lab1024.sa.foundation.domain.response.ResponseDTO",
                "import net.lab1024.sa.common.core.domain.PageResult" to
                    "import net.lab1024.sa.foundation.domain.response.PageResult",
                "import net.lab1024.sa.common.core.domain.RequestUser" to
                    "import net.lab1024.sa.foundation.domain.request.RequestUser",
                "import net.lab1024.sa.common.core.domain.PageParam" to
                    "import net.lab1024.sa.foundation.domain.request.PageParam",
                // Then process package-level patterns
                "import net.lab1024.sa.common.core.code" to
                    "import net.lab1024.sa.foundation.domain.code",
                "import net.lab1024.sa.common.core.constant" to
                    "import net.lab1024.sa.foundation.domain.constant",
                "import net.lab1024.sa.common.core.exception" to
                    "import net.lab1024.sa.foundation.domain.exception"
                // NOTE: BaseEnum and SmartBeanUtil are intentionally NOT migrated
                // BaseEnum: sa-base modules still use old package, causes type mismatch
                // SmartBeanUtil: remains in common.core.util (not moved to foundation yet)
            )

            var totalFilesModified = 0
            var totalReplacements = 0

            fileTree("src").matching {
                include("**/*.java")
            }.forEach { file ->
                var content = file.readText()
                var fileModified = false
                var fileReplacements = 0

                replacements.forEach { (old, new) ->
                    val occurrences = content.split(old).size - 1
                    if (occurrences > 0) {
                        content = content.replace(old, new)
                        fileModified = true
                        fileReplacements += occurrences
                    }
                }

                if (fileModified) {
                    file.writeText(content)
                    totalFilesModified++
                    totalReplacements += fileReplacements
                    println("✅ Migrated: ${file.path} ($fileReplacements replacements)")
                }
            }

            println("\n========================================")
            println("Migration Summary:")
            println("  Files modified: $totalFilesModified")
            println("  Total replacements: $totalReplacements")
            println("========================================")
        }
    }
}
