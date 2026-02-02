plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - LiteFlow Workflow Engine Module"

dependencies {
    // LiteFlow Workflow Engine
    api(libs.liteflow.spring.boot.starter)
    api(libs.liteflow.script.qlexpress)
    api(libs.liteflow.rule.sql)

    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)

    // SA Foundation - Domain (ResponseDTO, PageResult, etc.)
    api(project(":sa-base:foundation:domain"))

    // SA Foundation - Cache (JetCache for two-level caching)
    api(project(":sa-base:foundation:cache"))

    // SA Foundation - Core (SmartBeanUtil)
    api(project(":sa-base:foundation:core"))

    // SA Infrastructure - MyBatis (for database access)
    api(project(":sa-base:infrastructure:mybatis"))

    // SA Infrastructure - Token (Sa-Token for permission control)
    api(project(":sa-base:infrastructure:token"))

    // SA Infrastructure - Web (for RequestUser)
    api(project(":sa-base:infrastructure:web"))

    // SA Foundation - Repeat Submit (for @RepeatSubmit annotation)
    api(project(":sa-base:foundation:repeat-submit"))

    // SA Base Support - Reload (for hot reload integration)
    compileOnly(project(":sa-base:support:reload"))

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.hutool.all)

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
