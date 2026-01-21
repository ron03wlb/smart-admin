plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Serial Number Module"

dependencies {
    // Spring Boot Autoconfigure for AutoConfiguration support
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Web (for controller support)
    api(libs.spring.boot.starter.web)

    // Spring Boot Validation
    api(libs.spring.boot.starter.validation)

    // MyBatis-Plus (for database access)
    api(libs.mybatis.plus.spring.boot.starter)

    // SA Common Core - foundational domain objects and utilities
    api(project(":sa-base:foundation:core"))

    // SA Common Cache - for cache key constants
    api(project(":sa-base:foundation:cache"))

    // SA Common Redis Lock - for RedissonService
    api(project(":sa-base:foundation:redis-lock"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base:foundation:core"))

    // SA Base Infrastructure - specific modules instead of monolithic sa-base
    // Note: This module doesn't have controllers, so no need for sa-base-web

    // Guava for Interner
    api(libs.guava)

    // Hutool for random utilities
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
}
