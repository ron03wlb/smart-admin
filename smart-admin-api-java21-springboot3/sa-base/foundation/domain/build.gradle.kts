plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Foundation Domain - Core domain objects"

dependencies {
    // Spring Boot Autoconfigure (minimal)
    api(libs.spring.boot.autoconfigure)

    // Validation API
    api(libs.spring.boot.starter.validation)

    // Jackson (for ResponseDTO serialization)
    api(libs.jackson.databind)

    // Swagger annotations
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Utilities
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
