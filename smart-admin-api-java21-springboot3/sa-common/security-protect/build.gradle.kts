plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Validation
    api(libs.spring.boot.starter.validation)

    // Spring Web (for MultipartFile) - compileOnly as consumer provides it
    compileOnly(libs.spring.boot.starter.web)

    // Spring Security Crypto (for Argon2)
    api(libs.spring.security.crypto)

    // Apache Tika (for MIME detection)
    api(libs.tika.core)

    // Apache Commons
    api(libs.commons.lang3)

    // API Documentation
    api(libs.knife4j.openapi3.jakarta)

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
