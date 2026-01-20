plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Mail Module"

dependencies {
    // Spring Boot Autoconfigure for AutoConfiguration support
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Mail Starter
    api(libs.spring.boot.starter.mail)

    // Spring Boot Web (for ResponseDTO and controller support)
    api(libs.spring.boot.starter.web)

    // MyBatis-Plus (for database access)
    api(libs.mybatis.plus.spring.boot.starter)

    // SA Common Core - foundational domain objects and utilities
    api(project(":sa-common:core"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base-core"))

    // SA Base - required for SystemEnvironment
    api(project(":sa-base"))

    // Freemarker for template rendering
    api(libs.freemarker)

    // Hutool for utility functions
    api(libs.hutool.all)

    // Apache Commons Text for StringSubstitutor
    api(libs.commons.text)

    // JSoup for HTML processing
    api(libs.jsoup)

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
