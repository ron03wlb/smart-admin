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
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Foundation - Cache (JetCache for two-level caching)
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // SA Foundation - Core (SmartBeanUtil)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Infrastructure - MyBatis (for database access)
    api(project(":smartadmin-common:smartadmin-common-mybatis"))

    // SA Infrastructure - Token (Sa-Token for permission control)
    api(project(":smartadmin-common:smartadmin-common-token"))

    // SA Infrastructure - Web (for RequestUser)
    api(project(":smartadmin-common:smartadmin-common-web"))

    // SA Foundation - Repeat Submit (for @RepeatSubmit annotation)
    api(project(":smartadmin-common:smartadmin-common-repeat-submit"))

    // SA Base Support - Reload (for hot reload integration)
    compileOnly(project(":smartadmin-support:smartadmin-support-reload"))

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

    // Test - JUnit BOM and platform launcher for version alignment
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit5)
}
