plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Code Generator Module"

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
    api(project(":sa-base:foundation:domain"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":sa-base:foundation:core"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base:foundation:domain"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":sa-base:foundation:core"))

    // SA Base Infrastructure
    api(project(":sa-base:infrastructure:mybatis"))
    api(project(":sa-base:infrastructure:web"))
    api(project(":sa-base:infrastructure:swagger"))  // For SchemaEnum, CheckEnum

    // Template Engines (for code generation)
    api(libs.velocity.engine.core)
    api(libs.velocity.tools.generic)
    api(libs.freemarker)

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
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
