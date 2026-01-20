plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Validation API (for jakarta.validation annotations)
    api(libs.spring.boot.starter.validation)

    // Jackson for JSON processing
    api(libs.jackson.databind)

    // Swagger/OpenAPI annotations
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging API
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
