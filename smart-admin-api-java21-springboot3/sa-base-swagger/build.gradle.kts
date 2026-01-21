plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Swagger - Swagger/Knife4j configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.web)

    // SA Base Core
    api(project(":sa-base-core"))

    // SA Common - API Encrypt (for ApiDecrypt, ApiEncrypt annotations)
    api(project(":sa-common:api-encrypt"))

    // API Documentation
    api(libs.knife4j.openapi3.jakarta)

    // Sa-Token (for @SaCheckPermission, @SaCheckRole, @SaMode annotations)
    api(libs.sa.token.spring.boot.starter)

    // Utilities
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
