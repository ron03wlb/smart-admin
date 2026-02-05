plugins {
    java
    alias(libs.plugins.spring.boot)
    id("io.spring.dependency-management")
}

description = "SmartAdmin Unified Application - Single entry point for development"

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
    // ==================== Starter 依賴 ====================
    implementation(project(":smartadmin-starter:smartadmin-starter-all"))

    // ==================== Business Modules ====================
    implementation(project(":smartadmin-modules:smartadmin-system"))
    implementation(project(":smartadmin-modules:smartadmin-business"))
    implementation(project(":smartadmin-modules:smartadmin-oa"))

    // ==================== API Contract Layer (Optional) ====================
    // 如果需要暴露 API 契約給外部調用（例如微服務化準備）
    // implementation(project(":smartadmin-api:smartadmin-api-system"))
    // implementation(project(":smartadmin-api:smartadmin-api-business"))
    // implementation(project(":smartadmin-api:smartadmin-api-oa"))

    // Lombok annotation processor
    annotationProcessor(libs.lombok)

    // ==================== Testing Dependencies ====================
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
        mainClass.set("net.lab1024.sa.SmartAdminApplication")
    }

    // Configure resource processing for environment-specific resources
    processResources {
        // Set duplicate handling strategy - environment resources override base resources
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Exclude all environment directories first
        exclude("dev/**", "test/**", "pre/**", "prod/**")

        // Include base resources (mapper/, banner.txt, etc.)
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

    // Configure JAR naming to match Maven pattern: smartadmin-app-dev-3.0.0.jar
    bootJar {
        // Use custom file name to match Maven's finalName format
        val fileName = "smartadmin-app-" + activeEnv + "-" + project.version + ".jar"
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

    // JaCoCo Coverage Verification
    named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(test, jacocoTestReport)

        violationRules {
            rule {
                enabled = true
                element = "BUNDLE"

                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.50".toBigDecimal()
                }

                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.55".toBigDecimal()
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
}
