plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Web - Web configuration and JSON serialization"

dependencies {
    // Spring Boot Web
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)

    // Apache HttpClient 5 (for RestClient configuration)
    api(libs.httpclient5)

    // SA Base Core
    api(project(":sa-base:foundation:core"))

    // SA Base Swagger (for SwaggerTagConst)
    api(project(":sa-base:infrastructure:swagger"))

    // SA Common - Repeat Submit (for RepeatSubmitException)
    api(project(":sa-base:foundation:repeat-submit"))

    // Sa-Token (for NotPermissionException)
    api(libs.sa.token.spring.boot.starter)

    // Jackson
    api(libs.jackson.databind)

    // Utilities
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
