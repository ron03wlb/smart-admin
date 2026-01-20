plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base DevTools - Code generator and development utilities"

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
    api(project(":sa-common:core"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base-core"))

    // SA Base - required for SupportBaseController, SwaggerTagConst, SchemaEnum, CheckEnum
    api(project(":sa-base"))

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
